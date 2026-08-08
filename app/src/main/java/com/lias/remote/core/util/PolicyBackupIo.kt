// ====================================================================
// File: app/src/main/java/com/lias/remote/core/util/PolicyBackupIo.kt
// Version: 12.0.0
//
// Purpose:
//   Safe UTF-8 file I/O for Android policy backup/restore.
//
// Design:
//   - Uses Storage Access Framework URIs supplied by the user.
//   - No filesystem/storage permission required.
//   - Bounded restore size protects the UI process from accidentally
//     reading a huge unrelated file into memory.
// ====================================================================

package com.lias.remote.core.util

import android.content.ContentResolver
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets

object PolicyBackupIo {

    const val MIME_TYPE =
        "application/json"

    const val DEFAULT_FILE_NAME =
        "lias-policies-backup.json"

    private const val MAX_IMPORT_BYTES =
        2 * 1024 * 1024

    fun write(
        resolver: ContentResolver,
        uri: Uri,
        payload: String
    ): Result<Unit> =
        runCatching {

            resolver
                .openOutputStream(
                    uri,
                    "wt"
                )
                ?.use { output ->

                    output.write(
                        payload.toByteArray(
                            StandardCharsets.UTF_8
                        )
                    )

                    output.flush()

                }
                ?: error(
                    "Unable to open the selected file."
                )
        }

    fun read(
        resolver: ContentResolver,
        uri: Uri
    ): Result<String> =
        runCatching {

            resolver
                .openInputStream(
                    uri
                )
                ?.use { input ->

                    val output =
                        ByteArrayOutputStream()

                    val buffer =
                        ByteArray(
                            8 * 1024
                        )

                    var total =
                        0

                    while (true) {

                        val count =
                            input.read(
                                buffer
                            )

                        if (
                            count < 0
                        ) {
                            break
                        }

                        total +=
                            count

                        require(
                            total <=
                                MAX_IMPORT_BYTES
                        ) {
                            "The selected policy backup is larger than 2 MB."
                        }

                        output.write(
                            buffer,
                            0,
                            count
                        )
                    }

                    output
                        .toString(
                            StandardCharsets.UTF_8
                                .name()
                        )
                        .trim()
                        .also {
                            require(
                                it.isNotBlank()
                            ) {
                                "The selected policy backup is empty."
                            }
                        }
                }
                ?: error(
                    "Unable to open the selected file."
                )
        }
}
