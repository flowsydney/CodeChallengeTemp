package com.example.android.pictureinpicture

import android.os.SystemClock
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.*
import kotlinx.coroutines.android.awaitFrame

object ClockStopWatchTimer: StopWatchTimer {

    var job: Job? = null
    var viewModelScope: CoroutineScope? = null
    private var startUptimeMillis = SystemClock.uptimeMillis()
    private val _timeMillis = MutableLiveData(0L)

    private val _started = MutableLiveData(false)

    val started: MutableLiveData<Boolean> = _started

    override val timeMillis: MutableLiveData<Long>
        get() = _timeMillis


    override fun start() {
        if (started.value == true) return
        _started.postValue(true)
        job = viewModelScope?.launch { startTimer() }
    }

    override fun pause() {
        if (started.value == false) return
        _started.postValue(false)
        job?.cancel()
    }

    override fun reset() {
        startUptimeMillis = SystemClock.uptimeMillis()
        _timeMillis.postValue(0L)
    }

    @VisibleForTesting
    suspend fun CoroutineScope.startTimer() {
        startUptimeMillis = SystemClock.uptimeMillis() - (_timeMillis.value ?: 0L)
        while (isActive) {
            _timeMillis.postValue(SystemClock.uptimeMillis() - startUptimeMillis)
            // Updates on every render frame.
            awaitFrame()
        }
    }

    fun destroyCoroutine() {
        viewModelScope?.cancel()
    }
}