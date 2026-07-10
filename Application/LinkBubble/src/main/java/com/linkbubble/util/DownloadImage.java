/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.linkbubble.util;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.webkit.URLUtil;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.linkbubble.BuildConfig;
import com.linkbubble.MainController;
import com.linkbubble.R;
import com.linkbubble.ui.Prompt;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DownloadImage {

    private static final String TAG = "DownloadImage";

    private Context mContext;
    private String mUrlAsString;

    public DownloadImage(Context context, String urlAsString) {
        mContext = context;
        mUrlAsString = urlAsString;
    }

    public void download() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
                && ContextCompat.checkSelfPermission(mContext, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
            showErrorPrompt();
            return;
        }

        Log.d(TAG, "downloading image: " + mUrlAsString);
        Toast.makeText(mContext, R.string.notice_download_started, Toast.LENGTH_LONG).show();
        new DownloadImageTask().execute(mUrlAsString);
    }

    private void showErrorPrompt() {
        Resources resources = mContext.getResources();
        String message = resources.getString(R.string.error_saving_image);
        Prompt.show(message, mContext.getResources().getString(android.R.string.ok),
                Prompt.LENGTH_LONG, new Prompt.OnPromptEventListener() {
                    @Override
                    public void onActionClick() {
                    }

                    @Override
                    public void onClose() {
                    }
                });
    }

    private void showSuccessPrompt(final Uri imageUri) {
        Resources resources = mContext.getResources();
        String message = resources.getString(R.string.image_saved);
        Prompt.show(message, mContext.getResources().getString(R.string.action_open),
                Prompt.LENGTH_LONG, new Prompt.OnPromptEventListener() {
                    @Override
                    public void onActionClick() {
                        Intent intent = new Intent();
                        intent.setAction(Intent.ACTION_VIEW);
                        intent.setDataAndType(imageUri, "image/*");
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        mContext.startActivity(intent);
                        MainController.get().switchToBubbleView(false);
                    }

                    @Override
                    public void onClose() {
                    }
                });

    }

    private class DownloadImageTask extends AsyncTask<String, Integer, Uri> {
        protected Uri doInBackground(String... urls) {
            try {
                String fileExtension;
                String name;
                String mimeType = "image/jpeg";
                String sUrl = urls[0];
                byte[] buffer;
                ByteArrayOutputStream output = new ByteArrayOutputStream();

                // If the URL is a base64 encoded URL, we need to decode it.
                if (URLUtil.isDataUrl(sUrl)) {
                    String[] parts = sUrl.split(",");
                    String encoded = parts[1];

                    // Find the extension from the data URI.
                    String extension = "";
                    if (parts[0].toLowerCase().contains("image/")) {
                        Pattern pattern = Pattern.compile("image/([a-zA-Z]*)");
                        Matcher matcher = pattern.matcher(parts[0]);
                        matcher.find();
                        extension = matcher.group(1);
                        mimeType = "image/" + extension;
                    }
                    name = "dataimage" + extension;

                    if (sUrl.toLowerCase().contains(";base64")) {
                        buffer = Base64.decode(encoded, Base64.DEFAULT);
                        output.write(buffer, 0, buffer.length);
                        output.close();
                    } else {
                        buffer = Uri.decode(encoded).getBytes();
                        output.write(buffer, 0, buffer.length);
                        output.close();
                    }

                } else {
                    // For a normal image download we just read the image from the URL.
                    fileExtension = MimeTypeMap.getFileExtensionFromUrl(sUrl);
                    name = URLUtil.guessFileName(sUrl, null, fileExtension);
                    if (fileExtension != null) {
                        String guessedMimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension);
                        if (guessedMimeType != null) {
                            mimeType = guessedMimeType;
                        }
                    }

                    URL url = new URL(sUrl);
                    InputStream is = (InputStream) url.getContent();
                    buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = is.read(buffer)) != -1) {
                        output.write(buffer, 0, bytesRead);
                    }
                    output.close();
                }

                // Prefix the filename with the date to attempt to prevent overwriting of existing files.
                name = System.currentTimeMillis() + name;

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    return saveViaMediaStore(name, mimeType, output);
                } else {
                    return saveToLegacyDownloadsDir(name, output);
                }
            } catch (IOException e) {
                CrashTracking.log(e.getMessage());
                return null;
            }
        }

        private Uri saveViaMediaStore(String name, String mimeType, ByteArrayOutputStream output) throws IOException {
            ContentResolver resolver = mContext.getContentResolver();
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, name);
            values.put(MediaStore.Images.Media.MIME_TYPE, mimeType);
            values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

            Uri collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
            Uri itemUri = resolver.insert(collection, values);
            if (itemUri == null) {
                return null;
            }

            OutputStream os = resolver.openOutputStream(itemUri);
            if (os == null) {
                return null;
            }
            os.write(output.toByteArray());
            os.flush();
            os.close();

            return itemUri;
        }

        private Uri saveToLegacyDownloadsDir(String name, ByteArrayOutputStream output) throws IOException {
            File path = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS);

            File imageFile = new File(path, name);
            FileOutputStream fos = new FileOutputStream(imageFile);
            fos.write(output.toByteArray());
            fos.flush();
            fos.close();

            MediaScannerConnection.scanFile(mContext, new String[]{imageFile.getAbsolutePath()}, null, null);

            return FileProvider.getUriForFile(mContext, BuildConfig.APPLICATION_ID + ".fileprovider", imageFile);
        }

        protected void onProgressUpdate(Integer... progress) {
        }

        protected void onPostExecute(Uri result) {
            if (result != null) {
                showSuccessPrompt(result);
            } else {
                showErrorPrompt();
            }
        }
    }

}
