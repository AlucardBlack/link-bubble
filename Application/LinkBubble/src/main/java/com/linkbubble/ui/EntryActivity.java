/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.linkbubble.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.linkbubble.Config;
import com.linkbubble.Constant;
import com.linkbubble.MainApplication;
import com.linkbubble.MainController;
import com.linkbubble.R;
import com.linkbubble.Settings;
import com.linkbubble.util.CrashTracking;

import java.net.MalformedURLException;
import java.net.URL;

public class EntryActivity extends Activity {

    static EntryActivity sCurrentInstance;

    private final Handler mHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        sCurrentInstance = this;

        Intent intent = getIntent();
        boolean isActionView = false;
        boolean isActionSend = false;
        if (intent != null && intent.getAction() != null) {
            isActionView = intent.getAction().equals(Intent.ACTION_VIEW);
            isActionSend = intent.getAction().equals(Intent.ACTION_SEND);
        }

        super.onCreate(savedInstanceState);

        PreferenceManager.setDefaultValues(this, R.xml.preferences, true);

        if (isActionView || isActionSend) {
            boolean openLink = false;

            String url = intent.getDataString();

            if (isActionSend) {
                String type = intent.getType();
                Bundle extras = intent.getExtras();
                if (type != null && type.equals("text/plain") && extras.containsKey(Intent.EXTRA_TEXT)) {
                    String text = extras.getString(Intent.EXTRA_TEXT);
                    String[] splitText = text.split(" ");
                    for (String s : splitText) {
                        try {
                            URL _url = new URL(s);
                            url = _url.toString();
                            openLink = true;
                            break;
                        } catch (MalformedURLException ex) {
                        }
                    }

                    if (openLink == false) {
                        Toast.makeText(this, R.string.invalid_send_action, Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }
                }
            }

            if (null == url) {
                url = "";
            }
            // Special case code for the setting the default browser. If this URL is received, do nothing.
            if (url.equals(Config.SET_DEFAULT_BROWSER_URL)) {
                Toast.makeText(this, R.string.default_browser_set, Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            String openedFromAppName = null;
            boolean canLoadFromThisApp = true;
            if (Settings.get().isEnabled()) {
                openLink = true;
            }

            if (canLoadFromThisApp == false) {
                MainApplication.openInBrowser(this, intent, true, false);
            } else if (openLink) {
                MainApplication.checkRestoreCurrentTabs(this);

                boolean showedWelcomeUrl = false;
                if (Settings.get().getWelcomeMessageDisplayed() == false) {
                    if (!(MainController.get() != null && MainController.get().isUrlActive(Constant.WELCOME_MESSAGE_URL))) {
                        MainApplication.openLink(this, Constant.WELCOME_MESSAGE_URL, null);
                        showedWelcomeUrl = true;
                    }
                }

                MainApplication.openLink(this, url, true, openedFromAppName);
            } else {
                MainApplication.openInBrowser(this, intent, true, false);
            }
        } else {
            startActivityForResult(new Intent(this, HomeActivity.class), 0);
        }

        finish();
    }

    @Override
    protected void onDestroy() {
        if (sCurrentInstance == this) {
            sCurrentInstance = null;
        }

        super.onDestroy();
    }

    @Override
    protected void onStart() {
        super.onStart();

        sCurrentInstance = this;
    }

    @Override
    protected void onStop() {
        super.onStop();

        delayedFinishIfCurrent();
    }

    @Override
    public void onBackPressed() {
        delayedFinishIfCurrent();
    }

    void delayedFinishIfCurrent() {
        // Kill the activity to ensure it is not alive in the event a link is intercepted,
        // thus displaying the ugly UI for a few frames

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (sCurrentInstance == EntryActivity.this) {
                    finish();
                }
            }
        }, 500);
    }
}
