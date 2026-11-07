/**
 * Melyvio Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.aaditya.melyvio.utils

import android.content.Context
import android.net.Uri
import com.aaditya.melyvio.db.MusicDatabase
import com.aaditya.melyvio.innertube.YouTube
import com.aaditya.melyvio.models.toMediaMetadata
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.InputStream

/**
 * The Melyvio playlist backup file. It is a readable CSV with a Melyvio signature on the first
 * line, so only files exported from Melyvio can be imported back.
 */
object MelyvioPlaylistFormat {
    const val SIGNATURE = "#MELYVIO-PLAYLIST"
    const val HEADER = "Title,Artist,Album,YouTube Video ID"

    data class Row(
        val title: String,
        val artist: String,
        val album: String,
        val id: String,
    )

    private fun singleLine(text: String): String = text.replace(Regex("[\\u0000-\\u001F\\u007F]+"), " ").trim()

    /** Quotes a CSV field. A leading = + - @ gets a ' in front so spreadsheets never run it as a formula. */
    fun csvField(text: String): String {
        var t = singleLine(text)
        if (t.isNotEmpty() && t[0] in "=+-@") t = "'$t"
        return "\"" + t.replace("\"", "\"\"") + "\""
    }

    fun build(
        name: String,
        rows: List<Row>,
    ): String =
        buildString {
            append(SIGNATURE).append(" v1\n")
            append("#Playlist: ").append(singleLine(name).take(100)).append('\n')
            append("#Songs: ").append(rows.size).append('\n')
            append("#Made with Melyvio. To restore this playlist: create a new playlist in Melyvio and choose Import playlist.\n")
            append(HEADER).append('\n')
            rows.forEach { row ->
                append(csvField(row.title)).append(',')
                append(csvField(row.artist)).append(',')
                append(csvField(row.album)).append(',')
                append(row.id).append('\n')
            }
        }
}

data class ImportEntry(
    val videoId: String,
    val title: String,
    val artist: String,
)

data class ParsedPlaylist(
    val name: String,
    val entries: List<ImportEntry>,
)

sealed interface ParseResult {
    data class Ok(
        val playlist: ParsedPlaylist,
    ) : ParseResult

    data object NotMelyvio : ParseResult

    data object Empty : ParseResult

    data object TooLarge : ParseResult

    data object Unreadable : ParseResult
}

object PlaylistImportParser {
    const val MAX_BYTES = 2 * 1024 * 1024
    const val MAX_SONGS = 5000
    private val ID_REGEX = Regex("^[A-Za-z0-9_-]{11}$")

    fun readAndParse(
        context: Context,
        uri: Uri,
    ): ParseResult =
        try {
            val bytes = context.contentResolver.openInputStream(uri)?.use { readLimited(it) }
            when {
                bytes == null -> ParseResult.Unreadable
                bytes.size > MAX_BYTES -> ParseResult.TooLarge
                else -> parse(String(bytes, Charsets.UTF_8))
            }
        } catch (e: Exception) {
            ParseResult.Unreadable
        }

    /** Reads at most MAX_BYTES; a bigger file comes back as MAX_BYTES + 1 bytes (= too large). */
    private fun readLimited(input: InputStream): ByteArray {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        var total = 0
        while (true) {
            val n = input.read(buffer)
            if (n < 0) break
            total += n
            if (total > MAX_BYTES) return ByteArray(MAX_BYTES + 1)
            out.write(buffer, 0, n)
        }
        return out.toByteArray()
    }

    private fun clean(
        text: String,
        max: Int,
    ): String = text.replace(Regex("[\\u0000-\\u001F\\u007F]+"), " ").trim().take(max)

    private fun unguard(text: String): String = if (text.length >= 2 && text[0] == '\'' && text[1] in "=+-@") text.substring(1) else text

    fun parse(raw: String): ParseResult {
        val lines = raw.removePrefix("\uFEFF").replace("\r\n", "\n").replace('\r', '\n').split('\n')
        val first = lines.firstOrNull { it.isNotBlank() }?.trim() ?: return ParseResult.NotMelyvio
        if (!first.startsWith(MelyvioPlaylistFormat.SIGNATURE)) return ParseResult.NotMelyvio

        var name = ""
        var headerSeen = false
        val entries = LinkedHashMap<String, ImportEntry>()
        for (line in lines) {
            val t = line.trim()
            if (t.isEmpty()) continue
            if (t.startsWith("#")) {
                if (t.startsWith("#Playlist:")) name = clean(t.removePrefix("#Playlist:"), 100)
                continue
            }
            if (!headerSeen) {
                if (!t.equals(MelyvioPlaylistFormat.HEADER, ignoreCase = true)) return ParseResult.NotMelyvio
                headerSeen = true
                continue
            }
            val fields = parseCsvLine(t)
            if (fields.size < 4) continue
            val id = fields[3].trim()
            if (!ID_REGEX.matches(id)) continue
            if (entries.size >= MAX_SONGS) return ParseResult.TooLarge
            if (!entries.containsKey(id)) {
                entries[id] = ImportEntry(id, unguard(clean(fields[0], 200)), unguard(clean(fields[1], 200)))
            }
        }
        if (!headerSeen) return ParseResult.NotMelyvio
        if (entries.isEmpty()) return ParseResult.Empty
        if (name.isBlank()) name = "Imported playlist"
        return ParseResult.Ok(ParsedPlaylist(name, entries.values.toList()))
    }

    private fun parseCsvLine(line: String): List<String> {
        val out = ArrayList<String>()
        val sb = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            if (c == '"') {
                if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                    sb.append('"')
                    i++
                } else {
                    inQuotes = !inQuotes
                }
            } else if (c == ',' && !inQuotes) {
                out.add(sb.toString())
                sb.clear()
            } else {
                sb.append(c)
            }
            i++
        }
        out.add(sb.toString())
        return out
    }
}

enum class ImportStage { CHECKING, FETCHING, SEARCHING, ADDING }

sealed interface ImportState {
    data object Idle : ImportState

    data class Running(
        val name: String,
        val stage: ImportStage,
        val done: Int,
        val total: Int,
    ) : ImportState

    data class Finished(
        val name: String,
        val added: Int,
        val total: Int,
        val failed: List<ImportEntry>,
    ) : ImportState
}

/**
 * Imports a Melyvio playlist file into a playlist. It lives outside any screen, so the import keeps
 * going (and shows its loading screen) even after the create-playlist dialog has closed.
 *
 * Every song is found by its YouTube id first; if that fails the song is searched by name. Only
 * songs that cannot be found at all are reported, so the user can add those by hand.
 */
object PlaylistImportManager {
    private val _state = MutableStateFlow<ImportState>(ImportState.Idle)
    val state: StateFlow<ImportState> = _state.asStateFlow()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun isBusy(): Boolean = _state.value is ImportState.Running

    fun dismiss() {
        if (_state.value is ImportState.Finished) _state.value = ImportState.Idle
    }

    fun start(
        database: MusicDatabase,
        playlistId: String,
        name: String,
        entries: List<ImportEntry>,
    ) {
        if (isBusy() || entries.isEmpty()) return
        _state.value = ImportState.Running(name, ImportStage.CHECKING, 0, entries.size)
        scope.launch { run(database, playlistId, name, entries) }
    }

    private fun normalizeForMatch(s: String): String =
        s
            .lowercase()
            .filter { it.isLetterOrDigit() || it == ' ' }
            .replace(Regex("\\s+"), " ")
            .trim()

    private fun similar(
        found: String,
        wanted: String,
    ): Boolean {
        val a = normalizeForMatch(found)
        val b = normalizeForMatch(wanted)
        if (a.isEmpty() || b.isEmpty()) return false
        if (a == b || a.contains(b) || b.contains(a)) return true
        val ta = a.split(' ').toSet()
        val tb = b.split(' ').toSet()
        return ta.intersect(tb).size.toFloat() / maxOf(ta.size, tb.size) >= 0.6f
    }

    private fun update(
        name: String,
        stage: ImportStage,
        done: Int,
        total: Int,
    ) {
        _state.value = ImportState.Running(name, stage, done, total)
    }

    private suspend fun run(
        database: MusicDatabase,
        playlistId: String,
        name: String,
        entries: List<ImportEntry>,
    ) {
        val resolved = HashMap<String, String>() // id in the file -> song id in the library
        var added = 0
        try {
            // 1) which songs are already in this phone's library
            val missing = ArrayList<ImportEntry>()
            entries.forEachIndexed { index, e ->
                if (database.song(e.videoId).first() != null) resolved[e.videoId] = e.videoId else missing += e
                if (index % 20 == 0 || index == entries.lastIndex) update(name, ImportStage.CHECKING, index + 1, entries.size)
            }

            // 2) get the missing songs by their YouTube id
            if (missing.isNotEmpty()) {
                update(name, ImportStage.FETCHING, 0, missing.size)
                var fetched = 0
                for (chunk in missing.chunked(40)) {
                    val wanted = chunk.map { it.videoId }.toSet()
                    val items = YouTube.queue(videoIds = chunk.map { it.videoId }).getOrNull().orEmpty()
                    items.filter { it.id in wanted }.forEach { item ->
                        runCatching { database.insert(item.toMediaMetadata()) }.onSuccess { resolved[item.id] = item.id }
                    }
                    fetched += chunk.size
                    update(name, ImportStage.FETCHING, fetched, missing.size)
                }
            }

            // 3) still missing (removed / unavailable): search by name to add as many as possible
            val stillMissing = missing.filter { it.videoId !in resolved }
            val failed = ArrayList<ImportEntry>()
            if (stillMissing.isNotEmpty()) {
                update(name, ImportStage.SEARCHING, 0, stillMissing.size)
                var networkFailures = 0
                stillMissing.forEachIndexed { index, e ->
                    if (networkFailures >= 3) {
                        failed += e
                    } else {
                        val items =
                            YouTube
                                .search("${e.title} ${e.artist}".trim(), YouTube.SearchFilter.FILTER_SONG)
                                .getOrNull()
                                ?.items
                                ?.filterIsInstance<com.aaditya.melyvio.innertube.models.SongItem>()
                        if (items == null) {
                            networkFailures++
                            failed += e
                        } else {
                            networkFailures = 0
                            val match = items.firstOrNull { similar(it.title, e.title) }
                            if (match != null && runCatching { database.insert(match.toMediaMetadata()) }.isSuccess) {
                                resolved[e.videoId] = match.id
                            } else {
                                failed += e
                            }
                        }
                    }
                    update(name, ImportStage.SEARCHING, index + 1, stillMissing.size)
                }
            }

            // 4) put everything into the playlist, in the order of the file
            update(name, ImportStage.ADDING, 0, 0)
            val ids = entries.mapNotNull { resolved[it.videoId] }.distinct()
            if (ids.isNotEmpty()) {
                var playlist = database.playlist(playlistId).first()
                var tries = 0
                while (playlist == null && tries < 10) {
                    delay(200)
                    playlist = database.playlist(playlistId).first()
                    tries++
                }
                if (playlist != null) {
                    database.addSongsToPlaylist(playlist, ids.map { it to null })
                    added = ids.size
                } else {
                    failed += entries.filter { it.videoId in resolved }
                }
            }
            _state.value = ImportState.Finished(name, added, entries.size, failed)
        } catch (e: kotlin.coroutines.cancellation.CancellationException) {
            throw e
        } catch (e: Exception) {
            val unresolved = entries.filter { it.videoId !in resolved }
            _state.value = ImportState.Finished(name, added, entries.size, unresolved)
        }
    }
}
