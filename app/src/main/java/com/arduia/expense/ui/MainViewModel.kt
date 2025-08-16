package com.arduia.expense.ui

import androidx.lifecycle.*
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.arduia.core.arch.Mapper
import com.arduia.expense.data.CurrencyRepository
import com.arduia.expense.data.SettingsRepository
import com.arduia.expense.data.local.AboutUpdateDataModel
import com.arduia.expense.data.local.UpdateStatusDataModel
import com.arduia.expense.data.update.CheckAboutUpdateWorker
import com.arduia.expense.model.Result
import com.arduia.expense.model.getDataOrError
import com.arduia.expense.model.onSuccess
import com.arduia.expense.ui.about.AboutUpdateUiModel
import com.arduia.mvvm.post
import com.arduia.mvvm.set
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.time.delay
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val settingRepo: SettingsRepository,
    private val currencyRepo: CurrencyRepository,
    private val workManager: WorkManager,
    private val aboutUpdateUiDataMapper: Mapper<AboutUpdateDataModel, AboutUpdateUiModel>
) : ViewModel(), LifecycleObserver {

    private val _forceUpgradeState = MutableLiveData<Pair<Boolean, AboutUpdateUiModel?>>()
    val forceUpgradeState: LiveData<Pair<Boolean, AboutUpdateUiModel?>> = _forceUpgradeState

    init {
        //default disabled
        _forceUpgradeState.set(false to null)
        observeAndCacheSelectedCurrency()
        observeForceUpgrade()
    }

    private fun observeForceUpgrade() {
        settingRepo.getUpdateStatus()
            .flowOn(Dispatchers.IO)
            .onSuccess { status ->
                when (status) {
                    UpdateStatusDataModel.STATUS_FORCE_UPGRADE -> {
                        viewModelScope.launch(Dispatchers.IO) {
                            delay(1_000L)
                            val info = settingRepo.getAboutUpdateSync().getDataOrError()
                            _forceUpgradeState.post(true to aboutUpdateUiDataMapper.map(info))
                        }
                    }

                    else -> _forceUpgradeState.post(false to null)
                }
            }
            .launchIn(viewModelScope)
    }

    private fun observeAndCacheSelectedCurrency() {
        settingRepo.getSelectedCurrencyNumber()
            .flowOn(Dispatchers.IO)
            .onEach {
                if (it is Result.Success) {
                    currencyRepo.setSelectedCacheCurrency(it.data)
                }
            }
            .launchIn(viewModelScope)
    }


    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    private fun startCheckAboutUpdateWork() {
        val checkVersionRequest = OneTimeWorkRequestBuilder<CheckAboutUpdateWorker>()
            .build()
        Timber.d("Hilt Config: ${workManager.configuration.workerFactory}")
        workManager.enqueue(checkVersionRequest)
        Timber.d("startCheckUpdate")
    }
}