/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.peek.browser.ui

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ComponentName
import android.content.DialogInterface
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.preference.ListPreference
import android.preference.Preference
import android.preference.PreferenceCategory
import androidx.appcompat.widget.Toolbar
import android.view.MenuItem
import com.peek.browser.R
import com.peek.browser.Settings
import com.peek.browser.util.ActionItem
import com.peek.browser.util.Util

/*
 * This class exists solely because Android's PreferenceScreen implementation doesn't do anything
 * when the Up button is touched, and we need to go back in that case given our use of the Up button.
 */
class SettingsDefaultAppsActivity : AppCompatPreferenceActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_settings_default_apps)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        toolbar.setTitle(R.string.preference_default_apps_title)
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

    class SettingsDefaultAppsFragment : SettingsBaseFragment(), SharedPreferences.OnSharedPreferenceChangeListener {

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            addPreferencesFromResource(R.xml.preferences_default_apps)

            val defaultBrowserPreference = findPreference(Settings.PREFERENCE_DEFAULT_BROWSER)
            defaultBrowserPreference.summary = Settings.get().getDefaultBrowserLabel()
            val defaultBrowserIcon = Settings.get().getDefaultBrowserIcon(activity)
            if (defaultBrowserIcon != null) {
                setPreferenceIcon(defaultBrowserPreference, defaultBrowserIcon)
            }
            defaultBrowserPreference.setOnPreferenceClickListener {
                val alertDialog = ActionItem.getDefaultBrowserAlert(activity, object : ActionItem.OnActionItemSelectedListener {
                    override fun onSelected(actionItem: ActionItem) {
                        Settings.get().setDefaultBrowser(actionItem.getLabel(), actionItem.mPackageName)
                        defaultBrowserPreference.summary = Settings.get().getDefaultBrowserLabel()
                        val icon = Settings.get().getDefaultBrowserIcon(activity)
                        if (icon != null) {
                            setPreferenceIcon(defaultBrowserPreference, icon)
                        }
                    }
                })
                Util.showThemedDialog(alertDialog)
                true
            }

            configureDefaultAppsList()
        }

        private fun configureDefaultAppsList() {
            val preferenceCategory = findPreference("preference_category_other_apps") as PreferenceCategory
            preferenceCategory.removeAll()

            val noticePreference = Preference(activity)

            val packageManager = activity.packageManager
            val defaultAppsMap = Settings.get().getDefaultAppsMap()
            if (defaultAppsMap != null && defaultAppsMap.size > 0) {
                noticePreference.setSummary(R.string.preference_default_apps_notice_summary)
                preferenceCategory.addPreference(noticePreference)

                for (key in defaultAppsMap.keys) {
                    val componentName = defaultAppsMap[key]
                    try {
                        val info = packageManager.getActivityInfo(componentName!!, 0)
                        val label = info.loadLabel(packageManager)
                        val host = key
                        val preference = Preference(activity)
                        preference.title = label
                        setPreferenceIcon(preference, info.loadIcon(packageManager))
                        preference.summary = key
                        preference.setOnPreferenceClickListener {
                            val resources = activity.resources
                            val alertDialog = AlertDialog.Builder(activity).create()
                            alertDialog.setIcon(Util.getAlertIcon(activity))
                            alertDialog.setTitle(R.string.remove_default_title)
                            alertDialog.setMessage(String.format(resources.getString(R.string.remove_default_message), label, host, host))
                            alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, resources.getString(R.string.action_remove),
                                    DialogInterface.OnClickListener { dialog, which ->
                                        Settings.get().removeDefaultApp(host)
                                        configureDefaultAppsList()
                                    })
                            alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, resources.getString(R.string.action_cancel),
                                    DialogInterface.OnClickListener { dialog, which ->
                                    })
                            Util.showThemedDialog(alertDialog)
                            true
                        }
                        preferenceCategory.addPreference(preference)
                    } catch (e: PackageManager.NameNotFoundException) {
                        e.printStackTrace()
                    }
                }
            } else {
                noticePreference.setSummary(R.string.preference_default_apps_notice_no_defaults_summary)
                preferenceCategory.addPreference(noticePreference)
            }
        }

        override fun onResume() {
            super.onResume()

            preferenceScreen.sharedPreferences.registerOnSharedPreferenceChangeListener(this)

            configureDefaultAppsList()
        }

        override fun onPause() {
            super.onPause()

            preferenceScreen.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
        }

        override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String?) {
            val preference = findPreference(key)

            if (preference is ListPreference) {
                preference.summary = preference.entry
            }
        }
    }
}
