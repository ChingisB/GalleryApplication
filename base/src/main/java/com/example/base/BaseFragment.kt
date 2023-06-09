package com.example.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedDispatcher
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding

abstract class BaseFragment<VBinding: ViewBinding, ViewModel: BaseViewModel>: Fragment(){

    protected abstract val viewModel: ViewModel
    protected val binding: VBinding by lazy { getViewBinding() }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = binding.root


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        setupListeners()
        observeData()
        addOnBackPressedCallbacks(requireActivity().onBackPressedDispatcher)
    }

    protected abstract fun getViewBinding(): VBinding

    protected open fun setupViews() = Unit

    protected open fun setupListeners() = Unit

    protected open fun observeData() = Unit

    protected open fun addOnBackPressedCallbacks(dispatcher: OnBackPressedDispatcher) = Unit

    protected fun showToastShort(@StringRes message: Int) = Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()

    protected fun showToastShort(message: String) = Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()

    protected fun showToastLong(@StringRes message: Int) = Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()

    protected fun showToastLong(message: String) = Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()

}