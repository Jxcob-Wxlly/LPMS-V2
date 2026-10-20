package com.teampointblank.lpms

import android.content.Context
import com.teampointblank.lpms.data.ApiProvider
import com.teampointblank.lpms.data.AuthManager
import com.teampointblank.lpms.data.LpmsRepository
import com.teampointblank.lpms.data.SettingsStore
import com.teampointblank.lpms.data.TokenStore

/** Hand-wired dependencies (small app, so no DI framework). Created once by [LpmsApplication]. */
class AppContainer(context: Context) {
    val settings = SettingsStore(context)
    private val tokenStore = TokenStore(context)
    val auth = AuthManager(tokenStore)
    private val apiProvider = ApiProvider(settings) { auth.currentToken() }
    val repository = LpmsRepository(apiProvider, auth)
}
