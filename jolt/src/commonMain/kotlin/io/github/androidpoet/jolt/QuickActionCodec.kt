package io.github.androidpoet.jolt

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * JSON wire format carried inside the platform payload: iOS `userInfo["jolt"]`
 * and the Android intent extra `Jolt.EXTRA_ACTION`. Flat keys, every field
 * optional except `id` and `title`, unknown keys ignored, so a payload written
 * by one version decodes on another.
 */
public object QuickActionCodec {
    private val json =
        Json {
            ignoreUnknownKeys = true
            explicitNulls = false
            encodeDefaults = false
        }

    /** Encodes [action] to its JSON object. */
    public fun encode(action: QuickAction): String = json.encodeToString(ActionDto.serializer(), action.toDto())

    /** Decodes a JSON object produced by [encode]; throws [QuickActionsException.InvalidAction] on malformed input. */
    public fun decode(jsonString: String): QuickAction =
        try {
            json.decodeFromString(ActionDto.serializer(), jsonString).toModel()
        } catch (e: SerializationException) {
            throw QuickActionsException.InvalidAction("Malformed quick action JSON: ${e.message}", e)
        } catch (e: IllegalArgumentException) {
            throw QuickActionsException.InvalidAction("Malformed quick action JSON: ${e.message}", e)
        }

    /** Decodes leniently: null for malformed input instead of throwing. */
    public fun decodeOrNull(jsonString: String?): QuickAction? =
        jsonString?.let { runCatching { decode(it) }.getOrNull() }
}

@Serializable
internal data class ActionDto(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val icon: String? = null,
    val data: Map<String, String> = emptyMap(),
)

internal fun QuickAction.toDto(): ActionDto = ActionDto(id, title, subtitle, icon, data)

internal fun ActionDto.toModel(): QuickAction = QuickAction(id, title, subtitle, icon, data)
