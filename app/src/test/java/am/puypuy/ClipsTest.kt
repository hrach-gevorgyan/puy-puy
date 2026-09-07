package am.puypuy

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Every clip the code can ask for must exist in `res/raw`, and every clip in `res/raw` must be
 * asked for by something.
 *
 * Clips are addressed by NAME at runtime, several of them built by string concatenation, so
 * the compiler cannot help: a typo, a rename, or a `words.csv` edit is silent, and the only
 * symptom is a moment of unexplained quiet in a game. This test is the compiler for that.
 */
class ClipsTest {

    private val root = generateSequence(File(".").absoluteFile) { it.parentFile }
        .first { File(it, "tools/words.csv").exists() }

    private val raw = File(root, "app/src/main/res/raw")
    private val kotlin = File(root, "app/src/main/java")

    private fun clipsOnDisk(): Set<String> =
        raw.listFiles { f -> f.extension == "ogg" }.orEmpty().map { it.nameWithoutExtension }.toSet()

    private fun wordsCsv(): Set<String> =
        File(root, "tools/words.csv").readLines()
            .filter { it.isNotBlank() && !it.startsWith("#") }
            .map { it.substringBefore(",").trim() }
            .toSet()

    private fun sources(): List<String> =
        kotlin.walkTopDown().filter { it.extension == "kt" }.map { it.readText() }.toList()

    /**
     * Names the code can ask for that are deliberately not recorded yet. A missing clip is a
     * silent no-op by design, but it must be a KNOWN silence — see the end of docs/audio.md.
     */
    private val notYetRecorded = setOf(
        "munch", "brush", "wipe",
    ) + Animals.NAMES.map { "sfx_$it" }

    private object Animals {
        val NAMES = listOf(
            "crab", "fish", "seagull", "turtle", "starfish", "octopus",
            "frog", "butterfly", "snail", "bee", "duck", "cat",
        )
    }

    @Test
    fun `every clip in words_csv was generated`() {
        val missing = wordsCsv() - clipsOnDisk()
        assertTrue("words.csv lists clips that are not in res/raw: $missing", missing.isEmpty())
    }

    @Test
    fun `every praise clip the code plays exists`() {
        // Reward.kt builds these as "praise_$it" over a range; nothing checks them.
        val pattern = Regex("\\(1\\.\\.(\\d+)\\)\\.map")
        val top = pattern.find(sources().joinToString("\n"))?.groupValues?.get(1)?.toInt() ?: 8
        val onDisk = clipsOnDisk()
        for (i in 1..top) {
            assertTrue("praise_$i is played but not recorded", "praise_$i" in onDisk)
        }
    }

    @Test
    fun `every string literal passed to say or effect resolves`() {
        val src = sources().joinToString("\n")
        val literals = Regex("""(?:say|effect)\(\s*"([a-z0-9_]+)"""")
            .findAll(src).map { it.groupValues[1] }.toSet()
        val known = clipsOnDisk() + notYetRecorded
        val unknown = literals - known
        assertTrue(
            "these clip names are asked for but neither recorded nor listed as pending: $unknown",
            unknown.isEmpty(),
        )
    }

    @Test
    fun `every recorded clip is reachable from code`() {
        val src = sources().joinToString("\n")
        val onDisk = clipsOnDisk()
        // Names built by concatenation, which a literal search cannot see.
        val generated = (1..8).map { "praise_$it" } +
            (1..3).map { "star_$it" }
        // An exact literal, or one of the families built by concatenation. The previous
        // version also accepted the clip's PREFIX appearing anywhere in the source, so
        // "feed_yum" passed because some file mentioned "feed" — and ten clips outlived the
        // code that played them without this test noticing.
        val unreachable = onDisk.filter { clip -> clip !in generated && !src.contains("\"$clip\"") }
        assertTrue(
            "recorded but never played, so the shrinker is right to drop them: $unreachable",
            unreachable.isEmpty(),
        )
    }

    /**
     * docs/audio.md is the readable version of words.csv, and it is what anyone re-recording the
     * set in a real voice would work from. It had drifted to eighteen clips that no longer
     * existed and seventeen that did but were undocumented, so it is generated now — by
     * `tools/sync_audio_doc.py` — and this fails if somebody edits one without the other.
     */
    @Test
    fun `docs audio_md documents exactly the clips that exist`() {
        val doc = File(root, "docs/audio.md").readText()
        val documented = Regex("""\|\s*`([a-z0-9_]+)`\s*\|""").findAll(doc)
            .map { it.groupValues[1] }.toSet()
        val words = wordsCsv()
        assertTrue("docs/audio.md is missing: ${words - documented} — run tools/sync_audio_doc.py",
            (words - documented).isEmpty())
        assertTrue("docs/audio.md lists clips that no longer exist: ${documented - words - notYetRecorded}",
            (documented - words - notYetRecorded).isEmpty())
    }

    @Test
    fun `no clip name would break as an android resource name`() {
        val bad = clipsOnDisk().filterNot { Regex("^[a-z][a-z0-9_]*$").matches(it) }
        assertTrue("illegal res/raw names: $bad", bad.isEmpty())
    }
}
