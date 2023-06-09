package com.example.base

import androidx.lifecycle.ViewModel
import io.reactivex.disposables.CompositeDisposable
import java.lang.Exception

abstract class BaseViewModel: ViewModel(){

    protected val compositeDisposable = CompositeDisposable()


    override fun onCleared() {
        super.onCleared()
        compositeDisposable.dispose()
    }
}