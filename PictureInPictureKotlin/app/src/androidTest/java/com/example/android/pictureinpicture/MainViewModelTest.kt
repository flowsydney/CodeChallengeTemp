@file:OptIn(ExperimentalCoroutinesApi::class)

package com.example.android.pictureinpicture


import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*

import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule

@ExperimentalCoroutinesApi
class MainViewModelTest {

    @get:Rule
    val mockitoRule: MockitoRule = MockitoJUnit.rule()

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var viewModel: MainViewModel

    @Before
    fun setUp() {
        viewModel = MainViewModel()
        viewModel.clear()
    }


    @Test
    fun startOrPause_should_start_the_stopwatch_not_yet_started() = runTest {
        viewModel.startOrPause(ACTION_STOPWATCH_CONTROL)
        assertEquals(true, viewModel.started.getOrAwaitValue())
    }

    @Test
    fun startOrPause_should_pause_stopwatch_already_started() {
        viewModel.startOrPause(ACTION_STOPWATCH_CONTROL)
        viewModel.startOrPause(ACTION_STOPWATCH_CONTROL)
        assertEquals(false, viewModel.started.getOrAwaitValue())
    }

    @Test
    fun clear_should_reset_stopwatch_to_00() {
        viewModel.startOrPause(ACTION_STOPWATCH_CONTROL)
        viewModel.startOrPause(ACTION_STOPWATCH_CONTROL)
        viewModel.clear()
        assertEquals(false, viewModel.started.getOrAwaitValue(),)
        assertEquals("00:00:00", viewModel.time.getOrAwaitValue())
    }
}

@ExperimentalCoroutinesApi
class ClockStopWatchTimerTest {

    private lateinit var stopWatchTimer: ClockStopWatchTimer

    @Before
    fun setup() {

    }

    @Test
    fun start_should_start_timer() = runTest {
        stopWatchTimer.started.postValue(false)

        stopWatchTimer.start()

        assertTrue(stopWatchTimer.started.getOrAwaitValue())
    }

    @Test
    fun reset_should_reset_the_timer_00() {
        stopWatchTimer.reset()

        assertEquals(0L, stopWatchTimer.timeMillis.getOrAwaitValue())
    }
}