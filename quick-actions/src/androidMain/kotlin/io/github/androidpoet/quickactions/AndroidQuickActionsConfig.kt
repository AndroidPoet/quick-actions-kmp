package io.github.androidpoet.quickactions

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.annotation.DrawableRes
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.util.Consumer
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import io.github.androidpoet.quickactions.internal.LaunchDispatcher
import io.github.androidpoet.quickactions.internal.LaunchIntentPolicy
import java.util.Collections
import java.util.WeakHashMap

/**
 * Android presentation settings for [AndroidQuickActionsManager].
 *
 * @property defaultIconRes icon for actions without one; `0` uses the application icon.
 * @property iconResolver maps [QuickAction.icon] to a drawable resource at call
 *   time. Resource names are never looked up reflectively, so release builds with
 *   resource shrinking keep working. `null` or a `0` result falls back to [defaultIconRes].
 * @property targetActivity the Activity every shortcut opens; `null` resolves the
 *   package's launcher Activity.
 * @property intentFlags flags added to every shortcut intent. The default reuses a
 *   running instance of the target (delivered through `onNewIntent`) and creates one otherwise.
 * @property intentBuilder full control over the intent; the library still sets
 *   [QuickActions.ACTION], adds [intentFlags] and attaches the encoded action.
 */
public class AndroidQuickActionsConfig(
    @param:DrawableRes public val defaultIconRes: Int = 0,
    public val iconResolver: ((String) -> Int)? = null,
    public val targetActivity: ComponentName? = null,
    public val intentFlags: Int = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP,
    public val intentBuilder: ((Context, QuickAction) -> Intent)? = null,
)

/**
 * Process-wide defaults and the Android entry point for launches. Shortcut
 * intents carry [ACTION] and the encoded action under [EXTRA_ACTION]; the
 * Activity that receives one is wired with [attach], which
 * `rememberQuickActionsManager()` from the Compose module does for you.
 */
public object QuickActions {
    /** Intent action set on every shortcut this library publishes. Reuse it in `shortcuts.xml` for static items. */
    public const val ACTION: String = "io.github.androidpoet.quickactions.action.QUICK_ACTION"

    /** Intent extra holding the [QuickActionCodec] JSON of the tapped action. */
    public const val EXTRA_ACTION: String = "io.github.androidpoet.quickactions.extra.ACTION"

    private const val SAVED_STATE_KEY = "io.github.androidpoet.quickactions.attached"
    private const val KNOWN_SHORTCUT_FLAGS =
        ShortcutManagerCompat.FLAG_MATCH_MANIFEST or ShortcutManagerCompat.FLAG_MATCH_DYNAMIC or ShortcutManagerCompat.FLAG_MATCH_PINNED

    /** Configuration used when no explicit one is passed. Set it once at app start, before the first manager is created. */
    public var androidConfig: AndroidQuickActionsConfig = AndroidQuickActionsConfig()

    /** True once launch delivery has been wired through [attach] or one of the handle functions. */
    public var isDeliveryInstalled: Boolean = false
        private set

    internal val dispatcher = LaunchDispatcher()
    private val attached: MutableSet<Activity> = Collections.newSetFromMap(WeakHashMap())
    private var sharedManager: AndroidQuickActionsManager? = null

    /** The process-wide manager `rememberQuickActionsManager()` hands out, created on first use with [androidConfig]. */
    public fun manager(context: Context): AndroidQuickActionsManager =
        sharedManager ?: synchronized(this) {
            sharedManager ?: AndroidQuickActionsManager(context.applicationContext, androidConfig).also { sharedManager = it }
        }

    /**
     * Wires launch delivery to [activity] for its whole lifetime; calling it again for
     * the same instance does nothing. The launch intent is dispatched once per Activity
     * lifetime: a flag in the saved-state registry survives rotation and process restore,
     * and relaunches from Recents (`FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY`) are ignored.
     * Later taps arrive through `onNewIntent` until `ON_DESTROY`. Launches whose id is
     * not a shortcut the platform knows for this app are dropped, so an arbitrary
     * `am start` with a forged extra never reaches `launches`.
     */
    public fun attach(activity: ComponentActivity) {
        if (!attached.add(activity)) return
        isDeliveryInstalled = true
        val registry = activity.savedStateRegistry
        val restored = registry.consumeRestoredStateForKey(SAVED_STATE_KEY) != null
        registry.registerSavedStateProvider(SAVED_STATE_KEY) { Bundle().apply { putBoolean(SAVED_STATE_KEY, true) } }

        val verified = { intent: Intent? -> intent?.takeIf { isKnownShortcut(activity, it) } }
        consume(verified(activity.intent), restored, createdScreen = true)
        val listener = Consumer<Intent> { intent -> consume(verified(intent), restored = false, createdScreen = false) }
        activity.addOnNewIntentListener(listener)
        activity.lifecycle.addObserver(
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_DESTROY) {
                    activity.removeOnNewIntentListener(listener)
                    attached.remove(activity)
                }
            },
        )
    }

    /**
     * For Activities that are not a `ComponentActivity`: call from `onCreate`. Dispatches
     * a launch when [intent] carries an action and [savedInstanceState] is null. The id is
     * not verified against the platform; prefer [attach].
     */
    public fun handleLaunchIntent(
        intent: Intent?,
        savedInstanceState: Bundle?,
    ): QuickActionLaunch? {
        isDeliveryInstalled = true
        return consume(intent, restored = savedInstanceState != null, createdScreen = true)
    }

    /** For Activities that are not a `ComponentActivity`: call from `onNewIntent`. */
    public fun handleNewIntent(intent: Intent?): QuickActionLaunch? {
        isDeliveryInstalled = true
        return consume(intent, restored = false, createdScreen = false)
    }

    private fun consume(
        intent: Intent?,
        restored: Boolean,
        createdScreen: Boolean,
    ): QuickActionLaunch? {
        if (intent == null) return null
        val action = LaunchIntentPolicy.actionFor(intent.getStringExtra(EXTRA_ACTION), intent.flags, restored) ?: return null
        intent.removeExtra(EXTRA_ACTION)
        return dispatcher.dispatch(action, createdScreen)
    }

    private fun isKnownShortcut(
        context: Context,
        intent: Intent,
    ): Boolean {
        val id = QuickActionCodec.decodeOrNull(intent.getStringExtra(EXTRA_ACTION))?.id ?: return false
        val known = runCatching { ShortcutManagerCompat.getShortcuts(context, KNOWN_SHORTCUT_FLAGS) }.getOrDefault(emptyList())
        return known.any { it.id == id }
    }
}
