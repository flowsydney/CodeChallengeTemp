/*
 * Copyright (C) 2017 The Android Open Source Project
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

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.activity.viewModels
import androidx.annotation.RequiresApi

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment


/** Intent action for stopwatch controls from Picture-in-Picture mode.  */
internal const val ACTION_STOPWATCH_CONTROL = "stopwatch_control"
internal const val ACTION_MOVIE_CONTROL = "movie_control"

/** Intent extra for stopwatch controls from Picture-in-Picture mode.  */
internal const val EXTRA_CONTROL_TYPE = "control_type"
internal const val CONTROL_TYPE_CLEAR = 1
internal const val CONTROL_TYPE_START_OR_PAUSE = 2
internal const val CONTROL_TYPE_FORWARD = 3
internal const val CONTROL_TYPE_REWIND = 4


internal const val REQUEST_CLEAR = 5
internal const val REQUEST_START_OR_PAUSE = 6
internal const val REQUEST_FAST_REWIND = 7
internal const val REQUEST_FAST_FORWARD = 8

/**
 * Demonstrates usage of Picture-in-Picture mode on phones and tablets.
 */
class MainActivity : AppCompatActivity(R.layout.main_activity) {

    private val viewModel: MainViewModel by viewModels { MainViewModelFactory() }

    /**
     * A [BroadcastReceiver] for handling action items on the picture-in-picture mode.
     */
    private val broadcastReceiver = object : BroadcastReceiver() {

        // Called when an item is clicked.
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent == null || intent.action != ACTION_STOPWATCH_CONTROL) {
                return
            }
            when (intent.getIntExtra(EXTRA_CONTROL_TYPE, 0)) {
                CONTROL_TYPE_START_OR_PAUSE -> {
                    viewModel.startOrPause(intent.action as String)
                }
                CONTROL_TYPE_CLEAR -> viewModel.clear()
            }
        }
    }

    private val movieBroadcastReceiver = object : BroadcastReceiver() {

        // Called when an item is clicked.
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent == null ||  intent.action != ACTION_MOVIE_CONTROL) {
                return
            }
            when (intent.getIntExtra(EXTRA_CONTROL_TYPE, 0)) {
                CONTROL_TYPE_START_OR_PAUSE -> {
                    viewModel.startOrPause(intent.action as String)
                }
                CONTROL_TYPE_FORWARD -> {
                    viewModel.setMovieState(MovieState.FASTFORWARD)
                }
                CONTROL_TYPE_REWIND -> {
                    viewModel.setMovieState(MovieState.REWIND)
                }
            }
        }
    }

    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.setCoroutineScope(lifecycleScope)
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController
        registerReceiver(broadcastReceiver, IntentFilter(ACTION_STOPWATCH_CONTROL))
        registerReceiver(movieBroadcastReceiver, IntentFilter(ACTION_MOVIE_CONTROL))
    }

    private fun checkPictureInPicture() = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) && isInPictureInPictureMode

    override fun onRestart() {
        super.onRestart()
        if (!checkPictureInPicture()) {
            // Show the video controls so the video can be easily resumed.
            viewModel.setMovieControlState(MovieControlState.SHOW_CONTROL)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean, newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        if (isInPictureInPictureMode) {
            // Hide the controls in picture-in-picture mode.
            viewModel.setMovieControlState(MovieControlState.HIDE_CONTROL)
        } else {
            // Show the video controls if the video is not playing
            if (viewModel.getMovieState().value != MovieState.PAUSE) {
                viewModel.setMovieControlState(MovieControlState.SHOW_CONTROL)
            }
        }
    }

    /**
     * Handle navigation when the user chooses Up from the action bar.
     */
    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

}
