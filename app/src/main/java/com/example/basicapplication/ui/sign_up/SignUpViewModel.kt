package com.example.basicapplication.ui.sign_up

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.base.BaseViewModel
import com.example.basicapplication.R
import com.example.data.api.model.NetworkError
import com.example.data.repository.authentication_repository.AuthenticationRepository
import com.example.domain.resource_provider.ResourceProvider
import com.example.domain.use_case.*
import com.example.domain.use_case.validation.ValidateBirthdayUseCase
import com.example.domain.use_case.validation.ValidateConfirmPasswordUseCase
import com.example.domain.use_case.validation.ValidateEmailUseCase
import com.example.domain.use_case.validation.ValidatePasswordUseCase
import com.example.domain.use_case.validation.ValidateUsernameUseCase
import com.google.gson.Gson
import retrofit2.HttpException
import java.util.UUID
import javax.inject.Inject

class SignUpViewModel @Inject constructor(
    private val authenticationRepository: AuthenticationRepository,
    private val validateUsername: ValidateUsernameUseCase,
    private val validateBirthday: ValidateBirthdayUseCase,
    private val validateEmail: ValidateEmailUseCase,
    private val validatePassword: ValidatePasswordUseCase,
    private val validateConfirmPassword: ValidateConfirmPasswordUseCase,
    private val convertLocalDate: ConvertLocalDateUseCase,
    private val parseLocalDate: ParseDateUseCase,
    private val resourceProvider: ResourceProvider
) : BaseViewModel() {

    private val _signedUp = MutableLiveData<Boolean>()
    val signedUp: LiveData<Boolean> = _signedUp
    private val _signUpFormState = MutableLiveData(SignUpFormState())
    val signUpFormState: LiveData<SignUpFormState> = _signUpFormState


    fun submitEvent(event: SignUpFormEvent) = onEvent(event)


    private fun onEvent(event: SignUpFormEvent) {
        val formState = signUpFormState.value ?: SignUpFormState()
        when (event) {
            is SignUpFormEvent.EmailChanged -> _signUpFormState.postValue(formState.copy(email = event.email, emailError = null))

            is SignUpFormEvent.UsernameChanged -> _signUpFormState.postValue(
                formState.copy(username = event.username, usernameError = null)
            )

            is SignUpFormEvent.BirthdayChanged -> _signUpFormState.postValue(
                formState.copy(birthday = event.birthday, birthdayError = null)
            )

            is SignUpFormEvent.PasswordChanged -> _signUpFormState.postValue(
                formState.copy(password = event.password, passwordError = null)
            )

            is SignUpFormEvent.ConfirmPasswordChanged -> _signUpFormState.postValue(
                formState.copy(confirmPassword = event.confirmPassword, confirmPasswordError = null)
            )

            else -> submitSignUpForm()
        }
    }

    private fun submitSignUpForm() {
        val formState = _signUpFormState.value ?: SignUpFormState()
        val usernameResult = validateUsername.invoke(formState.username)
        val birthdayResult = validateBirthday.invoke(formState.birthday)
        val emailResult = validateEmail.invoke(formState.email)
        val passwordResult = validatePassword.invoke(formState.password)
        val confirmPasswordResult = validateConfirmPassword.invoke(formState.password, formState.confirmPassword)

        if (listOf(usernameResult, birthdayResult, emailResult, passwordResult, confirmPasswordResult).any { !it.success }) {
            _signUpFormState.postValue(
                formState.copy(
                    usernameError = usernameResult.errorMessage,
                    birthdayError = birthdayResult.errorMessage,
                    emailError = emailResult.errorMessage,
                    passwordError = passwordResult.errorMessage,
                    confirmPasswordError = confirmPasswordResult.errorMessage
                )
            )
            return
        }

        signUp(
            formState.username,
            convertLocalDate.invoke(parseLocalDate.invoke(formState.birthday)),
            formState.email,
            formState.password,
            formState.confirmPassword
        )
    }

    private fun signUp(username: String, birthday: String, email: String, password: String, confirmPassword: String) {
        authenticationRepository.signUp(
            birthday = birthday,
            username = username,
            password = password,
            confirmPassword = confirmPassword,
            email = email
        ).subscribe(
            { _signedUp.postValue(true) },
            { error ->
                if (error is HttpException) handleNetworkError(error)
                _signedUp.postValue(false)
            }).let(compositeDisposable::add)
    }

    private fun handleNetworkError(error: HttpException) {
        val body = error.response()?.errorBody()?.string()
        val adapter = Gson().getAdapter(NetworkError::class.java)
        val errorResponse = adapter.fromJson(body.toString())
        val message = errorResponse.detail
        val formState = _signUpFormState.value ?: SignUpFormState()
        if (message.contains("email")) {
            _signUpFormState.postValue(formState.copy(emailError = resourceProvider.getMessage(R.string.email_in_use_error)))
        }
        if (message.contains("username")) {
            _signUpFormState.postValue(formState.copy(usernameError = resourceProvider.getMessage(R.string.username_in_use_error)))
        }
    }

}