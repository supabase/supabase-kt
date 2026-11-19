package io.supabase.auth.url

import io.supabase.buildUrl

internal fun consumeUrlParameter(parameters: List<String>, url: String): String {
    return buildUrl(url) {
        parameters.forEach { parameter ->
            this.parameters.remove(parameter)
        }
    }
}