/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.peek.browser.ui

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.preference.ListPreference
import android.util.AttributeSet
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import com.peek.browser.R

class ThemedListPreference : ListPreference, AdapterView.OnItemClickListener {

    private var mClickedDialogEntryIndex = 0

    private var mDialogTitle: CharSequence? = null

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context) : super(context)

    override fun onCreateDialogView(): View {
        // inflate custom layout with custom title & listview
        val view = View.inflate(context, R.layout.view_preference_list_view, null)

        mDialogTitle = dialogTitle
        if (mDialogTitle == null) mDialogTitle = title
        (view.findViewById<View>(R.id.dialog_title) as TextView).text = mDialogTitle

        val list = view.findViewById<View>(android.R.id.list) as ListView
        // note the layout we're providing for the ListView entries
        val adapter = ArrayAdapter<CharSequence>(
                context, R.layout.view_preference_list_view_item,
                entries)

        list.adapter = adapter
        list.choiceMode = ListView.CHOICE_MODE_SINGLE
        list.setItemChecked(findIndexOfValue(value), true)
        list.onItemClickListener = this

        return view
    }

    override fun onPrepareDialogBuilder(builder: AlertDialog.Builder) {
        // adapted from ListPreference
        if (entries == null || entryValues == null) {
            // throws exception
            super.onPrepareDialogBuilder(builder)
            return
        }

        mClickedDialogEntryIndex = findIndexOfValue(value)

        // .setTitle(null) to prevent default (blue)
        // title+divider from showing up
        builder.setTitle(null)

        builder.setPositiveButton(null, null)
    }

    override fun onItemClick(parent: AdapterView<*>, view: View, position: Int, id: Long) {
        mClickedDialogEntryIndex = position
        onClick(dialog, DialogInterface.BUTTON_POSITIVE)
        dialog.dismiss()
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        // adapted from ListPreference
        super.onDialogClosed(positiveResult)

        if (positiveResult && mClickedDialogEntryIndex >= 0
                && entryValues != null) {
            val value = entryValues[mClickedDialogEntryIndex].toString()
            if (callChangeListener(value)) {
                setValue(value)
            }
        }
    }

    companion object {
        const val TAG = "ThemedListPreference"
    }
}
