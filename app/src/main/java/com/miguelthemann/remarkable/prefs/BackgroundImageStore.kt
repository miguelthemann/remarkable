/*
 * Copyright (c) 2026 Miguel Guerra
 * SPDX-License-Identifier: MIT
 */
package com.miguelthemann.remarkable.prefs

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Copies a user-picked image into app-private storage so it survives URI permission expiry. */
object BackgroundImageStore {
    private const val FILE_NAME = "custom_background.jpg"

    fun file(context: Context): File = File(context.filesDir, FILE_NAME)

    fun pathOrNull(context: Context): String? {
        val f = file(context)
        return f.takeIf { it.exists() && it.length() > 0L }?.absolutePath
    }

    suspend fun importFromUri(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
        val dest = file(context)
        context.contentResolver.openInputStream(uri)?.use { input ->
            dest.outputStream().use { output -> input.copyTo(output) }
        } ?: error("Could not read image")
        check(dest.length() > 0L) { "Empty image" }
        dest.absolutePath
    }

    suspend fun clear(context: Context) = withContext(Dispatchers.IO) {
        file(context).delete()
        Unit
    }
}
