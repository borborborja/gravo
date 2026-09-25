package com.gravo.grabadora.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.gravo.grabadora.audio.BitDepth
import com.gravo.grabadora.audio.MicSelector
import com.gravo.grabadora.audio.RecordFormat
import com.gravo.grabadora.audio.RecordingSpec
import com.gravo.grabadora.transcription.TranscriptionProviderId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class SyncProtocol(val label: String) { WEBDAV("WebDAV"), FTP("FTP"), SFTP("SFTP") }

data class AppSettings(
    val format: RecordFormat = RecordFormat.WAV,
    val depth: BitDepth = BitDepth.B24,
    val gainSlider: Int = 50,
    val micId: Int = MicSelector.DEFAULT_ID,
    val stereo: Boolean = true,
    val darkTheme: Boolean = true,
    val language: String = "es",
    val hideNotification: Boolean = false,
    val autoStartRecording: Boolean = false,
    val syncProtocol: SyncProtocol = SyncProtocol.WEBDAV,
    val syncServer: String = "",
    val syncUser: String = "",
    val syncFolder: String = "Grabadora",
    val autoUpload: Boolean = false,
    val transcriptionProvider: TranscriptionProviderId = TranscriptionProviderId.WHISPER,
    val trWhisperEndpoint: String = TranscriptionProviderId.WHISPER.defaultEndpoint,
    val trWhisperModel: String = TranscriptionProviderId.WHISPER.defaultModel,
    val trGeminiEndpoint: String = TranscriptionProviderId.GEMINI.defaultEndpoint,
    val trGeminiModel: String = TranscriptionProviderId.GEMINI.defaultModel,
    val trDeepgramEndpoint: String = TranscriptionProviderId.DEEPGRAM.defaultEndpoint,
    val trDeepgramModel: String = TranscriptionProviderId.DEEPGRAM.defaultModel,
    val transcribeLanguage: String = "",
    val autoTranscribe: Boolean = false,
) {
    val gainDb: Float get() = RecordingSpec.sliderToDb(gainSlider)

    fun toSpec(): RecordingSpec = RecordingSpec(
        format = format,
        channels = if (stereo) 2 else 1,
        depth = depth,
        gainDb = gainDb,
    )
}

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    private object Keys {
        val FORMAT = stringPreferencesKey("format")
        val DEPTH = intPreferencesKey("depth")
        val GAIN = intPreferencesKey("gain")
        val MIC = intPreferencesKey("mic")
        val STEREO = booleanPreferencesKey("stereo")
        val DARK = booleanPreferencesKey("dark")
        val LANG = stringPreferencesKey("lang")
        val HIDE_NOTIF = booleanPreferencesKey("hide_notif")
        val AUTOSTART = booleanPreferencesKey("autostart")
        val SYNC_PROTOCOL = stringPreferencesKey("sync_protocol")
        val SYNC_SERVER = stringPreferencesKey("sync_server")
        val SYNC_USER = stringPreferencesKey("sync_user")
        val SYNC_FOLDER = stringPreferencesKey("sync_folder")
        val AUTO_UPLOAD = booleanPreferencesKey("auto_upload")
        val SYNC_PASS_ENC = stringPreferencesKey("sync_pass_enc")
        val TR_PROVIDER = stringPreferencesKey("tr_provider")
        val TR_WHISPER_ENDPOINT = stringPreferencesKey("tr_whisper_endpoint")
        val TR_WHISPER_MODEL = stringPreferencesKey("tr_whisper_model")
        val TR_GEMINI_ENDPOINT = stringPreferencesKey("tr_gemini_endpoint")
        val TR_GEMINI_MODEL = stringPreferencesKey("tr_gemini_model")
        val TR_DEEPGRAM_ENDPOINT = stringPreferencesKey("tr_deepgram_endpoint")
        val TR_DEEPGRAM_MODEL = stringPreferencesKey("tr_deepgram_model")
        val TR_LANGUAGE = stringPreferencesKey("tr_language")
        val TR_AUTO = booleanPreferencesKey("tr_auto")
        val TR_WHISPER_KEY_ENC = stringPreferencesKey("tr_whisper_key_enc")
        val TR_GEMINI_KEY_ENC = stringPreferencesKey("tr_gemini_key_enc")
        val TR_DEEPGRAM_KEY_ENC = stringPreferencesKey("tr_deepgram_key_enc")
    }

    /** Contraseña de sync cifrada (AES-GCM, clave en Keystore); nunca se expone en AppSettings. */
    val syncPasswordEnc: Flow<String> = context.dataStore.data.map { it[Keys.SYNC_PASS_ENC] ?: "" }

    suspend fun setSyncPasswordEnc(blob: String) = context.dataStore.edit { it[Keys.SYNC_PASS_ENC] = blob }

    private fun keyEncKey(provider: TranscriptionProviderId) = when (provider) {
        TranscriptionProviderId.WHISPER -> Keys.TR_WHISPER_KEY_ENC
        TranscriptionProviderId.GEMINI -> Keys.TR_GEMINI_KEY_ENC
        TranscriptionProviderId.DEEPGRAM -> Keys.TR_DEEPGRAM_KEY_ENC
    }

    /** Clave de API del proveedor cifrada (AES-GCM, clave en Keystore); nunca se expone en claro. */
    fun transcriptionKeyEnc(provider: TranscriptionProviderId): Flow<String> =
        context.dataStore.data.map { it[keyEncKey(provider)] ?: "" }

    suspend fun setTranscriptionKeyEnc(provider: TranscriptionProviderId, blob: String) =
        context.dataStore.edit { it[keyEncKey(provider)] = blob }

    private fun endpointKey(provider: TranscriptionProviderId) = when (provider) {
        TranscriptionProviderId.WHISPER -> Keys.TR_WHISPER_ENDPOINT
        TranscriptionProviderId.GEMINI -> Keys.TR_GEMINI_ENDPOINT
        TranscriptionProviderId.DEEPGRAM -> Keys.TR_DEEPGRAM_ENDPOINT
    }

    private fun modelKey(provider: TranscriptionProviderId) = when (provider) {
        TranscriptionProviderId.WHISPER -> Keys.TR_WHISPER_MODEL
        TranscriptionProviderId.GEMINI -> Keys.TR_GEMINI_MODEL
        TranscriptionProviderId.DEEPGRAM -> Keys.TR_DEEPGRAM_MODEL
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { p ->
        AppSettings(
            format = p[Keys.FORMAT]?.let { runCatching { RecordFormat.valueOf(it) }.getOrNull() } ?: RecordFormat.WAV,
            depth = when (p[Keys.DEPTH]) {
                16 -> BitDepth.B16
                32 -> BitDepth.B32
                else -> BitDepth.B24
            },
            gainSlider = p[Keys.GAIN] ?: 50,
            micId = p[Keys.MIC] ?: MicSelector.DEFAULT_ID,
            stereo = p[Keys.STEREO] ?: true,
            darkTheme = p[Keys.DARK] ?: true,
            language = p[Keys.LANG] ?: "es",
            hideNotification = p[Keys.HIDE_NOTIF] ?: false,
            autoStartRecording = p[Keys.AUTOSTART] ?: false,
            syncProtocol = p[Keys.SYNC_PROTOCOL]?.let { runCatching { SyncProtocol.valueOf(it) }.getOrNull() }
                ?: SyncProtocol.WEBDAV,
            syncServer = p[Keys.SYNC_SERVER] ?: "",
            syncUser = p[Keys.SYNC_USER] ?: "",
            syncFolder = p[Keys.SYNC_FOLDER] ?: "Grabadora",
            autoUpload = p[Keys.AUTO_UPLOAD] ?: false,
            transcriptionProvider = p[Keys.TR_PROVIDER]
                ?.let { runCatching { TranscriptionProviderId.valueOf(it) }.getOrNull() }
                ?: TranscriptionProviderId.WHISPER,
            trWhisperEndpoint = p[Keys.TR_WHISPER_ENDPOINT] ?: TranscriptionProviderId.WHISPER.defaultEndpoint,
            trWhisperModel = p[Keys.TR_WHISPER_MODEL] ?: TranscriptionProviderId.WHISPER.defaultModel,
            trGeminiEndpoint = p[Keys.TR_GEMINI_ENDPOINT] ?: TranscriptionProviderId.GEMINI.defaultEndpoint,
            trGeminiModel = p[Keys.TR_GEMINI_MODEL] ?: TranscriptionProviderId.GEMINI.defaultModel,
            trDeepgramEndpoint = p[Keys.TR_DEEPGRAM_ENDPOINT] ?: TranscriptionProviderId.DEEPGRAM.defaultEndpoint,
            trDeepgramModel = p[Keys.TR_DEEPGRAM_MODEL] ?: TranscriptionProviderId.DEEPGRAM.defaultModel,
            transcribeLanguage = p[Keys.TR_LANGUAGE] ?: "",
            autoTranscribe = p[Keys.TR_AUTO] ?: false,
        )
    }

    suspend fun setFormat(v: RecordFormat) = context.dataStore.edit { it[Keys.FORMAT] = v.name }
    suspend fun setDepth(v: BitDepth) = context.dataStore.edit { it[Keys.DEPTH] = v.bits }
    suspend fun setGainSlider(v: Int) = context.dataStore.edit { it[Keys.GAIN] = v.coerceIn(0, 100) }
    suspend fun setMicId(v: Int) = context.dataStore.edit { it[Keys.MIC] = v }
    suspend fun setStereo(v: Boolean) = context.dataStore.edit { it[Keys.STEREO] = v }
    suspend fun setDarkTheme(v: Boolean) = context.dataStore.edit { it[Keys.DARK] = v }
    suspend fun setLanguage(v: String) = context.dataStore.edit { it[Keys.LANG] = v }
    suspend fun setHideNotification(v: Boolean) = context.dataStore.edit { it[Keys.HIDE_NOTIF] = v }
    suspend fun setAutoStart(v: Boolean) = context.dataStore.edit { it[Keys.AUTOSTART] = v }
    suspend fun setSyncProtocol(v: SyncProtocol) = context.dataStore.edit { it[Keys.SYNC_PROTOCOL] = v.name }
    suspend fun setSyncServer(v: String) = context.dataStore.edit { it[Keys.SYNC_SERVER] = v.trim() }
    suspend fun setSyncUser(v: String) = context.dataStore.edit { it[Keys.SYNC_USER] = v.trim() }
    suspend fun setSyncFolder(v: String) = context.dataStore.edit { it[Keys.SYNC_FOLDER] = v.trim() }
    suspend fun setAutoUpload(v: Boolean) = context.dataStore.edit { it[Keys.AUTO_UPLOAD] = v }
    suspend fun setTranscriptionProvider(v: TranscriptionProviderId) = context.dataStore.edit { it[Keys.TR_PROVIDER] = v.name }
    suspend fun setTranscriptionEndpoint(provider: TranscriptionProviderId, v: String) =
        context.dataStore.edit { it[endpointKey(provider)] = v.trim() }
    suspend fun setTranscriptionModel(provider: TranscriptionProviderId, v: String) =
        context.dataStore.edit { it[modelKey(provider)] = v.trim() }
    suspend fun setTranscribeLanguage(v: String) = context.dataStore.edit { it[Keys.TR_LANGUAGE] = v }
    suspend fun setAutoTranscribe(v: Boolean) = context.dataStore.edit { it[Keys.TR_AUTO] = v }
}
