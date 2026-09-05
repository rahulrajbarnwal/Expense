package com.icit.expense.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.icit.expense.MainActivity

/**
 * Single place that turns an incoming [Intent] into a [NavDestination].
 *
 * Three shapes arrive here, all handled the same way:
 *  - app icon long-press shortcuts (res/xml/shortcuts.xml) → `route` string extra
 *  - custom scheme deep links → `expense://feedback`
 *  - web deep links → `https://<WEB_HOST>/feedback`
 *
 * A plain app icon tap carries none of these, so [resolve] returns null and the app opens at home.
 */
object AppDeepLinks {

    const val SCHEME = "expense"

    /** TODO: point this at the real marketing domain and host assetlinks.json there before enabling autoVerify. */
    const val WEB_HOST = "expense.icit.app"

    const val EXTRA_ROUTE = "route"

    const val ROUTE_FEEDBACK = "feedback"
    const val ROUTE_ADD_EXPENSE = "add_expense"
    const val ROUTE_ANALYTICS = "analytics"
    const val ROUTE_HISTORY = "history"

    fun resolve(intent: Intent?): NavDestination? {
        if (intent == null) return null

        // Shortcuts and notifications pass the route explicitly.
        destinationFor(intent.getStringExtra(EXTRA_ROUTE))?.let { return it }

        val data = intent.data ?: return null
        val route = if (SCHEME.equals(data.scheme, ignoreCase = true)) {
            // expense://feedback — the route is the host.
            data.host
        } else {
            // https://host/feedback — the route is the first path segment.
            data.pathSegments.firstOrNull()
        }
        return destinationFor(route)
    }

    private fun destinationFor(route: String?): NavDestination? = when (route?.lowercase()) {
        ROUTE_FEEDBACK, "feedback_screen" -> NavDestination.Feedback
        ROUTE_ADD_EXPENSE, "add" -> NavDestination.AddExpense()
        ROUTE_ANALYTICS -> NavDestination.Analytics
        ROUTE_HISTORY -> NavDestination.History
        else -> null
    }

    /** Deep link URI for sharing, e.g. `expense://feedback`. */
    fun uriFor(route: String): Uri = Uri.parse("$SCHEME://$route")

    /**
     * Intent that opens [route] directly — use this from notifications and widgets.
     * FLAG_ACTIVITY_CLEAR_TOP reuses the running task instead of stacking a second copy of the app.
     */
    fun intentFor(context: Context, route: String): Intent =
        Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            putExtra(EXTRA_ROUTE, route)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
}
