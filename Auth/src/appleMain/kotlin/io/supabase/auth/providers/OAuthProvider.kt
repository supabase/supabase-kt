package io.supabase.auth.providers

import io.supabase.auth.Auth
import platform.Foundation.NSURL

internal expect fun Auth.openUrl(url: NSURL)