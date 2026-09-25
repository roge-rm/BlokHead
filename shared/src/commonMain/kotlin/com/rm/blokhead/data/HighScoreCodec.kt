package com.rm.blokhead.data

/** The high-score table's storage format: one "score|name" line per entry. Shared so the Android
 *  DataStore and the browser's localStorage hold the table the same way. */
object HighScoreCodec {
    // "|" can't appear in a name (stripped below) so it's a safe field separator; each entry
    // is one line.
    fun encode(entries: List<HighScoreEntry>): String =
        entries.joinToString("\n") { "${it.score}|${sanitize(it.name)}" }

    fun decode(raw: String): List<HighScoreEntry> {
        if (raw.isBlank()) return emptyList()
        return raw.lineSequence().mapNotNull { line ->
            val separator = line.indexOf('|')
            if (separator < 0) return@mapNotNull null
            val score = line.substring(0, separator).toIntOrNull() ?: return@mapNotNull null
            HighScoreEntry(line.substring(separator + 1), score)
        }.toList()
    }

    private fun sanitize(name: String): String = name.replace("|", "").replace("\n", "").trim()
}
