package tech.sourceid.sid_address_verification.domain

import android.content.Context
import org.json.JSONObject
import tech.sourceid.sid_address_verification.data.requests.AddGeoTagRequest

private const val PREFS_NAME = "geo_tag_cache"
private const val KEY_CACHED_TAGS = "cached_tags"

fun cacheGeoTag(context: Context, geoTag: AddGeoTagRequest) {
    val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val cached = sharedPrefs.getStringSet(KEY_CACHED_TAGS, mutableSetOf())?.toMutableSet() ?: mutableSetOf()
    cached.add(JSONObject().apply {
        put("address", geoTag.address)
        put("latitude", geoTag.latitude)
        put("longitude", geoTag.longitude)
        put("deviceTimestamp", geoTag.deviceTimestamp)
    }.toString())

    sharedPrefs.edit().putStringSet(KEY_CACHED_TAGS, cached).apply()
}

fun getCachedGeoTags(context: Context): List<AddGeoTagRequest> {
    val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val cached = sharedPrefs.getStringSet(KEY_CACHED_TAGS, setOf()) ?: setOf()

    return cached.mapNotNull {
        try {
            val json = JSONObject(it)
            AddGeoTagRequest(
                address = json.getString("address"),
                latitude = json.getDouble("latitude"),
                longitude = json.getDouble("longitude"),
                deviceTimestamp = json.getString("deviceTimestamp")
            )
        } catch (e: Exception) {
            null
        }
    }
}

fun clearCachedGeoTags(context: Context) {
    val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    sharedPrefs.edit().remove(KEY_CACHED_TAGS).apply()
}
