package io.supabase.postgrest.query

internal sealed interface AcceptHeader {
    data class Single(val maybe: Boolean): AcceptHeader {
        companion object {
            fun headerValue(stripNulls: Boolean) =
                "application/vnd.pgrst.object+json" + if(stripNulls) ";nulls=stripped" else ""
        }
    }
    data object Json: AcceptHeader {
        fun headerValue(stripNulls: Boolean) =
            if(stripNulls) "application/vnd.pgrst.array+json;nulls=stripped" else "application/json"
    }
    data object CSV: AcceptHeader {
        fun headerValue() = "text/csv"
    }
    data object GeoJson: AcceptHeader {
        fun headerValue() = "application/geo+json"
    }

}

internal data class ExplainData(
    val options: String,
    val format: String
) {

    fun headerValue(
        mediaType: String,
    ) = "application/vnd.pgrst.plan+${format}; for=\"${mediaType}\"; options=${options};"

}