package io.github.androidpoet.quickactions

/**
 * Typed failure carried by [QuickActionsResult.Failure]. Every subtype has a
 * stable numeric [code] for logging and analytics.
 */
public sealed class QuickActionsException(
    public val code: Int,
    override val message: String,
    override val cause: Throwable?,
) : Exception(message, cause) {
    /** This platform cannot show quick actions. */
    public class Unsupported(
        message: String = "Quick actions are not supported on this platform",
        cause: Throwable? = null,
    ) : QuickActionsException(CODE_UNSUPPORTED, message, cause)

    /** An action failed validation, or a list held duplicate ids. */
    public class InvalidAction(
        message: String,
        cause: Throwable? = null,
    ) : QuickActionsException(CODE_INVALID_ACTION, message, cause)

    /** More actions were requested than the platform accepts right now. */
    public class TooManyActions(
        public val requested: Int,
        public val max: Int,
        cause: Throwable? = null,
    ) : QuickActionsException(CODE_TOO_MANY, "Requested $requested quick actions; the platform accepts $max", cause)

    /** Android rate-limits shortcut changes while the app is in the background. Retry in the foreground. */
    public class RateLimited(
        cause: Throwable? = null,
    ) : QuickActionsException(CODE_RATE_LIMITED, "The platform is rate-limiting quick action changes", cause)

    /** An encoded action exceeds [QUICK_ACTION_MAX_PAYLOAD_BYTES]. */
    public class PayloadTooLarge(
        message: String,
        cause: Throwable? = null,
    ) : QuickActionsException(CODE_PAYLOAD_TOO_LARGE, message, cause)

    /** The platform reported an error not covered by another subtype. */
    public class PlatformError(
        message: String,
        cause: Throwable? = null,
    ) : QuickActionsException(CODE_PLATFORM, message, cause)

    private companion object {
        const val CODE_UNSUPPORTED = 3001
        const val CODE_INVALID_ACTION = 3002
        const val CODE_TOO_MANY = 3003
        const val CODE_RATE_LIMITED = 3004
        const val CODE_PAYLOAD_TOO_LARGE = 3005
        const val CODE_PLATFORM = 3006
    }
}
