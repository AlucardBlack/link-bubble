/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.linkbubble.ui

import android.content.ActivityNotFoundException
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import com.linkbubble.MainController
import com.linkbubble.R

class NotificationNewBraveBrowserActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val mainController = MainController.get()
        mainController?.switchToBubbleView(false)
        try {
            val gpsIntent = Intent(Intent.ACTION_VIEW)
            gpsIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            gpsIntent.data = Uri.parse("market://details?id=" + resources.getString(R.string.tab_based_browser_id_name))
            startActivity(gpsIntent)
        } catch (anfe: ActivityNotFoundException) {
        }

        finish()
    }

    companion object {
        const val NOTIFICATION_ID = 1000
    }
}
