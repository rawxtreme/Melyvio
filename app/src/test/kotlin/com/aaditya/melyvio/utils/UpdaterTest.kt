package com.aaditya.melyvio.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class UpdaterTest {
    @Test
    fun parsesLatestReleaseAndPicksApk() {
        val response =
            """
            {
              "tag_name": "v1.2.3",
              "name": "Melyvio 1.2.3",
              "body": null,
              "html_url": "https://github.com/rawxtreme/Melyvio/releases/tag/v1.2.3",
              "published_at": "2026-09-05T12:00:00Z",
              "assets": [
                {"name": "notes.txt", "browser_download_url": "https://example.com/notes.txt", "size": 1},
                {"name": "Melyvio.apk", "browser_download_url": "https://example.com/Melyvio.apk", "size": 42}
              ]
            }
            """.trimIndent()
        val release = Updater.parseRelease(response)

        assertEquals("v1.2.3", release.tagName)
        assertEquals("", release.description)
        assertEquals("https://example.com/Melyvio.apk", Updater.getDownloadUrlForCurrentVariant(release))
    }

    @Test
    fun comparesVersionTags() {
        assertTrue(Updater.isUpdateAvailable("1.0.0", "v1.0.1"))
        assertFalse(Updater.isUpdateAvailable("1.0.0", "v1.0.0"))
        assertFalse(Updater.isUpdateAvailable("1.2.0", "v1.1.9"))
    }
}
