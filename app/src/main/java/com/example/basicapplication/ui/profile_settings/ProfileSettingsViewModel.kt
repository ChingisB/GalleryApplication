package com.example.basicapplication.ui.profile_settings


import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.base.BaseViewModel
import com.example.basicapplication.util.Constants
import com.example.data.api.model.NetworkError
import com.example.domain.repository.token_repository.TokenRepository
import com.example.domain.repository.user_repository.RemoteUserRepository
import com.example.domain.resource_provider.ResourceProvider
import com.example.domain.use_case.*
import com.example.domain.use_case.validation.ValidateBirthdayUseCase
import com.example.domain.use_case.validation.ValidateConfirmPasswordUseCase
import com.example.domain.use_case.validation.ValidateEmailUseCase
import com.example.domain.use_case.validation.ValidatePasswordUseCase
import com.example.domain.use_case.validation.ValidateUsernameUseCase
import com.example.util.Resource
import com.google.gson.Gson
import io.reactivex.android.schedulers.AndroidSchedulers
import retrofit2.HttpException
import javax.inject.Inject

class ProfileSettingsViewModel(
    private val tokenRepository: TokenRepository,
    private val remoteUserRepository: RemoteUserRepository,
    private val validateUsernameUseCase: ValidateUsernameUseCase,
    private val validateEmailUseCase: ValidateEmailUseCase,
    private val validateBirthdayUseCase: ValidateBirthdayUseCase,
    private val validatePasswordUseCase: ValidatePasswordUseCase,
    private val validateConfirmPasswordUseCase: ValidateConfirmPasswordUseCase,
    private val signOutUseCase: SignOutUseCase,
    private val resourceProvider: ResourceProvider
) : BaseViewModel() {


    private val _updateUserResult = MutableLiveData<Resource<Boolean>>()
    val updateUserResult: LiveData<Resource<Boolean>> = _updateUserResult
    private val _updatePasswordResult = MutableLiveData<Resource<Boolean>>()
    val updatePasswordResult: LiveData<Resource<Boolean>> = _updatePasswordResult
    private val _signOutResult = MutableLiveData<Boolean>()
    val signOutResult: LiveData<Boolean> = _signOutResult
    private var initialUserInfoState = ProfileSettingFormState()
    private val _profileSettingsFormState = MutableLiveData<ProfileSettingFormState>()
    val profileSettingFormState: LiveData<ProfileSettingFormState> = _profileSettingsFormState
    var userId = 0
//        TODO unite

    fun setInitialFormState(username: String, birthday: String, email: String) {
        initialUserInfoState = initialUserInfoState.copy(username = username, birthday = birthday, email = email)
        _profileSettingsFormState.postValue(initialUserInfoState)

    }

    fun submitEvent(event: ProfileSettingFormEvent) {
        onEvent(event)
    }

    private fun onEvent(event: ProfileSettingFormEvent) {
        val formState = _profileSettingsFormState.value ?: initialUserInfoState

        when (event) {
            is ProfileSettingFormEvent.UsernameChanged -> {
                _profileSettingsFormState.postValue(formState.copy(username = event.username, usernameError = null))
            }
            is ProfileSettingFormEvent.BirthdayChanged -> {
                _profileSettingsFormState.postValue(formState.copy(birthday = event.birthday, birthdayError = null))
            }
            is ProfileSettingFormEvent.EmailChanged -> {
                _profileSettingsFormState.postValue(formState.copy(email = event.email, emailError = null))
            }
            is ProfileSettingFormEvent.OldPasswordChanged -> _profileSettingsFormState.postValue(
                formState.copy(oldPassword = event.oldPassword, oldPasswordError = null)
            )
            is ProfileSettingFormEvent.NewPasswordChanged -> _profileSettingsFormState.postValue(
                formState.copy(newPassword = event.newPassword, newPasswordError = null)
            )
            is ProfileSettingFormEvent.ConfirmPasswordChanged -> _profileSettingsFormState.postValue(
                formState.copy(confirmPassword = event.confirmPassword, confirmPasswordError = null)
            )
            else -> submitProfileSettingsForm()
        }
    }

    fun signOut() = signOutUseCase.invoke()
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe({ _signOutResult.postValue(true)}, { it.printStackTrace() }).let(compositeDisposable::add)


    private fun checkIsInitialFormState(): Boolean{
        val formState = _profileSettingsFormState.value ?: initialUserInfoState
        return formState.email == initialUserInfoState.email && formState.username == initialUserInfoState.username &&
                formState.birthday == initialUserInfoState.birthday
    }

    private fun submitProfileSettingsForm() {
        val formState = _profileSettingsFormState.value ?: initialUserInfoState
        val username = formState.username
        val birthday = formState.birthday
        val email = formState.email
        val oldPassword = formState.oldPassword
        val newPassword = formState.newPassword
        val confirmPassword = formState.confirmPassword


        val usernameResult = validateUsernameUseCase.invoke(formState.username)
        val birthdayResult = validateBirthdayUseCase.invoke(formState.birthday)
        val emailResult = validateEmailUseCase.invoke(formState.email)
        val oldPasswordResult = validatePasswordUseCase.invoke(formState.oldPassword)
        val newPasswordResult = validatePasswordUseCase.invoke(formState.newPassword)
        val confirmPasswordResult = validateConfirmPasswordUseCase.invoke(formState.newPassword, formState.confirmPassword)

        val isInitialFormState = checkIsInitialFormState()


        if (!isInitialFormState && listOf(oldPassword, newPassword, confirmPassword).all { it == "" }) {
            val hasError = listOf(usernameResult, birthdayResult, emailResult).any { !it.success }
            if (hasError) {
                _profileSettingsFormState.postValue(
                    formState.copy(
                        usernameError = usernameResult.errorMessage,
                        birthdayError = birthdayResult.errorMessage,
                        emailError = birthdayResult.errorMessage
                    )
                )
                return
            }
            updateUserInfo(username, email, birthday)
            return
        }

        if (isInitialFormState && listOf(oldPassword, newPassword, confirmPassword).any { it != "" }) {
            val hasError = listOf(oldPasswordResult, newPasswordResult, confirmPasswordResult).any { !it.success }
            if (hasError) {
                _profileSettingsFormState.postValue(
                    formState.copy(
                        oldPasswordError = oldPasswordResult.errorMessage,
                        newPasswordError = newPasswordResult.errorMessage,
                        confirmPasswordError = confirmPasswordResult.errorMessage
                    )
                )
                return
            }
            updatePassword(oldPassword, newPassword)
            return
        }

        val hasError = listOf(usernameResult, birthdayResult, emailResult, oldPasswordResult, newPasswordResult, confirmPasswordResult)
            .any { !it.success }
        if (hasError) {
            _profileSettingsFormState.postValue(
                formState.copy(
                    usernameError = usernameResult.errorMessage,
                    birthdayError = birthdayResult.errorMessage,
                    emailError = birthdayResult.errorMessage,
                    oldPasswordError = oldPasswordResult.errorMessage,
                    newPasswordError = newPasswordResult.errorMessage,
                    confirmPasswordError = confirmPasswordResult.errorMessage
                )
            )
            return
        }
        updatePassword(oldPassword, newPassword)
        updateUserInfo(username, email, birthday)
    }

    private fun updatePassword(oldPassword: String, newPassword: String) {
        remoteUserRepository.updatePassword(userId, oldPassword, newPassword).subscribe(
            { _updatePasswordResult.postValue(Resource.Success(true)) },
            { error ->
                val message: String
                if (error is HttpException) {
                    if (error.code() == 500) {
                        _updatePasswordResult.postValue(Resource.Success(true))
                    }
                    val body = error.response()?.errorBody()?.string()
                    val adapter = Gson().getAdapter(NetworkError::class.java)
                    val errorResponse = adapter.fromJson(body.toString())
                    message = errorResponse.detail
                    val formState = _profileSettingsFormState.value ?: ProfileSettingFormState()
                    if (message.contains("oldPassword")) {
                        _profileSettingsFormState.postValue(
                            formState.copy(oldPasswordError = resourceProvider.getMessage("incorrect_old_password"))
                        )
                    }
                }
            }
        ).let(compositeDisposable::add)
    }

    private fun updateUserInfo(username: String, email: String, birthday: String) {
        remoteUserRepository.updateUser(userId, username = username, email = email, birthday = birthday)
            .doOnSubscribe { _updateUserResult.postValue(Resource.Loading) }.subscribe(
            { _updateUserResult.postValue(Resource.Success(true)) },
            { error ->
                var message: String = error.message.toString()
                if (error is HttpException) {
                    val body = error.response()?.errorBody()?.string()
                    val adapter = Gson().getAdapter(NetworkError::class.java)
                    val errorResponse = adapter.fromJson(body.toString())
                    message = errorResponse.detail
                    val formState = _profileSettingsFormState.value ?: ProfileSettingFormState()
                    if (message.contains("email")) {
                        _profileSettingsFormState.postValue(formState.copy(emailError = resourceProvider.getMessage("email_in_use_error")))
                    }
                    if (message.contains("username")) {
                        _profileSettingsFormState.postValue(
                            formState.copy(usernameError = resourceProvider.getMessage("username_in_use_error"))
                        )
                    }
                }
                _updateUserResult.postValue(Resource.Error(message = message))
            }
        ).let(compositeDisposable::add)
    }


    class Factory @Inject constructor(
        private val tokenRepository: TokenRepository,
        private val remoteUserRepository: RemoteUserRepository,
        private val validateUsername: ValidateUsernameUseCase,
        private val validateEmail: ValidateEmailUseCase,
        private val validateBirthday: ValidateBirthdayUseCase,
        private val validatePassword: ValidatePasswordUseCase,
        private val validateConfirmPassword: ValidateConfirmPasswordUseCase,
        private val signOutUseCase: SignOutUseCase,
        private val resourceProvider: ResourceProvider
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T = kotlin.runCatching {
            @Suppress("UNCHECKED_CAST")
            return ProfileSettingsViewModel(
                tokenRepository,
                remoteUserRepository,
                validateUsername,
                validateEmail,
                validateBirthday,
                validatePassword,
                validateConfirmPassword,
                signOutUseCase,
                resourceProvider
            ) as T
        }.getOrElse { error(Constants.UNKNOWN_VIEW_MODEL_CLASS_ERROR) }
    }
}