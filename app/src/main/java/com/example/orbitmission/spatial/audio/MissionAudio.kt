package com.example.orbitmission.spatial.audio

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.pico.spatial.core.ecs.AmbientAudioComponent
import com.pico.spatial.core.ecs.Entity
import com.pico.spatial.core.ecs.LoadType
import com.pico.spatial.core.ecs.ObjectAudioComponent
import com.pico.spatial.core.ecs.TransformComponent
import com.pico.spatial.core.ecs.audio.AudioPlayerController
import com.pico.spatial.core.ecs.resource.AudioResource
import com.pico.spatial.core.math.Vector3
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield

/** Owns a small, reusable set of spatial emitters. All SDK calls run on the main thread. */
class MissionAudio : AutoCloseable {
    private data class Clip(val resource: AudioResource, val player: AudioPlayerController)
    private val main = Handler(Looper.getMainLooper())
    private val clips = mutableMapOf<String, Clip>()
    private var musicSource: Entity? = null
    private var voiceSource: Entity? = null
    private var effectSource: Entity? = null
    private var engineSource: Entity? = null
    private var currentVoice: Clip? = null
    private var currentEffect: Clip? = null
    private var initialized = false
    private var closed = false
    private var paused = false
    private var musicEnabled = true
    private var voiceEnabled = true
    private var sfxEnabled = true
    private var engineActive = false

    suspend fun initialize(root: Entity) = withContext(Dispatchers.Main.immediate) {
        if (closed || initialized) return@withContext
        initialized = true
        fun source(ambient: Boolean = false) = Entity().also {
            if (ambient) it.components.set(AmbientAudioComponent(volume = 0.25f))
            else it.components.set(ObjectAudioComponent())
            root.addChild(it)
        }
        musicSource = source(ambient = true)
        voiceSource = source().also { position(it, Vector3(0f, 1.5f, -1.5f)) }
        effectSource = source()
        engineSource = source()
        load(MUSIC, "audio/deep_space_loop_60s.wav", musicSource!!, loop = true, volume = 0.25f)
        yield()
        for (key in EFFECTS) {
            if (closed) return@withContext
            load(key, "audio/$key.wav", if (key == ENGINE) engineSource!! else effectSource!!,
                loop = key == ENGINE, volume = if (key == ENGINE) 0.45f else 0.7f)
            yield()
        }
        for (language in listOf("zh-Hans", "en")) {
            for (key in VOICES) {
                if (closed) return@withContext
                load("$language/$key", "audio/$language/$key.wav", voiceSource!!)
                yield()
            }
        }
        syncLoops()
    }

    fun playVoice(languageId: String, key: String) = onMain {
        currentVoice?.player?.stop()
        currentVoice = null
        if (!voiceEnabled || paused) return@onMain
        // Both Chinese UI variants deliberately use the same Simplified Chinese recording.
        val language = if (languageId.lowercase().startsWith("en")) "en" else "zh-Hans"
        val clip = getClip("$language/$key") ?: return@onMain
        currentVoice = clip
        start(clip, "$language/$key")
    }

    fun playEffect(key: String, position: Vector3? = null) = onMain {
        if (!sfxEnabled || paused) return@onMain
        if (key == ENGINE) {
            Log.e(TAG, "Use setEngine() for the looping engine clip")
            return@onMain
        }
        val clip = getClip(key) ?: return@onMain
        currentEffect?.player?.stop()
        currentEffect = clip
        this.position(effectSource, position ?: Vector3(0f, 1.4f, -1.5f))
        start(clip, key)
    }

    fun setEngine(active: Boolean, position: Vector3) = onMain {
        engineActive = active
        this.position(engineSource, position)
        syncLoops()
    }

    fun setEnabled(music: Boolean, voice: Boolean, sfx: Boolean) = onMain {
        musicEnabled = music
        voiceEnabled = voice
        sfxEnabled = sfx
        if (!voice) { currentVoice?.player?.stop(); currentVoice = null }
        if (!sfx) { currentEffect?.player?.stop(); currentEffect = null }
        syncLoops()
    }

    fun pause() = onMain {
        paused = true
        clips.values.forEach { it.player.stop() }
        currentVoice = null
        currentEffect = null
    }

    fun resume() = onMain {
        paused = false
        syncLoops() // Never replay a previous instruction or one-shot effect.
    }

    override fun close() = onMain {
        closed = true
        clips.values.forEach { clip ->
            runCatching { clip.player.stop(); clip.player.close() }
                .onFailure { Log.e(TAG, "Audio controller release failed", it) }
            runCatching { clip.resource.close() }
                .onFailure { Log.e(TAG, "Audio resource release failed", it) }
        }
        clips.clear()
        listOf(musicSource, voiceSource, effectSource, engineSource).forEach { entity ->
            runCatching { entity?.destroy() }
                .onFailure { Log.e(TAG, "Audio source release failed", it) }
        }
        musicSource = null; voiceSource = null; effectSource = null; engineSource = null
        currentVoice = null; currentEffect = null
        initialized = false
    }

    private fun load(id: String, path: String, source: Entity, loop: Boolean = false, volume: Float = 1f) {
        var resource: AudioResource? = null
        var player: AudioPlayerController? = null
        try {
            resource = AudioResource.load("orbit/$id", "asset://$path", LoadType.FROM_ASSETS)
            check(resource.valid) { "Invalid audio resource" }
            player = source.prepareAudio(resource)
            check(player.valid) { "Invalid audio player" }
            check(player.setLoop(loop)) { "Could not configure looping" }
            check(player.setVolume(volume)) { "Could not configure volume" }
            clips[id] = Clip(resource, player)
        } catch (error: Exception) {
            runCatching { player?.close() }
            runCatching { resource?.close() }
            Log.e(TAG, "Could not load shipped audio asset asset://$path", error)
        }
    }

    private fun getClip(key: String): Clip? = clips[key].also {
        if (it == null) Log.e(TAG, "Audio clip unavailable: $key (initialized=$initialized)")
    }

    private fun start(clip: Clip, key: String) {
        if (!clip.player.play()) Log.e(TAG, "Audio playback failed: $key")
    }

    private fun syncLoops() {
        fun sync(key: String, enabled: Boolean) {
            val clip = clips[key] ?: return
            if (enabled && !paused) {
                if (!clip.player.isPlaying()) start(clip, key)
            } else if (clip.player.isPlaying()) clip.player.stop()
        }
        sync(MUSIC, musicEnabled)
        sync(ENGINE, sfxEnabled && engineActive)
    }

    private fun position(entity: Entity?, value: Vector3) {
        entity?.components?.get<TransformComponent>()?.setPosition(value)
    }

    private fun onMain(action: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            if (!closed) action()
        } else main.post { if (!closed) action() }
    }

    companion object {
        private const val TAG = "OrbitAudio"
        private const val MUSIC = "music"
        private const val ENGINE = "rocket_engine"
        private val EFFECTS = listOf("button_click", ENGINE, "orbit_success", "mission_failure", "collision")
        private val VOICES = listOf("select_planet", "place_rocket", "orbit_success", "mission_failure", "collision_warning", "boundary_warning")
    }
}
