/**
 * Melyvio Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.aaditya.melyvio.utils

import com.aaditya.melyvio.R
import androidx.annotation.DrawableRes
import android.database.Cursor
import androidx.sqlite.db.SupportSQLiteDatabase
import com.aaditya.melyvio.db.MusicDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

data class DnaArtist(
    val id: String,
    val name: String,
    val thumbnailUrl: String?,
    val plays: Int,
)

data class DnaSong(
    val id: String,
    val title: String,
    val artist: String,
    val thumbnailUrl: String?,
    val plays: Int,
)

data class DnaGenre(
    val name: String,
    val percent: Int,
)

data class DnaDay(
    val label: String,
    val minutes: Int,
)

data class DnaPersona(
    @DrawableRes val icon: Int,
    val title: String,
    val description: String,
)

data class MusicDnaData(
    val totalMinutes: Long,
    val totalPlays: Int,
    val uniqueSongs: Int,
    val topArtists: List<DnaArtist>,
    val topSongs: List<DnaSong>,
    val genres: List<DnaGenre>,
    val weekly: List<DnaDay>,
    val liked: List<DnaSong>,
    val discovered: List<DnaArtist>,
    val persona: DnaPersona,
) {
    val isEmpty: Boolean get() = totalPlays == 0
    val weeklyMinutes: Long get() = weekly.sumOf { it.minutes.toLong() }
    val favouriteGenre: String get() = genres.firstOrNull()?.name ?: "Mixed"
}

enum class DnaPeriod(
    val days: Int?,
) {
    WEEK(7),
    MONTH(30),
    ALL(null),
}

/** "18h 20m" / "45m" */
fun formatDnaDuration(totalMinutes: Long): String {
    val h = totalMinutes / 60
    val m = totalMinutes % 60
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}

/**
 * Music DNA is worked out on the phone from the listening history (the "event" table) that the app
 * already keeps. Nothing leaves the device. The history can be paused or cleared by the user.
 */
object MusicDna {
    private fun LocalDateTime.toDbMillis(): Long = atZone(ZoneOffset.UTC).toInstant().toEpochMilli()

    private inline fun <T> SupportSQLiteDatabase.rows(
        sql: String,
        args: Array<Any?> = emptyArray(),
        map: (Cursor) -> T,
    ): List<T> {
        val out = ArrayList<T>()
        query(sql, args).use { c ->
            while (c.moveToNext()) out.add(map(c))
        }
        return out
    }

    private const val ARTIST_OF_SONG =
        "(SELECT a.name FROM song_artist_map m JOIN artist a ON a.id = m.artistId " +
            "WHERE m.songId = s.id ORDER BY m.position LIMIT 1)"

    suspend fun compute(
        database: MusicDatabase,
        period: DnaPeriod,
    ): MusicDnaData =
        withContext(Dispatchers.IO) {
            runCatching { calculate(database.openHelper.readableDatabase, period) }
                .getOrElse { empty() }
        }

    private fun empty() =
        MusicDnaData(
            totalMinutes = 0,
            totalPlays = 0,
            uniqueSongs = 0,
            topArtists = emptyList(),
            topSongs = emptyList(),
            genres = emptyList(),
            weekly = lastSevenDays().map { DnaDay(it.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()), 0) },
            liked = emptyList(),
            discovered = emptyList(),
            persona = Persona.NEW,
        )

    private fun lastSevenDays(): List<LocalDate> {
        val today = LocalDate.now()
        return (6 downTo 0).map { today.minusDays(it.toLong()) }
    }

    private fun calculate(
        db: SupportSQLiteDatabase,
        period: DnaPeriod,
    ): MusicDnaData {
        val from = period.days?.let { LocalDateTime.now().minusDays(it.toLong()).toDbMillis() } ?: 0L

        val totals =
            db.rows("SELECT COUNT(*), COALESCE(SUM(playTime), 0), COUNT(DISTINCT songId) FROM event WHERE timestamp >= ?", arrayOf(from)) {
                Triple(it.getInt(0), it.getLong(1), it.getInt(2))
            }.firstOrNull() ?: Triple(0, 0L, 0)
        val totalPlays = totals.first
        if (totalPlays == 0) return empty().copy(liked = likedSongs(db))
        val totalMinutes = totals.second / 60_000L

        val topArtists =
            db.rows(
                """
                SELECT a.id, a.name, a.thumbnailUrl, COUNT(*) AS c
                FROM event e
                JOIN song_artist_map m ON m.songId = e.songId
                JOIN artist a ON a.id = m.artistId
                WHERE e.timestamp >= ?
                GROUP BY a.id
                ORDER BY c DESC, SUM(e.playTime) DESC
                LIMIT 5
                """.trimIndent(),
                arrayOf(from),
            ) { DnaArtist(it.getString(0), it.getString(1), it.getString(2), it.getInt(3)) }

        val topSongs =
            db.rows(
                """
                SELECT s.id, s.title, s.thumbnailUrl, COUNT(*) AS c, $ARTIST_OF_SONG AS artist
                FROM event e
                JOIN song s ON s.id = e.songId
                WHERE e.timestamp >= ?
                GROUP BY s.id
                ORDER BY c DESC
                LIMIT 5
                """.trimIndent(),
                arrayOf(from),
            ) { DnaSong(it.getString(0), it.getString(1), it.getString(4) ?: "", it.getString(2), it.getInt(3)) }

        // genre + mood are guessed from artist names and song titles (the database has no genre column)
        data class Row(
            val text: String,
            val title: String,
            val plays: Int,
        )
        val songRows =
            db.rows(
                """
                SELECT s.title, COALESCE(s.albumName, ''),
                       COALESCE((SELECT GROUP_CONCAT(a.name, ' ') FROM song_artist_map m JOIN artist a ON a.id = m.artistId WHERE m.songId = s.id), ''),
                       COUNT(*) AS c
                FROM event e
                JOIN song s ON s.id = e.songId
                WHERE e.timestamp >= ?
                GROUP BY s.id
                ORDER BY c DESC
                LIMIT 400
                """.trimIndent(),
                arrayOf(from),
            ) { Row(it.getString(0) + " " + it.getString(1) + " " + it.getString(2), it.getString(0), it.getInt(3)) }

        val genrePlays = HashMap<String, Int>()
        var romanticPlays = 0
        var rowPlays = 0
        for (row in songRows) {
            rowPlays += row.plays
            GenreGuesser.guess(row.text)?.let { genrePlays[it] = (genrePlays[it] ?: 0) + row.plays }
            if (GenreGuesser.isRomantic(row.title)) romanticPlays += row.plays
        }
        val matched = genrePlays.values.sum()
        val genres =
            if (rowPlays == 0 || matched * 100 < rowPlays * 15) {
                listOf(DnaGenre("Mixed", 100))
            } else {
                genrePlays.entries
                    .sortedByDescending { it.value }
                    .take(4)
                    .map { DnaGenre(it.key, ((it.value * 100f) / matched).roundToInt().coerceAtLeast(1)) }
            }

        // listening per day, last 7 days (always a week, whatever period is chosen)
        val days = lastSevenDays()
        val weekFrom = days.first().atStartOfDay().toDbMillis()
        val perDay =
            db
                .rows(
                    "SELECT strftime('%Y-%m-%d', timestamp / 1000, 'unixepoch'), COALESCE(SUM(playTime), 0) FROM event WHERE timestamp >= ? GROUP BY 1",
                    arrayOf(weekFrom),
                ) { it.getString(0) to it.getLong(1) }.toMap()
        val weekly =
            days.map {
                DnaDay(
                    label = it.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                    minutes = ((perDay[it.toString()] ?: 0L) / 60_000L).toInt(),
                )
            }

        // time of day (recorded clock hour) for the personality
        val hours = LongArray(24)
        db.rows("SELECT CAST(strftime('%H', timestamp / 1000, 'unixepoch') AS INTEGER), COALESCE(SUM(playTime), 0) FROM event WHERE timestamp >= ? GROUP BY 1", arrayOf(from)) {
            val h = it.getInt(0)
            if (h in 0..23) {
                hours[h] = it.getLong(1)
            }
            Unit
        }
        val hourTotal = hours.sum().coerceAtLeast(1L)
        val nightShare = ((22..23) + (0..4)).sumOf { hours[it] }.toFloat() / hourTotal
        val morningShare = (5..9).sumOf { hours[it] }.toFloat() / hourTotal

        val discovered =
            db.rows(
                """
                SELECT a.id, a.name, a.thumbnailUrl, COUNT(*) AS c
                FROM event e
                JOIN song_artist_map m ON m.songId = e.songId
                JOIN artist a ON a.id = m.artistId
                GROUP BY a.id
                HAVING MIN(e.timestamp) >= ?
                ORDER BY c DESC
                LIMIT 8
                """.trimIndent(),
                arrayOf(LocalDateTime.now().minusDays(30).toDbMillis()),
            ) { DnaArtist(it.getString(0), it.getString(1), it.getString(2), it.getInt(3)) }

        val topGenre = genres.first()
        val persona =
            Persona.pick(
                totalPlays = totalPlays,
                uniqueSongs = totals.third,
                topArtistShare = (topArtists.firstOrNull()?.plays ?: 0).toFloat() / totalPlays,
                topSongShare = (topSongs.firstOrNull()?.plays ?: 0).toFloat() / totalPlays,
                romanticShare = if (rowPlays == 0) 0f else romanticPlays.toFloat() / rowPlays,
                nightShare = nightShare,
                morningShare = morningShare,
                topGenre = topGenre.name,
                topGenreShare = topGenre.percent / 100f,
                discoveredCount = discovered.size,
            )

        return MusicDnaData(
            totalMinutes = totalMinutes,
            totalPlays = totalPlays,
            uniqueSongs = totals.third,
            topArtists = topArtists,
            topSongs = topSongs,
            genres = genres,
            weekly = weekly,
            liked = likedSongs(db),
            discovered = discovered,
            persona = persona,
        )
    }

    private fun likedSongs(db: SupportSQLiteDatabase): List<DnaSong> =
        runCatching {
            db.rows(
                "SELECT s.id, s.title, s.thumbnailUrl, $ARTIST_OF_SONG FROM song s WHERE s.liked = 1 ORDER BY s.likedDate DESC LIMIT 8",
            ) { DnaSong(it.getString(0), it.getString(1), it.getString(3) ?: "", it.getString(2), 0) }
        }.getOrDefault(emptyList())
}

private fun norm(s: String): String =
    s
        .lowercase()
        .map { if (it.isLetterOrDigit()) it else ' ' }
        .joinToString("")
        .trim()
        .replace(Regex("\\s+"), " ")

/** Rough genre guess from artist names and song titles. It is an estimate, not exact. */
object GenreGuesser {
    private val genres: List<Pair<String, List<String>>> =
        listOf(
            "Bollywood" to listOf("bollywood", "arijit singh", "atif aslam", "shreya ghoshal", "sonu nigam", "kumar sanu", "udit narayan", "alka yagnik", "neha kakkar", "jubin nautiyal", "darshan raval", "pritam", "a r rahman", "vishal shekhar", "armaan malik", "mohit chauhan", "sunidhi chauhan", "sachin jigar", "tulsi kumar", "palak muchhal", "stebin ben", "payal dev", "shilpa rao", "arijit", "hindi", "from the movie", "original motion picture", "t series"),
            "Punjabi" to listOf("punjabi", "diljit", "sidhu moose", "ap dhillon", "karan aujla", "guru randhawa", "hardy sandhu", "harrdy sandhu", "b praak", "jassi gill", "amrinder gill", "ammy virk", "gippy grewal", "jass manak", "sharry maan", "babbu maan", "shubh"),
            "Hip-Hop" to listOf("hip hop", "rap", "emiway", "raftaar", "divine", "krsna", "seedhe maut", "drake", "kendrick", "eminem", "travis scott", "badshah", "mc stan", "king", "50 cent"),
            "Pop" to listOf("taylor swift", "ed sheeran", "dua lipa", "justin bieber", "ariana grande", "billie eilish", "harry styles", "the weeknd", "bruno mars", "shawn mendes", "selena gomez", "olivia rodrigo", "sabrina carpenter", "charlie puth", "maroon 5", "adele", "sia", "katy perry", "lady gaga", "rihanna", "miley cyrus", "one direction", "camila cabello"),
            "Rock" to listOf("rock", "metal", "linkin park", "coldplay", "imagine dragons", "arctic monkeys", "nirvana", "guns n roses", "metallica", "queen", "ac dc", "green day", "foo fighters", "pink floyd", "led zeppelin", "indian ocean", "euphoria"),
            "EDM" to listOf("edm", "remix", "dj", "alan walker", "marshmello", "martin garrix", "avicii", "tiesto", "zedd", "calvin harris", "david guetta", "kygo", "skrillex", "swedish house mafia", "club mix", "dance"),
            "Lo-fi & Chill" to listOf("lofi", "lo fi", "chill", "slowed", "reverb", "relax", "sleep", "study", "ambient", "acoustic"),
            "Devotional" to listOf("bhajan", "aarti", "mantra", "hanuman", "chalisa", "shiv", "krishna", "radha", "devotional", "ganesh", "durga", "bhakti", "jai", "om"),
            "Ghazal & Sufi" to listOf("ghazal", "sufi", "jagjit singh", "nusrat", "rahat fateh", "qawwali", "ghulam ali", "pankaj udhas", "mehdi hassan", "abida parveen", "coke studio", "kabir"),
            "Classical" to listOf("classical", "instrumental", "raga", "raag", "piano", "symphony", "beethoven", "mozart", "bach", "sitar", "flute", "santoor"),
            "South Indian" to listOf("tamil", "telugu", "malayalam", "kannada", "anirudh", "sid sriram", "devi sri prasad", "thaman", "ilaiyaraaja", "harris jayaraj", "yuvan shankar raja", "gv prakash"),
            "K-Pop" to listOf("bts", "blackpink", "stray kids", "twice", "newjeans", "kpop", "k pop", "exo", "seventeen", "jungkook", "jimin"),
            "Retro" to listOf("retro", "old is gold", "evergreen", "90s", "80s", "70s", "60s", "r d burman", "lata mangeshkar", "mohammed rafi", "kishore kumar", "mukesh", "manna dey", "hemant kumar", "asha bhosle"),
        ).map { (name, words) -> name to words.map { norm(it) } }

    private val romantic =
        listOf("love", "pyaar", "pyar", "ishq", "mohabbat", "dil", "romantic", "sanam", "saathiya", "jaan", "husn", "tujhe", "tum hi", "tumhe", "humsafar", "raabta", "kabira", "heart", "kiss").map { norm(it) }

    private fun hit(
        padded: String,
        keyword: String,
    ): Boolean = if (keyword.length <= 4) padded.contains(" $keyword ") else padded.contains(keyword)

    fun guess(text: String): String? {
        val padded = " " + norm(text) + " "
        var best: String? = null
        var bestHits = 0
        for ((name, words) in genres) {
            var hits = 0
            for (w in words) if (hit(padded, w)) hits++
            if (hits > bestHits) {
                bestHits = hits
                best = name
            }
        }
        return best
    }

    fun isRomantic(title: String): Boolean {
        val padded = " " + norm(title) + " "
        return romantic.any { hit(padded, it) }
    }
}

object Persona {
    val NEW = DnaPersona(R.drawable.ic_mono_leaf, "The New Explorer", "Your Music DNA is just starting to grow. Keep listening and it gets sharper.")

    private val romantic = DnaPersona(R.drawable.favorite, "The Romantic Listener", "You live for love songs and soulful melodies. Every playlist of yours has a heart.")
    private val party = DnaPersona(R.drawable.graphic_eq, "The Party Starter", "Beats first. Your music keeps the energy up wherever you go.")
    private val chill = DnaPersona(R.drawable.ic_mono_leaf, "The Chill Soul", "Calm, mellow and thoughtful. You pick music that slows the world down.")
    private val devoted = DnaPersona(R.drawable.ic_mono_flame, "The Devoted Soul", "Music is peace for you. Your playlist is full of devotion and calm.")
    private val night = DnaPersona(R.drawable.bedtime, "The Night Owl", "The quieter the night, the louder your playlist. Most of your music plays after dark.")
    private val morning = DnaPersona(R.drawable.ic_mono_sun, "The Early Bird", "You start the day with music. Mornings are your favourite listening time.")
    private val loyal = DnaPersona(R.drawable.mic, "The Loyal Fan", "When you love an artist, you really love them. One voice leads your taste.")
    private val repeater = DnaPersona(R.drawable.repeat, "The Repeat Master", "A good song deserves another play. And another. And another.")
    private val explorer = DnaPersona(R.drawable.explore_outlined, "The Explorer", "Always finding something new. Your taste keeps travelling to new artists.")
    private val allRounder = DnaPersona(R.drawable.library_music, "The All-Rounder", "A little of everything. Your taste is wide and your playlists never get boring.")

    fun pick(
        totalPlays: Int,
        uniqueSongs: Int,
        topArtistShare: Float,
        topSongShare: Float,
        romanticShare: Float,
        nightShare: Float,
        morningShare: Float,
        topGenre: String,
        topGenreShare: Float,
        discoveredCount: Int,
    ): DnaPersona {
        if (totalPlays < 5) return NEW
        val candidates = ArrayList<Pair<Float, DnaPersona>>()
        candidates += romanticShare / 0.30f to romantic
        when (topGenre) {
            "EDM", "Hip-Hop", "Punjabi" -> candidates += topGenreShare / 0.40f to party
            "Lo-fi & Chill", "Ghazal & Sufi", "Classical" -> candidates += topGenreShare / 0.40f to chill
            "Devotional" -> candidates += topGenreShare / 0.40f to devoted
        }
        candidates += nightShare / 0.40f to night
        candidates += morningShare / 0.40f to morning
        candidates += topArtistShare / 0.40f to loyal
        candidates += topSongShare / 0.20f to repeater
        val explorerValue = if (uniqueSongs >= 15 && totalPlays > 0) (uniqueSongs.toFloat() / totalPlays) / 0.85f else 0f
        candidates += maxOf(explorerValue, discoveredCount / 8f) to explorer
        val best = candidates.maxByOrNull { it.first }
        return if (best != null && best.first >= 1f) best.second else allRounder
    }
}
