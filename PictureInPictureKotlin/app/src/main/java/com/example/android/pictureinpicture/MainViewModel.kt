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

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import kotlinx.coroutines.CoroutineScope

class MainViewModel: ViewModel() {
    private val movieControlState = MutableLiveData<MovieControlState>()
    private val movieState = MutableLiveData<MovieState>()

    val started: LiveData<Boolean> = ClockStopWatchTimer.started.map { it }
    val time: LiveData<String> = ClockStopWatchTimer.timeMillis.map { millis ->
        val minutes = millis / 1000 / 60
        val m = minutes.toString().padStart(2, '0')
        val seconds = (millis / 1000) % 60
        val s = seconds.toString().padStart(2, '0')
        val hundredths = (millis % 1000) / 10
        val h = hundredths.toString().padStart(2, '0')
        "$m:$s:$h"
    }

    init {
        ClockStopWatchTimer.started.postValue(false)
    }

    fun setMovieState(state: MovieState) {
        movieState.postValue(state)
    }
    fun getMovieState(): LiveData<MovieState> {
        return movieState
    }
    fun setMovieControlState(state: MovieControlState) {
        movieControlState.postValue(state)
    }
    fun getMovieControlState(): LiveData<MovieControlState> {
        return movieControlState
    }

    /**
     * Starts the stopwatch if it is not yet started, or pauses it if it is already started.
     */
    fun startOrPause(action: String) {
        when (action) {
            ACTION_STOPWATCH_CONTROL -> {
                if (ClockStopWatchTimer.started.value == true) {
                    ClockStopWatchTimer.pause()
                } else {
                    ClockStopWatchTimer.start()
                }
            }
            ACTION_MOVIE_CONTROL -> {
               if (movieState.value != MovieState.PAUSE) {
                   setMovieState(MovieState.PAUSE)
               } else {
                   setMovieState(MovieState.PLAY)
               }
            }
        }
    }


    /**
     * Clears the stopwatch to 00:00:00.
     */
    fun clear() {
        ClockStopWatchTimer.pause()
        ClockStopWatchTimer.reset()
    }

    fun setCoroutineScope(coroutineScope: CoroutineScope) {
       ClockStopWatchTimer.viewModelScope = coroutineScope
    }

}


enum class MovieState {
    PLAY,
    PAUSE,
    FASTFORWARD,
    REWIND
}
enum class MovieControlState {
    SHOW_CONTROL,
    HIDE_CONTROL
}