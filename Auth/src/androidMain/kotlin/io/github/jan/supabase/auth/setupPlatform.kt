package io.github.jan.supabase.auth

import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.startup.Initializer
import io.github.jan.supabase.annotations.SupabaseInternal
import io.github.jan.supabase.logging.d
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private var appContext: Context? = null

internal class SupabaseInitializer : Initializer<Context> {
    override fun create(context: Context): Context = context.applicationContext.also { appContext = it }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()

}

internal fun applicationContext(): Context = appContext ?: error("Application context not initialized")

@SupabaseInternal
actual suspend fun Auth.setupPlatform() {
    addLifecycleCallbacks(this)
}

private fun addLifecycleCallbacks(auth: Auth) {
    if(!auth.config.enableLifecycleCallbacks) return auth.initDone()
    val lifecycle = ProcessLifecycleOwner.get().lifecycle
    val scope = auth.authScope
    scope.launch(Dispatchers.Main) {
        lifecycle.addObserver(
            object : DefaultLifecycleObserver {

                override fun onStart(owner: LifecycleOwner) {
                    if(!auth.isAutoRefreshRunning && auth.config.alwaysAutoRefresh) {
                        auth.logger.d {
                            "Trying to re-load session from storage..."
                        }
                        scope.launch {
                            val sessionFound = auth.loadFromStorage()
                            if(!sessionFound) {
                                auth.logger.d {
                                    "No session found, not starting auto refresh"
                                }
                                auth.initDone()
                            } else {
                                auth.logger.d {
                                    "Session found, auto refresh started"
                                }
                            }
                        }
                    }
                }
                override fun onStop(owner: LifecycleOwner) {
                    if(auth.isAutoRefreshRunning) {
                        auth.logger.d { "Cancelling auto refresh because app is switching to the background" }
                        scope.launch {
                            auth.stopAutoRefreshForCurrentSession()
                            auth.resetSessionStatusForBackground()
                        }
                    }
                }
            }
        )
    }
}
