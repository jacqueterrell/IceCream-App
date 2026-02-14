package com.icecreamapp.sweethearts.ui

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class IceCreamViewModelFactory(
    private val application: Application,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass != IceCreamViewModel::class.java) {
            throw IllegalArgumentException("Unknown ViewModel class")
        }
        return IceCreamViewModel(application) as T
    }
}
