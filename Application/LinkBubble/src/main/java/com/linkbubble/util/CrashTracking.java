/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.linkbubble.util;

import android.util.Log;

import com.linkbubble.BuildConfig;

public class CrashTracking {

    private static final String TAG = "CrashTracking";

    public static void logHandledException(Throwable throwable) {
        if (BuildConfig.DEBUG) {
            Log.w(TAG, throwable);
        }
    }

    public static void setInt(String key, int value) {
    }

    public static void setDouble(String key, double value) {
    }

    public static void setFloat(String key, float value) {
    }

    public static void setString(String key, String string) {
    }

    public static void setBool(String key, boolean value) {
    }

    public static void log(String message) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, message);
        }
    }
}
