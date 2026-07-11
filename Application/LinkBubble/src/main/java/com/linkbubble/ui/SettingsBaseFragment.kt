/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.linkbubble.ui

import android.app.ActivityManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.preference.Preference
import android.preference.PreferenceFragment

open class SettingsBaseFragment : PreferenceFragment() {

    fun setPreferenceIcon(preference: Preference, iconResId: Int) {
        setPreferenceIcon(preference, resources.getDrawable(iconResId))
    }

    /*
     * Ensure icons display at the correct size for the device resolution. Prevents icons with
     * non-standard sizes from causing text to be justified at wrong position.
     * This was an issue with "Share picker" (too small) and preference_theme_* (too large) on Nexus S
     */
    fun setPreferenceIcon(preference: Preference, drawableIn: Drawable) {
        var drawable = drawableIn
        if (drawable is BitmapDrawable) {
            //getResources().getDrawableForDensity()
            //getResources().getDrawableForDensity()
            val bitmap = drawable.bitmap
            val activityManager = activity!!.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val iconSize = (activityManager.launcherLargeIconSize * .67f).toInt()
            //int iconSize = getResources().getDimensionPixelSize(R.dimen.settings_icon_size);
            val w = bitmap.width
            val h = bitmap.height
            val largest = Math.max(w, h)
            if (largest > 0) {
                if (largest > iconSize) {
                    val b = Bitmap.createScaledBitmap(bitmap, iconSize, iconSize, true)
                    drawable = BitmapDrawable(resources, b)
                } else if (largest < iconSize) {
                    val b = Bitmap.createScaledBitmap(bitmap, iconSize, iconSize, true)
                    drawable = BitmapDrawable(resources, b)
                }
            }
        }

        preference.icon = drawable
    }
}
