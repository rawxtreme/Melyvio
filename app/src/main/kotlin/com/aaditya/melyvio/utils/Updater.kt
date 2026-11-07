/**
 * Melyvio Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.aaditya.melyvio.utils

import com.aaditya.melyvio.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class ReleaseInfo(
    val tagName: String,
    val versionName: String,
    val description: String,
    val releaseDate: String,
    val htmlUrl: String,
    val assets: List<ReleaseAsset>,
)

data class ReleaseAsset(
    val name: String,
    val downloadUrl: String,
    val size: Long,
)

/**
 * Checks https://github.com/rawxtreme/Melyvio for the latest release tag.
 */
object Updater {
    const val REPO_URL = "https://github.com/rawxtreme/Melyvio"
    private const val LATEST_RELEASE_API = "https://api.github.com/repos/rawxtreme/Melyvio/releases/latest"
    const val RELEASES_PAGE_URL = "$REPO_URL/releases/latest"

    var lastCheckTime = -1L
        private set

    @Volatile
    private var cachedReleaseInfo: ReleaseInfo? = null

    /** 1 if v1 > v2, -1 if v1 < v2, 0 if equal. Ignores a leading "v" and any "+build" / "-suffix". */
    fun compareVersions(
        v1: String,
        v2: String,
    ): Int {
        fun parts(v: String) =
            v
                .trim()
                .removePrefix("v")
                .removePrefix("V")
                .substringBefore("+")
                .substringBefore("-")
                .split(".")
                .map { it.toIntOrNull() ?: 0 }

        val a = parts(v1)
        val b = parts(v2)
        for (i in 0 until maxOf(a.size, b.size)) {
            val p1 = a.getOrNull(i) ?: 0
            val p2 = b.getOrNull(i) ?: 0
            if (p1 > p2) return 1
            if (p1 < p2) return -1
        }
        return 0
    }

    fun isUpdateAvailable(
        currentVersion: String,
        latestVersion: String,
    ): Boolean = compareVersions(latestVersion, currentVersion) > 0

    internal fun parseAssets(assetsArray: JSONArray): List<ReleaseAsset> {
        val assets = mutableListOf<ReleaseAsset>()
        for (i in 0 until assetsArray.length()) {
            val asset = assetsArray.getJSONObject(i)
            val name = asset.getString("name")
            if (!name.endsWith(".apk", ignoreCase = true)) continue
            assets.add(
                ReleaseAsset(
                    name = name,
                    downloadUrl = asset.getString("browser_download_url"),
                    size = asset.optLong("size", 0L),
                ),
            )
        }
        return assets
    }

    internal fun parseRelease(response: String): ReleaseInfo {
        val json = JSONObject(response)
        val tag = json.getString("tag_name")
        val name = json.optString("name").takeUnless { json.isNull("name") || it.isBlank() } ?: tag
        return ReleaseInfo(
            tagName = tag,
            versionName = name.removePrefix("v"),
            description = json.optString("body").takeUnless { json.isNull("body") }.orEmpty(),
            releaseDate = json.optString("published_at"),
            htmlUrl = json.optString("html_url").ifBlank { RELEASES_PAGE_URL },
            assets = parseAssets(json.optJSONArray("assets") ?: JSONArray()),
        )
    }

    private fun httpGet(url: String): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = 15_000
            conn.readTimeout = 15_000
            conn.setRequestProperty("Accept", "application/vnd.github+json")
            conn.setRequestProperty("User-Agent", "Melyvio-Updater")
            val code = conn.responseCode
            if (code !in 200..299) throw IllegalStateException("GitHub returned HTTP $code")
            return conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    /** Fetches the latest release (tag) from GitHub. */
    suspend fun getLatestRelease(forceRefresh: Boolean = false): Result<ReleaseInfo> =
        withContext(Dispatchers.IO) {
            runCatching {
                cachedReleaseInfo?.takeIf { !forceRefresh }?.let { return@runCatching it }
                val release = parseRelease(httpGet(LATEST_RELEASE_API))
                cachedReleaseInfo = release
                lastCheckTime = System.currentTimeMillis()
                release
            }
        }

    /** The APK to download: first .apk asset, otherwise the release page. */
    fun getDownloadUrlForCurrentVariant(releaseInfo: ReleaseInfo): String? =
        releaseInfo.assets.firstOrNull()?.downloadUrl ?: releaseInfo.htmlUrl

    /** Compares the latest GitHub release tag with the installed version. Returns (release, hasUpdate). */
    suspend fun checkForUpdate(forceRefresh: Boolean = false): Result<Pair<ReleaseInfo?, Boolean>> =
        getLatestRelease(forceRefresh).map { release ->
            release to isUpdateAvailable(BuildConfig.BASE_VERSION_NAME, release.tagName)
        }

    fun getCachedLatestRelease(): ReleaseInfo? = cachedReleaseInfo
}
