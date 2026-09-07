package io.github.androidpoet.jolt

/** How many quick actions both home screens actually show, static items included. */
public const val QUICK_ACTIONS_RECOMMENDED_MAX: Int = 4

/**
 * One entry in the long-press menu.
 *
 * @property id stable identifier handed back on launch. iOS `type`, Android shortcut id.
 * @property title primary label, already localized. Android launchers show about ten characters.
 * @property subtitle secondary label. iOS shows it under the title; Android appends it to the
 *   title in the long label ("Start timer · 25 minutes").
 * @property icon iOS: an SF Symbol name. Android: the key handed to
 *   `AndroidQuickActionsConfig.iconResolver`; never looked up by resource name.
 * @property data free-form payload returned in [QuickActionLaunch]. It travels through the
 *   platform (an exported Activity on Android), so treat it as untrusted input: use it to pick
 *   a route, never as a URL or command to execute as-is.
 */
public data class QuickAction(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val icon: String? = null,
    val data: Map<String, String> = emptyMap(),
)

/**
 * A tap on a quick action.
 *
 * @property action the action as published, or, for a static or pinned item the
 *   library did not publish, one synthesised from the platform record. Its id may
 *   therefore be absent from `QuickActionsManager.actions`.
 * @property createdScreen `true` when the tap created the screen (Android
 *   `onCreate`, iOS scene connect); `false` when it reached a screen that was
 *   already showing (`onNewIntent`, `performActionFor`).
 * @property atEpochMillis when the launch was dispatched.
 */
public data class QuickActionLaunch(
    val action: QuickAction,
    val createdScreen: Boolean,
    val atEpochMillis: Long,
)
