package io.github.androidpoet.jolt

/** Largest encoded action accepted; iOS `userInfo` and Android intent extras both want small payloads. */
public const val QUICK_ACTION_MAX_PAYLOAD_BYTES: Int = 1024

/** Returns the first problem with this action, including payload size, or null when it is valid. */
public fun QuickAction.validate(): QuickActionsException? {
    if (id.isBlank()) return QuickActionsException.InvalidAction("id must not be blank")
    if (title.isBlank()) return QuickActionsException.InvalidAction("title must not be blank (action $id)")
    if (data.keys.any { it.isBlank() }) return QuickActionsException.InvalidAction("data keys must not be blank (action $id)")
    val bytes = QuickActionCodec.encode(this).encodeToByteArray().size
    if (bytes > QUICK_ACTION_MAX_PAYLOAD_BYTES) {
        return QuickActionsException.PayloadTooLarge(
            "Encoded action $id is $bytes bytes; the limit is $QUICK_ACTION_MAX_PAYLOAD_BYTES",
        )
    }
    return null
}

/**
 * Returns the first problem with this list: a per-action failure, then duplicate
 * ids, then more than [max] entries. Null when the list is valid.
 */
public fun List<QuickAction>.validate(max: Int): QuickActionsException? {
    forEach { action -> action.validate()?.let { return it } }
    val duplicate = groupingBy { it.id }.eachCount().entries.firstOrNull { it.value > 1 }
    if (duplicate != null) return QuickActionsException.InvalidAction("duplicate quick action id ${duplicate.key}")
    if (size > max) return QuickActionsException.TooManyActions(requested = size, max = max)
    return null
}
