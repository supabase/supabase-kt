package io.github.jan.supabase.storage

import io.github.jan.supabase.auth.api.AuthenticatedSupabaseApi
import io.github.jan.supabase.buildUrl
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.putJsonObject
import io.github.jan.supabase.safeBody
import io.github.jan.supabase.storage.BucketApi.Companion.UPSERT_HEADER
import io.github.jan.supabase.storage.resumable.ResumableCache
import io.github.jan.supabase.storage.resumable.ResumableClientImpl
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.content.OutgoingContent
import io.ktor.http.defaultForFilePath
import io.ktor.http.formUrlEncode
import io.ktor.http.headers
import io.ktor.http.isSuccess
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.copyTo
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.time.Duration

internal class BucketApiImpl(
    override val bucketId: String,
    val storage: Storage,
    api: AuthenticatedSupabaseApi,
    resumableCache: ResumableCache
) : BucketApi {

    private var headers = Headers.Empty
    private val api = api.withDefaultRequest {
        headers.appendAll(this@BucketApiImpl.headers)
    }

    override val supabaseClient = storage.supabaseClient

    override val resumable = ResumableClientImpl(this, resumableCache)

    override suspend fun purgeCache(path: String, options: PurgeCacheOptions.() -> Unit) {
        val options = PurgeCacheOptions().apply(options)
        storage.api.delete("cdn/${storagePath(bucketId, path)}") {
            if(options.transformations) {
                parameter("transformations", true)
            }
        }
    }

    override fun setHeader(name: String, value: String): BucketApi {
        headers = headers {
            appendAll(headers)
            set(name, value)
        }
        return this
    }

    override suspend fun update(
        path: String,
        data: UploadData,
        options: UploadOptionBuilder.() -> Unit
    ): FileUploadResponse =
        uploadOrUpdate(
            HttpMethod.Put, defaultUploadUrl(path), path, data, options
        )

    override suspend fun uploadToSignedUrl(
        path: String,
        token: String,
        data: UploadData,
        options: UploadOptionBuilder.() -> Unit
    ): FileUploadResponse {
        return uploadOrUpdate(
            HttpMethod.Put, uploadToSignedUrlUrl(path, token), path, data, options
        )
    }

    override suspend fun createSignedUploadUrl(path: String, upsert: Boolean): UploadSignedUrl {
        val result = api.post("upload/sign/${storagePath(bucketId, path)}") {
            header(UPSERT_HEADER, upsert.toString())
        }
        val urlPath = result.safeBody<JsonObject>()["url"]?.jsonPrimitive?.content?.substring(1)
            ?: error("Expected a url in create upload signed url response")
        val url = Url(storage.resolveUrl(urlPath))
        return UploadSignedUrl(
            url = url.toString(),
            path = path,
            token = url.parameters["token"]
                ?: error("Expected a token in create upload signed url response")
        )
    }

    override suspend fun upload(
        path: String,
        data: UploadData,
        options: UploadOptionBuilder.() -> Unit
    ): FileUploadResponse =
        uploadOrUpdate(
            HttpMethod.Post, defaultUploadUrl(path), path, data, options
        )

    override suspend fun delete(paths: Collection<String>) {
        api.deleteJson(storagePath(bucketId), buildJsonObject {
            putJsonArray("prefixes") {
                paths.forEach(this::add)
            }
        })
    }

    override suspend fun move(from: String, to: String, destinationBucket: String?) {
        api.postJson("move", buildJsonObject {
            put("bucketId", bucketId)
            put("sourceKey", from)
            put("destinationKey", to)
            destinationBucket?.let { put("destinationBucket", it) }
        })
    }

    override suspend fun copy(from: String, to: String, destinationBucket: String?) {
        api.postJson("copy", buildJsonObject {
            put("bucketId", bucketId)
            put("sourceKey", from)
            put("destinationKey", to)
            destinationBucket?.let { put("destinationBucket", it) }
        })
    }

    override suspend fun createSignedUrl(
        path: String,
        expiresIn: Duration,
        builder: SignedUrlBuilder.() -> Unit
    ): String {
        val modifier = SignedUrlBuilder().apply(builder)
        val transformation = modifier.transformation
        val body = api.postJson("sign/${storagePath(bucketId, path)}", buildJsonObject {
            put("expiresIn", expiresIn.inWholeSeconds)
            transformation?.let {
                val transform = buildJsonObject {
                    putImageTransformation(transformation)
                }
                if(transform.isNotEmpty()) {
                    put("transform", transform)
                }
            }
        }).safeBody<JsonObject>()
        val signedUrl = body["signedURL"]?.jsonPrimitive?.content ?: error("Expected signed url in response")
        return resolveSignedUrl(signedUrl, modifier)
    }

    override suspend fun createSignedUrls(
        expiresIn: Duration,
        paths: Collection<String>,
        builder: SignedUrlsBuilder.() -> Unit
    ): List<SignedUrl> {
        val modifier = SignedUrlsBuilder().apply(builder)
        val body = api.postJson("sign/${storagePath(bucketId)}", buildJsonObject {
            putJsonArray("paths") {
                paths.forEach(this::add)
            }
            put("expiresIn", expiresIn.inWholeSeconds)
        }).safeBody<List<SignedUrl>>().map {
            it.copy(signedURL = resolveSignedUrl(it.signedURL, modifier))
        }
        return body
    }

    private fun resolveSignedUrl(signedUrl: String, modifier: BucketUrlBuilder.WithForceDownload): String =
        buildUrl(storage.resolveUrl(signedUrl.substring(1))) {
            modifier.cacheNonce?.let { parameters.append("cacheNonce", it) }
            modifier.download?.let { parameters.append("download", it.fileName ?: "") }
        }

    override suspend fun downloadAuthenticated(
        path: String,
        options: DownloadOptionBuilder.() -> Unit
    ): ByteArray = normalDownloadRequest(path, false, options)


    override suspend fun downloadPublic(
        path: String,
        options: DownloadOptionBuilder.() -> Unit
    ): ByteArray = normalDownloadRequest(path, true, options)

    private suspend fun normalDownloadRequest(
        path: String,
        public: Boolean,
        options: DownloadOptionBuilder.() -> Unit
    ): ByteArray {
        val downloadOptions = DownloadOptionBuilder().apply(options)
        return api.rawRequest {
            prepareDownloadRequest(path, public, downloadOptions)
            downloadOptions.httpRequestOverrides.forEach { it() }
        }.body()
    }

    override suspend fun downloadAuthenticated(
        path: String,
        channel: ByteWriteChannel,
        options: DownloadOptionBuilder.() -> Unit
    ) {
        channelDownloadRequest(path, channel, false, options)
    }


    override suspend fun downloadPublic(
        path: String,
        channel: ByteWriteChannel,
        options: DownloadOptionBuilder.() -> Unit
    ) {
        channelDownloadRequest(path, channel, true, options)
    }

    internal suspend fun channelDownloadRequest(
        path: String,
        channel: ByteWriteChannel,
        public: Boolean,
        options: DownloadOptionBuilder.() -> Unit,
    ) {
        val downloadOptions = DownloadOptionBuilder().apply(options)
        api.prepareRequest {
            prepareDownloadRequest(path, public, downloadOptions)
            downloadOptions.httpRequestOverrides.forEach { it() }
        }.execute {
            if (!it.status.isSuccess()) throw storage.parseErrorResponse(it)
            it.bodyAsChannel().copyTo(channel)
        }
        channel.flushAndClose()
    }

    internal fun HttpRequestBuilder.prepareDownloadRequest(
        path: String,
        public: Boolean,
        options: DownloadOptionBuilder
    ) {
        val transformation = ImageTransformation().apply(options.transform).query()
        val url = when (public) {
            true -> publicUrl(
                path
            ) {
               if(!transformation.isEmpty()) transform(options.transform)
               cacheNonce = options.cacheNonce
            }
            false -> buildUrl(
                if (transformation.isEmpty()) authenticatedUrl(path) else authenticatedRenderUrl(path, options.transform)
            ) {
                options.cacheNonce?.let { parameters.append("cacheNonce", it) }
            }
        }
        method = HttpMethod.Get
        url(url)
    }

    override suspend fun list(
        prefix: String,
        filter: StorageListFilter.Files.() -> Unit
    ): List<FileObject> {
        return api.postJson("list/${storagePath(bucketId)}", buildJsonObject {
            put("prefix", prefix)
            putJsonObject(StorageListFilter.Files().apply(filter).buildBody())
        }).safeBody()
    }

    override suspend fun info(path: String): FileObjectV2 {
        val response = api.get("info/${storagePath(bucketId, path)}")
        return response.safeBody<FileObjectV2>().copy(serializer = storage.serializer)
    }

    override suspend fun exists(path: String): Boolean {
        try {
            api.request(storagePath(bucketId, path)) {
                method = HttpMethod.Head
            }
            return true
        } catch (e: RestException) {
            if (e.statusCode in listOf(HttpStatusCode.NotFound.value, HttpStatusCode.BadRequest.value)) return false
            throw e
        }
    }

    private fun defaultUploadUrl(path: String) = storagePath(bucketId, path)

    private fun uploadToSignedUrlUrl(path: String, token: String) = "upload/sign/${storagePath(bucketId, path)}?token=$token"

    internal suspend fun uploadOrUpdate(
        method: HttpMethod,
        url: String,
        path: String,
        data: UploadData,
        options: UploadOptionBuilder.() -> Unit,
    ): FileUploadResponse {
        val optionBuilder = UploadOptionBuilder(storage.serializer).apply(options)
        val response = api.request(url) {
            this.method = method
            defaultUploadRequest(path, data, optionBuilder)
            optionBuilder.httpRequestOverrides.forEach { it() }
        }.safeBody<JsonObject>()
        val key = response["Key"]?.jsonPrimitive?.content
            ?: error("Expected a key in a upload response")
        val id = response["Id"]?.jsonPrimitive?.content
        return FileUploadResponse(id, path, key)
    }

    @OptIn(ExperimentalEncodingApi::class)
    private fun HttpRequestBuilder.defaultUploadRequest(
        path: String,
        data: UploadData,
        optionBuilder: UploadOptionBuilder,
    ) {
        setBody(object : OutgoingContent.ReadChannelContent() {
            override val contentType: ContentType = optionBuilder.contentType ?: ContentType.defaultForFilePath(path)
            override val contentLength: Long = data.size
            override fun readFrom(): ByteReadChannel = data.stream
        })
        header(HttpHeaders.ContentType, optionBuilder.contentType ?: ContentType.defaultForFilePath(path))
        header(UPSERT_HEADER, optionBuilder.upsert.toString())
        optionBuilder.userMetadata?.let {
            header(BucketApi.METADATA_HEADER, Base64.encode(it.toString().encodeToByteArray()))
        }
    }

    override suspend fun changePublicStatusTo(public: Boolean) = storage.updateBucket(bucketId) {
        this@updateBucket.public = public
    }

    override fun authenticatedUrl(path: String): String =
        storage.resolveUrl("object/authenticated/${storagePath(bucketId, path)}")

    override fun publicUrl(path: String, builder: PublicUrlBuilder.() -> Unit): String {
        val modifier = PublicUrlBuilder().apply(builder)
        return buildUrl(storage.resolveUrl(
            if(modifier.transformation != null) "render/image/public/${storagePath(bucketId, path)}" else "object/public/${storagePath(bucketId, path)}"
        )) {
            modifier.transformation?.let { parameters.appendAll(it.query()) }
            modifier.cacheNonce?.let { parameters.append("cacheNonce", it) }
            modifier.download?.let { parameters.append("download", it.fileName ?: "") }
        }
    }

    override fun authenticatedRenderUrl(
        path: String,
        transform: ImageTransformation.() -> Unit
    ): String {
        val transformation = ImageTransformation().apply(transform).query().formUrlEncode()
        return storage.resolveUrl("render/image/authenticated/${storagePath(bucketId, path)}${if (transformation.isNotBlank()) "?$transformation" else ""}")
    }

}
