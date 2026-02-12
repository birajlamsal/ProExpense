package com.arduia.expense.data

import android.content.Context
import com.arduia.expense.data.local.AboutUpdateDataModel
import com.arduia.expense.model.FlowResult
import com.arduia.expense.model.Result
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

interface SettingsRepository{

    fun getSelectedLanguage(): FlowResult<String>

    suspend fun setSelectedLanguage(id: String)

    suspend fun getSelectedLanguageSync(): Result<String>

    fun getFirstUser(): FlowResult<Boolean>

    suspend fun getFirstUserSync():  Result<Boolean>

    suspend fun setFirstUser(isFirstUser: Boolean)

    fun getSelectedCurrencyNumber(): FlowResult<String>

    suspend fun getSelectedCurrencyNumberSync(): Result<String>

    suspend fun setSelectedCurrencyNumber(num: String)

    suspend fun setSelectedThemeMode(mode: Int)

    suspend fun getSelectedThemeModeSync(): Result<Int>

    fun getUpdateStatus(): FlowResult<Int>

    suspend fun setUpdateStatus(status: Int)

    suspend fun getAboutUpdateSync(): Result<AboutUpdateDataModel>

    suspend fun setAboutUpdate(info: AboutUpdateDataModel)

    fun getUserName(): FlowResult<String>

    suspend fun getUserNameSync(): Result<String>

    suspend fun setUserName(name: String)

    suspend fun getLastSyncAt(): Result<Long>

    suspend fun setLastSyncAt(timeMillis: Long)

    suspend fun getLastAuthAt(): Result<Long>

    suspend fun setLastAuthAt(timeMillis: Long)

    suspend fun getLastUserId(): Result<String>

    suspend fun setLastUserId(userId: String)

    interface Factory{
        fun create(context: Context): SettingsRepository
    }
}
