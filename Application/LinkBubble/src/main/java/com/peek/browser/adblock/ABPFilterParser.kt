/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.peek.browser.adblock

import android.content.Context
import com.peek.browser.R
import java.nio.charset.StandardCharsets

/**
 * Created by bbondy on 2015-10-13.
 *
 * Wrapper for native library
 */
class ABPFilterParser(context: Context) {

    private val mBuffer: ByteArray?
    private val mVerNumber: String

    init {
        mVerNumber = ADBlockUtils.getDataVerNumber(context.getString(R.string.adblock_url))
        // One time load and parse of the raw EasyList text filter list.
        mBuffer = ADBlockUtils.readData(context, context.getString(R.string.adblock_localfilename),
                context.getString(R.string.adblock_url), ETAG_PREPEND, mVerNumber, false)
        if (mBuffer != null) {
            parseList(String(mBuffer, StandardCharsets.UTF_8))
        }
    }

    fun shouldBlockJava(baseHost: String, url: String, filterOption: String): Boolean {
        if (null == mBuffer) {
            return false
        }

        return shouldBlock(baseHost, url, filterOption)
    }

    external fun parseList(data: String)
    external fun shouldBlock(baseHost: String, url: String, filterOption: String): Boolean

    companion object {
        init {
            System.loadLibrary("Peek")
        }

        private const val ETAG_PREPEND = "abp"
    }
}
