package com.example.android.pictureinpicture

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.CoroutineScope

class MainViewModelFactory(private val viewModelScope: CoroutineScope) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            return MainViewModel(ClockStopWatchTimer(viewModelScope)) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}