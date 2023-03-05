/*
 * Copyright (C) 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.pictureinpicture

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map

class MainViewModel(private val stopWatchTimer: ClockStopWatchTimer): ViewModel() {


    val started: MutableLiveData<Boolean> = this.stopWatchTimer.started.map { it } as MutableLiveData<Boolean>
    val time: MutableLiveData<String> = this.stopWatchTimer.timeMillis.map { millis ->
        val minutes = millis / 1000 / 60
        val m = minutes.toString().padStart(2, '0')
        val seconds = (millis / 1000) % 60
        val s = seconds.toString().padStart(2, '0')
        val hundredths = (millis % 1000) / 10
        val h = hundredths.toString().padStart(2, '0')
        "$m:$s:$h"
    } as MutableLiveData<String>

    init {
        started.postValue(false)
    }

    /**
     * Starts the stopwatch if it is not yet started, or pauses it if it is already started.
     */
    fun startOrPause() {
        if (stopWatchTimer.started.value == true) {
            stopWatchTimer.pause()
        } else {
            stopWatchTimer.start()
        }
    }

    /**
     * Clears the stopwatch to 00:00:00.
     */
    fun clear() {
        stopWatchTimer.pause()
        stopWatchTimer.reset()
    }
}
