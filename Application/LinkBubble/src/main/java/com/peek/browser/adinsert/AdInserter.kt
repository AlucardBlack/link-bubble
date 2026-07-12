/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.peek.browser.adinsert

import android.content.Context
import com.peek.browser.adblock.ADBlockUtils
import java.io.IOException

/**
 * Ad insertion list worker
 */
class AdInserter(context: Context) {

    private val mHosts: HashMap<String, String>

    init {
        mHosts = HashMap()
        val datObject = loadData(context)
        parseDatObject(datObject)
    }

    fun getHostObjects(host: String): String {
        var result = mHosts[host]
        if (null == result) {
            result = ""
        }

        return result
    }

    private fun loadData(context: Context): String {
        val assetManager = context.resources.assets
        var buffer: ByteArray? = null

        try {
            val inputStream = assetManager.open(DATA_FILE_NAME)
            val size = inputStream.available()
            buffer = ByteArray(size)
            var n: Int
            var bytesOffset = 0
            val tempBuffer = ByteArray(ADBlockUtils.BUFFER_TO_READ)
            while (inputStream.read(tempBuffer).also { n = it } != -1) {
                System.arraycopy(tempBuffer, 0, buffer, bytesOffset, n)
                bytesOffset += n
            }
            inputStream.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        if (null == buffer) {
            return ""
        }

        return String(buffer)
    }

    // We could use Json object here in future maybe, but unfortunately
    // JSONObject isn't working properly for me maybe because of the structure's size.
    private fun parseDatObject(datObject: String) {
        var currentIndex = 0
        while (datObject.length > currentIndex) {
            if ('\"' != datObject[currentIndex]) {
                break
            }
            val index = datObject.indexOf('\"', currentIndex + 1)
            if (-1 == index) {
                break
            }
            val mapKey = datObject.substring(currentIndex + 1, index)
            if (datObject.length < index + 2) {
                break
            }
            val endIndex = datObject.indexOf("}]", index + 2)
            if (-1 == endIndex) {
                break
            }
            val mapValue = datObject.substring(index + 2, endIndex + 2)
            mHosts[mapKey] = mapValue
            currentIndex = endIndex + 3
        }
    }

    companion object {
        private const val DATA_FILE_NAME = "data/adInfo.dat"
    }
}
