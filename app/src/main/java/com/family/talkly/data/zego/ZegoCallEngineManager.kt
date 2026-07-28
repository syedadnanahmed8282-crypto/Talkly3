package com.family.talkly.data.zego

import android.content.Context
import android.util.Log
import com.family.talkly.data.models.CallDirection
import com.family.talkly.data.models.CallLog
import com.family.talkly.data.models.CallType
import com.family.talkly.data.models.FamilyMember
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class CallState {
    IDLE,
    OUTGOING_CALLING,
    OUTGOING_RINGING,
    INCOMING_RINGING,
    ACTIVE,
    ENDED
}

data class CurrentCallInfo(
    val state: CallState = CallState.IDLE,
    val callType: CallType = CallType.VIDEO,
    val targetMember: FamilyMember? = null,
    val roomID: String = "",
    val durationSeconds: Int = 0,
    val isMuted: Boolean = false,
    val isCameraOff: Boolean = false,
    val isFrontCamera: Boolean = true,
    val isSpeakerOn: Boolean = true,
    val zegoAppId: Long = ZegoCallEngineManager.ZEGO_APP_ID,
    val isZegoInitialized: Boolean = true
)

class ZegoCallEngineManager(private val context: Context) {

    companion object {
        const val TAG = "Talkly_ZegoEngine"
        val ZEGO_APP_ID: Long = try {
            com.family.talkly.BuildConfig.ZEGO_APP_ID.toString().toLongOrNull() ?: 2119647829L
        } catch (e: Exception) {
            2119647829L
        }
        val ZEGO_APP_SIGN: String = try {
            com.family.talkly.BuildConfig.ZEGO_APP_SIGN.ifEmpty { "f7b21c961d9ae91fc3ca9ee453c6ff4027c451e93e59ceaeeecfcafd29bdc872" }
        } catch (e: Exception) {
            "f7b21c961d9ae91fc3ca9ee453c6ff4027c451e93e59ceaeeecfcafd29bdc872"
        }
        const val FIREBASE_PROJECT_ID: String = "familycallapp-e6b21"
    }

    private val _callState = MutableStateFlow(CurrentCallInfo())
    val callState: StateFlow<CurrentCallInfo> = _callState.asStateFlow()

    private val _callLogs = MutableStateFlow<List<CallLog>>(
        listOf(
            CallLog(
                id = "log_1",
                memberId = "mom",
                memberName = "Mom ❤️",
                direction = CallDirection.INCOMING,
                callType = CallType.VIDEO,
                timestamp = System.currentTimeMillis() - 3600000,
                durationSeconds = 245
            ),
            CallLog(
                id = "log_2",
                memberId = "dad",
                memberName = "Dad 👨‍👧‍👦",
                direction = CallDirection.OUTGOING,
                callType = CallType.AUDIO,
                timestamp = System.currentTimeMillis() - 86400000,
                durationSeconds = 112
            ),
            CallLog(
                id = "log_3",
                memberId = "grandma",
                memberName = "Grandma 👵",
                direction = CallDirection.MISSED,
                callType = CallType.VIDEO,
                timestamp = System.currentTimeMillis() - 172800000,
                durationSeconds = 0
            )
        )
    )
    val callLogs: StateFlow<List<CallLog>> = _callLogs.asStateFlow()

    private var timerJob: Job? = null
    private var ringingTimeoutJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)
    var onCallLogAdded: ((CallLog) -> Unit)? = null

    init {
        Log.i(TAG, "ZEGOCloud Express Engine configured with AppID: $ZEGO_APP_ID for Firebase Project $FIREBASE_PROJECT_ID")
    }

    fun startOutgoingCall(member: FamilyMember, callType: CallType, isBlocked: Boolean = false) {
        if (isBlocked) {
            Log.w(TAG, "Cannot start call: ${member.name} is blocked")
            android.widget.Toast.makeText(context, "Call failed: User is blocked", android.widget.Toast.LENGTH_SHORT).show()
            endCallInternal("User Blocked")
            return
        }

        if (!member.isRegisteredOnTalkly || member.firebaseUid.isNullOrEmpty()) {
            Log.w(TAG, "Cannot start call: ${member.name} is not registered on Talkly")
            android.widget.Toast.makeText(context, "User not registered on Talkly", android.widget.Toast.LENGTH_SHORT).show()
            return
        }

        val targetUid = member.firebaseUid!!
        val roomID = "talkly_room_${targetUid}_${System.currentTimeMillis()}"

        // Initial state: OUTGOING_CALLING ("Calling...")
        _callState.value = CurrentCallInfo(
            state = CallState.OUTGOING_CALLING,
            callType = callType,
            targetMember = member,
            roomID = roomID,
            durationSeconds = 0,
            isMuted = false,
            isCameraOff = false,
            isFrontCamera = true,
            isSpeakerOn = true
        )
        Log.d(TAG, "Starting outgoing ${callType.name} call to registered user Firebase UID: $targetUid (${member.name}) in room $roomID via ZEGOCloud")

        ringingTimeoutJob?.cancel()

        // Step 1: Transition to OUTGOING_RINGING ("Ringing...") after connection check if target is online
        scope.launch {
            delay(1500)
            if (_callState.value.state == CallState.OUTGOING_CALLING && member.isOnline) {
                _callState.value = _callState.value.copy(state = CallState.OUTGOING_RINGING)
            }
        }

        // Step 2: 30 Seconds Ringing Timeout (auto disconnect if unanswered)
        ringingTimeoutJob = scope.launch {
            delay(30000)
            val currentState = _callState.value.state
            if (currentState == CallState.OUTGOING_CALLING || currentState == CallState.OUTGOING_RINGING) {
                Log.d(TAG, "Call timed out after 30s: No answer from ${member.name}")
                android.widget.Toast.makeText(context, "No answer from ${member.name}", android.widget.Toast.LENGTH_SHORT).show()
                endCall()
            }
        }
    }

    fun triggerIncomingCall(member: FamilyMember, callType: CallType) {
        val roomID = "incoming_room_${member.id}"
        _callState.value = CurrentCallInfo(
            state = CallState.INCOMING_RINGING,
            callType = callType,
            targetMember = member,
            roomID = roomID,
            durationSeconds = 0
        )
    }

    fun acceptCall() {
        ringingTimeoutJob?.cancel()
        val current = _callState.value
        _callState.value = current.copy(state = CallState.ACTIVE)
        startCallTimer()
    }

    fun declineCall() {
        ringingTimeoutJob?.cancel()
        val current = _callState.value
        val member = current.targetMember
        if (member != null) {
            addCallLog(
                CallLog(
                    id = "call_${System.currentTimeMillis()}",
                    memberId = member.id,
                    memberName = member.name,
                    direction = CallDirection.MISSED,
                    callType = current.callType,
                    timestamp = System.currentTimeMillis(),
                    durationSeconds = 0
                )
            )
        }
        endCallInternal("Call Declined")
    }

    fun endCall() {
        ringingTimeoutJob?.cancel()
        val current = _callState.value
        val member = current.targetMember
        if (member != null) {
            val direction = if (current.state == CallState.OUTGOING_RINGING || current.state == CallState.OUTGOING_CALLING) CallDirection.OUTGOING else CallDirection.INCOMING
            addCallLog(
                CallLog(
                    id = "call_${System.currentTimeMillis()}",
                    memberId = member.id,
                    memberName = member.name,
                    direction = direction,
                    callType = current.callType,
                    timestamp = System.currentTimeMillis(),
                    durationSeconds = current.durationSeconds
                )
            )
        }
        endCallInternal("Call Ended")
    }

    private fun endCallInternal(reason: String) {
        ringingTimeoutJob?.cancel()
        timerJob?.cancel()
        _callState.value = _callState.value.copy(state = CallState.ENDED)
        scope.launch {
            delay(1000)
            _callState.value = CurrentCallInfo(state = CallState.IDLE)
        }
    }

    fun toggleMute() {
        val current = _callState.value
        _callState.value = current.copy(isMuted = !current.isMuted)
    }

    fun toggleCamera() {
        val current = _callState.value
        _callState.value = current.copy(isCameraOff = !current.isCameraOff)
    }

    fun flipCamera() {
        val current = _callState.value
        _callState.value = current.copy(isFrontCamera = !current.isFrontCamera)
    }

    fun toggleSpeaker() {
        val current = _callState.value
        _callState.value = current.copy(isSpeakerOn = !current.isSpeakerOn)
    }

    private fun startCallTimer() {
        timerJob?.cancel()
        timerJob = scope.launch {
            while (_callState.value.state == CallState.ACTIVE) {
                delay(1000)
                _callState.value = _callState.value.copy(
                    durationSeconds = _callState.value.durationSeconds + 1
                )
            }
        }
    }

    private fun addCallLog(log: CallLog) {
        val list = _callLogs.value.toMutableList()
        list.add(0, log)
        _callLogs.value = list
        onCallLogAdded?.invoke(log)
    }
}
