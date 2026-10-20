package com.teampointblank.lpms.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel

/** Creates a ViewModel with constructor arguments: `val vm = lpmsViewModel { MyViewModel(repo, id) }`. */
@Composable
inline fun <reified VM : ViewModel> lpmsViewModel(crossinline builder: () -> VM): VM {
    return viewModel<VM>(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = builder() as T
        }
    )
}
