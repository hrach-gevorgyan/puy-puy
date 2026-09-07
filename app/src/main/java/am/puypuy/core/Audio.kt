package am.puypuy.core

import android.content.Context
import android.media.AudioFocusRequest
import android.os.Build
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool

/**
 * §7.4. Two channels: speech and effects.
 *
 * Effects never interrupt speech; speech interrupts speech (latest wins).
 * There is no mute button — the volume keys are enough, and a visible mute
 * button will be found and pressed.
 *
 * Clips are looked up by `res/raw` name at runtime. A name with no file behind
 * it is silently a no-op, so the app stays playable while the voice clips are
 * still being generated.
 */
class Audio(private val context: Context) {

    private val attrs = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_GAME)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    private val effects = SoundPool.Builder()
        .setMaxStreams(6)
        .setAudioAttributes(attrs)
        .build()

    /** raw resource id -> SoundPool sample id, loaded on first use. */
    private val loaded = mutableMapOf<Int, Int>()
    private val resIds = mutableMapOf<String, Int>()

    /** Sample ids that have finished loading and can be played immediately. */
    private val ready = mutableSetOf<Int>()

    /** Sample ids asked for before they finished loading, with the volume asked for. */
    private val pending = mutableMapOf<Int, Float>()

    init {
        // ONE listener for the whole pool: setting it per load
        // meant each new sample silently disowned the previous sample's callback.
        effects.setOnLoadCompleteListener { pool, sampleId, status ->
            if (status != 0) return@setOnLoadCompleteListener
            ready += sampleId
            pending.remove(sampleId)?.let { v -> pool.play(sampleId, v, v, 1, 0, 1f) }
        }
    }

    private var speech: MediaPlayer? = null

    /**
     * While true, nothing plays at all.
     *
     * This is the only reliable way to keep him quiet when the app is not in front. Half the
     * app speaks *after a delay* — he answers a coconut 4.5 seconds later, names a fruit
     * 800ms after it is eaten — and those coroutines keep running while the app is in the
     * background. Stopping the current clip did nothing about the next one.
     */
    private var paused = false

    /**
     * Raised while a word is playing. hashvir's rule: one voice, one face — his mouth moves
     * whenever he is the one talking.
     */
    var onSpeaking: ((Boolean) -> Unit)? = null

    private fun resId(name: String): Int = resIds.getOrPut(name) {
        @Suppress("DiscouragedApi")
        context.resources.getIdentifier(name, "raw", context.packageName)
    }

    /** Fire and forget. Short clips only — pops, munches, animal noises. */
    fun effect(name: String, volume: Float = 1f) {
        if (paused) return
        val res = resId(name)
        if (res == 0) return
        ensureFocus()
        val sample = loaded[res]
        if (sample != null && ready.contains(sample)) {
            effects.play(sample, volume, volume, 1, 0, 1f)
            return
        }
        if (sample != null) {
            // Still loading. Queue it rather than dropping it silently: caching
            // the id immediately, so every play during the load window was a no-op.
            pending[sample] = volume
            return
        }
        val id = effects.load(context, res, 1)
        loaded[res] = id
        pending[id] = volume
    }

    /** A spoken word. Latest wins — a new word cuts off the one before it. */
    fun say(name: String) {
        if (paused) return
        val res = resId(name)
        if (res == 0) return
        ensureFocus()
        stopSpeech()
        // Same attributes as the effects channel; the default is MUSIC usage, which ducks
        // and routes differently from the rest of the app's sound.
        speech = MediaPlayer.create(context, res, attrs, focusManager.generateAudioSessionId())?.apply {
            setOnCompletionListener {
                it.release()
                if (speech === it) speech = null
                onSpeaking?.invoke(false)
            }
            start()
            onSpeaking?.invoke(true)
        }
    }

    private fun stopSpeech() {
        if (speech != null) onSpeaking?.invoke(false)
        speech?.let {
            runCatching { it.stop() }
            it.release()
        }
        speech = null
    }

    /** Called on every `onExit` — a game must never leave audio running. */
    fun stopAll() {
        stopSpeech()
        effects.autoPause()
    }

    /**
     * Called when the app leaves the foreground. Without this he carries on talking in a
     * pocket, over whatever the adult started playing instead.
     */
    fun pauseAll() {
        paused = true
        stopSpeech()
        effects.autoPause()
        abandonFocus()
    }

    fun resumeEffects() {
        paused = false
        effects.autoResume()
    }

    // --- audio focus -----------------------------------------------------------------
    // Requested lazily on the first sound and held while the app is in front. Transient
    // loss (a notification) ducks rather than stops: a three-year-old should not have the
    // game go silent because a message arrived.

    private val focusManager =
        context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager

    private var focusRequest: AudioFocusRequest? = null
    private var hasFocus = false

    private fun ensureFocus() {
        if (hasFocus) return
        hasFocus = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val request = AudioFocusRequest.Builder(android.media.AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(attrs)
                .setWillPauseWhenDucked(false)
                .setOnAudioFocusChangeListener { }
                .build()
            focusRequest = request
            focusManager.requestAudioFocus(request) ==
                android.media.AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            @Suppress("DEPRECATION")
            focusManager.requestAudioFocus(
                null,
                android.media.AudioManager.STREAM_MUSIC,
                android.media.AudioManager.AUDIOFOCUS_GAIN,
            ) == android.media.AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
    }

    private fun abandonFocus() {
        if (!hasFocus) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest?.let { focusManager.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            focusManager.abandonAudioFocus(null)
        }
        hasFocus = false
    }

    fun release() {
        stopSpeech()
        abandonFocus()
        effects.release()
    }
}

