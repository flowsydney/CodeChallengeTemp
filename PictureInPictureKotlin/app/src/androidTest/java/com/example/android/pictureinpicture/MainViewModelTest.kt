@file:OptIn(ExperimentalCoroutinesApi::class)

package com.example.android.pictureinpicture


import kotlinx.coroutines.*
import kotlinx.coroutines.test.*

import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule

@ExperimentalCoroutinesApi
class MainViewModelTest {

    @get:Rule
    val mockitoRule: MockitoRule = MockitoJUnit.rule()

    private lateinit var stopWatchTimer: ClockStopWatchTimer

    private lateinit var viewModel: MainViewModel

    @Before
    fun setUp() {
        stopWatchTimer =
            ClockStopWatchTimer(createTestCoroutineScope(TestCoroutineDispatcher() + TestCoroutineExceptionHandler()))
        viewModel = MainViewModel(stopWatchTimer)
        viewModel.clear()
    }


    @Test
    fun startOrPause_should_start_the_stopwatch_not_yet_started() = runTest {
        stopWatchTimer.started.postValue(false)
        viewModel.startOrPause()
        assertEquals(true, stopWatchTimer.started.value)
    }

    @Test
    fun startOrPause_should_pause_stopwatch_already_started() {
        viewModel.startOrPause()
        viewModel.startOrPause()
        assertEquals(false, stopWatchTimer.started.value)
    }

    @Test
    fun clear_should_reset_stopwatch_to_00() {
        viewModel.startOrPause()
        viewModel.startOrPause()
        viewModel.clear()
        assertEquals(false, stopWatchTimer.started.value,)
        assertEquals(null, viewModel.time.value)
    }
}

@ExperimentalCoroutinesApi
class ClockStopWatchTimerTest {

    private lateinit var stopWatchTimer: ClockStopWatchTimer

    @Before
    fun setup() {
        stopWatchTimer =
            ClockStopWatchTimer(createTestCoroutineScope(TestCoroutineDispatcher() + TestCoroutineExceptionHandler()))
    }

    @Test
    fun start_should_start_timer() = runTest {
        stopWatchTimer.started.postValue(false)

        stopWatchTimer.start()

        assertTrue(stopWatchTimer.started.value!!)
    }

    @Test
    fun reset_should_reset_the_timer_00() {
        stopWatchTimer.reset()

        assertEquals(0L, stopWatchTimer.timeMillis.value)
    }
}