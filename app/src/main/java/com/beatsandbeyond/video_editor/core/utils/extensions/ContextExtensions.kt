package com.beatsandbeyond.video_editor.core.utils.extensions

import android.content.Context
import android.content.pm.PackageManager
import android.util.TypedValue
import androidx.core.content.ContextCompat

/**
 * Extension functions on [Context] for common operations.
 */

/** Converts dp to pixels. */
fun Context.dpToPx(dp: Float): Int =
    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.displayMetrics).toInt()

/** Converts sp to pixels. */
fun Context.spToPx(sp: Float): Int =
    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, resources.displayMetrics).toInt()

/** Returns true if the given [permission] is currently granted. */
fun Context.isPermissionGranted(permission: String): Boolean =
    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

/** Returns true if ALL of the given [permissions] are currently granted. */
fun Context.arePermissionsGranted(vararg permissions: String): Boolean =
    permissions.all { isPermissionGranted(it) }
