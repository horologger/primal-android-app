package net.primal.core.networking.blossom

import io.github.aakira.napier.Napier
import io.ktor.client.call.body
import io.ktor.client.request.head
import io.ktor.client.request.headers
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.content.OutgoingContent
import io.ktor.http.isSuccess
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.writeFully
import kotlinx.coroutines.withContext
import net.primal.core.utils.coroutines.DispatcherProvider
import okio.BufferedSource
import okio.use

private const val DEFAULT_BUFFER_SIZE = 8 * 1024

internal class BlossomApiImpl(
    private val baseBlossomUrl: String,
    private val dispatcherProvider: DispatcherProvider,
) : BlossomApi {

    private val httpClient by lazy { createBlossomHttpClient() }

    override suspend fun headMedia(authorization: String, fileMetadata: FileMetadata) =
        performHeadRequest(
            endpoint = "media",
            authorization = authorization,
            fileMetadata = fileMetadata,
            errorPrefix = "Head Media",
        )

    override suspend fun headUpload(authorization: String, fileMetadata: FileMetadata) =
        performHeadRequest(
            endpoint = "upload",
            authorization = authorization,
            fileMetadata = fileMetadata,
            errorPrefix = "Head Upload",
        )

    override suspend fun putUpload(
        authorization: String,
        fileMetadata: FileMetadata,
        bufferedSource: BufferedSource,
        onProgress: ((Int, Int) -> Unit)?,
    ): BlobDescriptor =
        performPutUpload(
            "upload",
            authorization = authorization,
            contentType = fileMetadata.mimeType ?: "application/octet-stream",
            fileMetadata = fileMetadata,
            bufferedSource = bufferedSource,
            onProgress = onProgress,
            errorPrefix = "Put Upload",
            checkFileSize = true,
        )

    override suspend fun putMedia(
        authorization: String,
        fileMetadata: FileMetadata,
        bufferedSource: BufferedSource,
        onProgress: ((Int, Int) -> Unit)?,
    ): BlobDescriptor =
        performPutUpload(
            endpoint = "media",
            authorization = authorization,
            contentType = fileMetadata.mimeType ?: "application/octet-stream",
            fileMetadata = fileMetadata,
            bufferedSource = bufferedSource,
            onProgress = onProgress,
            errorPrefix = "Put Media",
        )

    override suspend fun putMirror(authorization: String, fileUrl: String): BlobDescriptor {
        val response = withContext(dispatcherProvider.io()) {
            httpClient.put("$baseBlossomUrl/mirror") {
                headers {
                    append(HttpHeaders.Authorization, authorization)
                }
                setBody(MirrorRequest(fileUrl))
            }
        }

        if (!response.status.isSuccess()) {
            val reason = response.headers["X-Reason"] ?: "Unknown"
            throw BlossomMirrorException(message = reason)
        }

        return response.body<BlobDescriptor>()
    }

    private suspend fun performHeadRequest(
        endpoint: String,
        authorization: String,
        fileMetadata: FileMetadata,
        errorPrefix: String,
    ) {
        val response = withContext(dispatcherProvider.io()) {
            httpClient.head("$baseBlossomUrl/$endpoint") {
                headers {
                    append(HttpHeaders.Authorization, authorization)
                    append("X-SHA-256", fileMetadata.sha256)
                    append("X-Content-Length", fileMetadata.sizeInBytes.toString())
                    append("X-Content-Type", fileMetadata.mimeType ?: "application/octet-stream")
                }
            }
        }

        if (!response.status.isSuccess()) {
            val reason = response.headers["X-Reason"] ?: "Unknown"
            throw UploadRequirementException(message = "$reason ($errorPrefix)")
        }
    }

    private suspend fun performPutUpload(
        endpoint: String,
        authorization: String,
        contentType: String,
        fileMetadata: FileMetadata,
        bufferedSource: BufferedSource,
        errorPrefix: String,
        onProgress: ((Int, Int) -> Unit)? = null,
        checkFileSize: Boolean = false
    ): BlobDescriptor {
        var uploadedBytes = 0L
        val totalBytes = fileMetadata.sizeInBytes
        val reportInterval = 256 * 1024L
        var lastReportBytes = 0L

        Napier.d { "Blossom ▶ PUT /$endpoint started — totalSize=$totalBytes" }

        val response = withContext(dispatcherProvider.io()) {
            bufferedSource.use { source ->
                httpClient.put("$baseBlossomUrl/$endpoint") {
                    headers {
                        append(HttpHeaders.Authorization, authorization)
                        append(HttpHeaders.ContentLength, totalBytes.toString())
                        append(HttpHeaders.ContentType, contentType)
                    }

                    setBody(object : OutgoingContent.WriteChannelContent() {
                        override val contentType: ContentType
                            get() = ContentType.parse(contentType)

                        override suspend fun writeTo(channel: ByteWriteChannel) {
                            val buffer = ByteArray(4 * 1024)
                            while (!source.exhausted()) {
                                val read = source.read(buffer)
                                if (read == -1) break
                                channel.writeFully(buffer, 0, read)
                                uploadedBytes += read

                                if (uploadedBytes - lastReportBytes >= reportInterval) {
                                    Napier.v { "Blossom ▶ uploaded $uploadedBytes / $totalBytes" }
                                    onProgress?.invoke(uploadedBytes.toInt(), totalBytes.toInt())
                                    lastReportBytes = uploadedBytes
                                }
                            }
                            channel.flush()
                        }
                    })
                }
            }
        }

        if (!response.status.isSuccess()) {
            val reason = response.headers["X-Reason"] ?: "Unknown"
            Napier.e { "Blossom ▶ PUT /$endpoint failed: $reason" }
            throw BlossomUploadException("$reason ($errorPrefix)")
        }

        val descriptor = response.body<BlobDescriptor>()
        if (checkFileSize && descriptor.sizeInBytes != totalBytes) {
            Napier.e { "Blossom ▶ PUT /$endpoint size mismatch: expected $totalBytes, got ${descriptor.sizeInBytes}" }
            throw BlossomUploadException("Different file size on the server.")
        }

        Napier.d { "Blossom ▶ PUT /$endpoint upload complete: ${descriptor.url}" }
        return descriptor
    }
}
