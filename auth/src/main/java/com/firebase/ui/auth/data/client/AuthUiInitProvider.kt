/*
 * Copyright 2025 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.firebase.ui.auth.data.client

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.pm.ProviderInfo
import android.database.Cursor
import android.net.Uri
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.util.Preconditions
import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class AuthUiInitProvider : ContentProvider() {

    override fun attachInfo(context: Context, info: ProviderInfo) {
        Preconditions.checkNotNull(info, "AuthUiInitProvider ProviderInfo cannot be null.")
        if ("com.firebase.ui.auth.authuiinitprovider" == info.authority) {
            throw IllegalStateException(
                "Incorrect provider authority in manifest. Most likely due to a missing " +
                        "applicationId variable in application's build.gradle."
            )
        } else {
            super.attachInfo(context, info)
        }
    }

    override fun onCreate(): Boolean {
        val context = context ?: throw IllegalStateException("Context cannot be null")
        AuthUI.setApplicationContext(context)
        return false
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? = null

    override fun getType(uri: Uri): String? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int = 0
}