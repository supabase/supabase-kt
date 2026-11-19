package io.supabase.auth

import io.supabase.auth.status.SessionStatus
import io.supabase.logging.d
import kotlinx.coroutines.launch
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.UIKit.UIApplicationDidEnterBackgroundNotification
import platform.UIKit.UIApplicationWillEnterForegroundNotification

/**
 * iOS and tvOS suspend the process while the app is in the background, which
 * freezes the delay-based auto-refresh timer. Without lifecycle callbacks the
 * timer can sleep through the access token's expiry, leaving every request
 * with an expired JWT after the app returns to the foreground - until the
 * user signs out and back in.
 *
 * This mirrors the Android implementation: cancel auto refresh when entering
 * the background, and re-import the session from storage when returning to
 * the foreground - refreshing immediately if the token went stale while
 * suspended, or re-arming the timer with the correct remaining time if not.
 */
internal actual suspend fun Auth.setupApplePlatform() {
    addLifecycleCallbacks(this)
    initDone()
}

private fun addLifecycleCallbacks(auth: Auth) {
    if(!auth.config.enableLifecycleCallbacks) return
    val center = NSNotificationCenter.defaultCenter
    val scope = auth.authScope
    center.addObserverForName(
        name = UIApplicationWillEnterForegroundNotification,
        `object` = null,
        queue = NSOperationQueue.mainQueue
    ) { _ ->
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
                } else {
                    auth.logger.d {
                        "Session found, auto refresh started"
                    }
                }
            }
        }
    }
    center.addObserverForName(
        name = UIApplicationDidEnterBackgroundNotification,
        `object` = null,
        queue = NSOperationQueue.mainQueue
    ) { _ ->
        if(auth.isAutoRefreshRunning) {
            auth.logger.d { "Cancelling auto refresh because app is switching to the background" }
            scope.launch {
                auth.stopAutoRefreshForCurrentSession()
                auth.setSessionStatus(SessionStatus.Initializing)
            }
        }
    }
}
