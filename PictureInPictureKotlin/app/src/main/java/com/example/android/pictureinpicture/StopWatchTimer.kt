package com.example.android.pictureinpicture

import androidx.lifecycle.MutableLiveData

interface StopWatchTimer {
    val timeMillis: MutableLiveData<Long>
    fun start()
    fun pause()
    fun reset()
}