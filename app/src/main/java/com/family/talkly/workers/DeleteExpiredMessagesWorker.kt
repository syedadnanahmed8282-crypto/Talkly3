package com.family.talkly.workers

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.family.talkly.data.models.ChatMessage
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class DeleteExpiredMessagesWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val TAG = "DeleteExpiredWorker"
        const val WORK_NAME = "periodic_delete_expired_messages_work"

        /**
         * Schedules periodic WorkManager job to delete messages older than 48 hours
         * every hour, and runs an immediate one-time cleanup job.
         */
        fun schedulePeriodicCleanup(context: Context) {
            try {
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()

                val periodicWork = PeriodicWorkRequestBuilder<DeleteExpiredMessagesWorker>(
                    1, TimeUnit.HOURS
                )
                    .setConstraints(constraints)
                    .build()

                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    periodicWork
                )

                // Also trigger immediate one-time cleanup on launch
                val immediateWork = OneTimeWorkRequestBuilder<DeleteExpiredMessagesWorker>().build()
                WorkManager.getInstance(context).enqueue(immediateWork)

                Log.i(TAG, "Successfully scheduled WorkManager job for expiring messages (>48h)")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to schedule WorkManager job: ${e.localizedMessage}")
            }
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Log.d(TAG, "Starting background scan to delete Firestore messages older than 48 hours...")
        
        var deletedCount = 0
        val cutoffTimestamp = System.currentTimeMillis() - ChatMessage.EXPIRATION_48_HOURS_MS

        try {
            val firestore = FirebaseFirestore.getInstance()

            // 1. Query subcollections named 'messages' across family chats
            val subcollectionTask = firestore.collectionGroup("messages")
                .whereLessThan("timestamp", cutoffTimestamp)
                .get()
            val subcollectionSnapshot = Tasks.await(subcollectionTask, 15, TimeUnit.SECONDS)

            if (!subcollectionSnapshot.isEmpty) {
                val batch = firestore.batch()
                for (doc in subcollectionSnapshot.documents) {
                    batch.delete(doc.reference)
                    deletedCount++
                }
                val commitTask = batch.commit()
                Tasks.await(commitTask, 15, TimeUnit.SECONDS)
            }

            // 2. Query root 'messages' collection if exists
            val rootCollectionTask = firestore.collection("messages")
                .whereLessThan("timestamp", cutoffTimestamp)
                .get()
            val rootCollectionSnapshot = Tasks.await(rootCollectionTask, 15, TimeUnit.SECONDS)

            if (!rootCollectionSnapshot.isEmpty) {
                val batch = firestore.batch()
                for (doc in rootCollectionSnapshot.documents) {
                    batch.delete(doc.reference)
                    deletedCount++
                }
                val commitTask = batch.commit()
                Tasks.await(commitTask, 15, TimeUnit.SECONDS)
            }

            Log.i(TAG, "WorkManager Cleanup Completed: Successfully deleted $deletedCount expired messages older than 48 hours.")
            Result.success()
        } catch (e: Exception) {
            Log.w(TAG, "Error executing DeleteExpiredMessagesWorker: ${e.localizedMessage}", e)
            Result.retry()
        }
    }
}
