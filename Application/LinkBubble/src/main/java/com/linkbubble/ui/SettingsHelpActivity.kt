/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.linkbubble.ui

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.preference.Preference
import android.preference.PreferenceFragment
import androidx.appcompat.widget.Toolbar
import android.view.MenuItem
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import com.linkbubble.Constant
import com.linkbubble.MainApplication
import com.linkbubble.R
import com.linkbubble.util.Analytics
import com.linkbubble.util.Util

/*
 * This class exists solely because Android's PreferenceScreen implementation doesn't do anything
 * when the Up button is touched, and we need to go back in that case given our use of the Up button.
 */
class SettingsHelpActivity : AppCompatPreferenceActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_settings_help)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        toolbar.setTitle(R.string.preference_help_title)
        setSupportActionBar(toolbar)
        Util.padForStatusBarInset(toolbar)
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp)
        toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.activity_close_enter, R.anim.activity_close_exit)
    }

    class SettingsHelpFragment : PreferenceFragment() {

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            addPreferencesFromResource(R.xml.preferences_help)

            findPreference("preference_credits").setOnPreferenceClickListener {
                showCreditsDialog()
                true
            }

            findPreference("preference_osl").setOnPreferenceClickListener {
                showOpenSourceLicensesDialog()
                true
            }

            findPreference("preference_show_welcome_message").setOnPreferenceClickListener {
                MainApplication.openLink(activity, Constant.WELCOME_MESSAGE_URL, Analytics.OPENED_URL_FROM_SETTINGS)
                true
            }

            findPreference("preference_privacy_policy").setOnPreferenceClickListener {
                MainApplication.openLink(activity, Constant.PRIVACY_POLICY_URL, Analytics.OPENED_URL_FROM_SETTINGS)
                true
            }

            findPreference("preference_terms_of_service").setOnPreferenceClickListener {
                MainApplication.openLink(activity, Constant.TERMS_OF_SERVICE_URL, Analytics.OPENED_URL_FROM_SETTINGS)
                true
            }
        }

        private var mForceCrashCountdown = TAPS_TO_FORCE_A_CRASH
        var mForceCrashToast: Toast? = null

        fun showCreditsDialog() {
            val layout = View.inflate(activity, R.layout.view_credits, null)

            val builder = AlertDialog.Builder(activity)
            builder.setNegativeButton(android.R.string.ok, null)
            builder.setView(layout)
            builder.setTitle(R.string.credits_title)

            val alertDialog = builder.create()
            alertDialog.setIcon(Util.getAlertIcon(activity))
            Util.showThemedDialog(alertDialog)
        }

        private fun showOpenSourceLicensesDialog() {
            val webView = WebView(activity)
            webView.loadUrl("file:///android_asset/open_source_licenses.html")
            webView.webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                    val i = Intent(Intent.ACTION_VIEW)
                    i.data = Uri.parse(url)
                    startActivity(i)
                    return true
                }
            }

            val builder = AlertDialog.Builder(activity)
            builder.setIcon(Util.getAlertIcon(activity))
            builder.setNegativeButton(R.string.action_ok, null)
            builder.setView(webView)
            builder.setTitle(R.string.preference_osl_title)

            val alertDialog = builder.create()
            Util.showThemedDialog(alertDialog)
        }

        companion object {
            private const val TAPS_TO_FORCE_A_CRASH = 7
        }
    }
}
