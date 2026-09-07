package io.github.androidpoet.quickactions

/** Outcome of a quick-actions call. Exhaustively `when` over it; no call throws for control flow. */
public sealed class QuickActionsResult<out T> {
    /** The call succeeded with [value]. */
    public data class Success<T>(
        public val value: T,
    ) : QuickActionsResult<T>()

    /** The call failed with a typed [error]. */
    public data class Failure(
        public val error: QuickActionsException,
    ) : QuickActionsResult<Nothing>()
}

/** Returns the value, or null on failure. */
public fun <T> QuickActionsResult<T>.getOrNull(): T? = (this as? QuickActionsResult.Success)?.value

/** Returns the error, or null on success. */
public fun <T> QuickActionsResult<T>.errorOrNull(): QuickActionsException? = (this as? QuickActionsResult.Failure)?.error
