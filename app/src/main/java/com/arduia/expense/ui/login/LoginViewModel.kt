package com.arduia.expense.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arduia.expense.data.remote.supabase.SupabaseAuthRepository
import com.arduia.expense.data.remote.supabase.SupabaseProfileRepository
import com.arduia.expense.data.remote.supabase.SupabaseSyncRepository
import com.arduia.expense.data.SettingsRepository
import com.arduia.expense.data.local.LocalDataRepository
import com.arduia.expense.model.Result
import com.arduia.expense.model.getDataOrError
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers

data class LoginUiState(
    val loading: Boolean = false,
    val success: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: SupabaseAuthRepository,
    private val syncRepository: SupabaseSyncRepository,
    private val profileRepository: SupabaseProfileRepository,
    private val settingsRepository: SettingsRepository,
    private val localDataRepository: LocalDataRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun signIn(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = LoginUiState(error = "Email and password required")
            return
        }
        _uiState.value = LoginUiState(loading = true)
        viewModelScope.launch(Dispatchers.IO) {
            when (val result = authRepository.signIn(email, password)) {
                is Result.Success -> {
                    val currentUserId = authRepository.currentUserId().orEmpty()
                    val lastUserId = settingsRepository.getLastUserId().getDataOrError()
                    if (lastUserId.isNotBlank() && lastUserId != currentUserId) {
                        localDataRepository.clearAll()
                        settingsRepository.setLastSyncAt(0L)
                        settingsRepository.setUserName("")
                    }
                    settingsRepository.setLastUserId(currentUserId)

                    when (val profile = profileRepository.fetchUserName()) {
                        is Result.Success -> settingsRepository.setUserName(profile.data)
                        is Result.Error -> settingsRepository.setUserName("")
                        Result.Loading -> Unit
                    }
                    settingsRepository.setLastAuthAt(System.currentTimeMillis())
                    when (val sync = syncRepository.syncTwoWay()) {
                        is Result.Success -> _uiState.value = LoginUiState(success = true)
                        is Result.Error -> _uiState.value =
                            LoginUiState(error = sync.exception.message ?: "Sync failed")
                        Result.Loading -> _uiState.value = LoginUiState(loading = true)
                    }
                }
                is Result.Error -> _uiState.value =
                    LoginUiState(error = result.exception.message ?: "Login failed")
                Result.Loading -> _uiState.value = LoginUiState(loading = true)
            }
        }
    }

    suspend fun syncNow(): Result<Unit> {
        return when (val result = syncRepository.syncTwoWay()) {
            is Result.Success -> Result.Success(Unit)
            is Result.Error -> Result.Error(result.exception)
            Result.Loading -> Result.Loading
        }
    }
}
