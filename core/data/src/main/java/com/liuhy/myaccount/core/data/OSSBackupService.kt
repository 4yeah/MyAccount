package com.liuhy.myaccount.core.data

import android.content.Context
import android.util.Log
import com.alibaba.sdk.android.oss.OSS
import com.alibaba.sdk.android.oss.OSSClient
import com.alibaba.sdk.android.oss.ServiceException
import com.alibaba.sdk.android.oss.callback.OSSCompletedCallback
import com.alibaba.sdk.android.oss.callback.OSSProgressCallback
import com.alibaba.sdk.android.oss.common.auth.OSSPlainTextAKSKCredentialProvider
import com.alibaba.sdk.android.oss.model.DeleteObjectRequest
import com.alibaba.sdk.android.oss.model.GetObjectRequest
import com.alibaba.sdk.android.oss.model.GetObjectResult
import com.alibaba.sdk.android.oss.model.ListObjectsRequest
import com.alibaba.sdk.android.oss.model.ListObjectsResult
import com.alibaba.sdk.android.oss.model.OSSRequest
import com.alibaba.sdk.android.oss.model.OSSResult
import com.alibaba.sdk.android.oss.model.PutObjectRequest
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * 阿里云 OSS 备份服务：封装上传/下载逻辑。
 *
 * 使用方式：
 * val service = OSSBackupService(context, endpoint, accessKeyId, accessKeySecret, bucketName)
 * service.upload(file, objectKey)
 * service.download(objectKey, targetFile)
 */
class OSSBackupService(
    private val context: Context,
    private val endpoint: String,
    private val accessKeyId: String,
    private val accessKeySecret: String,
    private val bucketName: String
) {

    private val oss: OSS by lazy {
        val credentialProvider = OSSPlainTextAKSKCredentialProvider(accessKeyId, accessKeySecret)
        OSSClient(context, endpoint, credentialProvider)
    }

    /**
     * 上传文件到 OSS。
     *
     * @param file 本地文件
     * @param objectKey OSS 对象键（路径），如 "backup/2026-05-01.json"
     * @param onProgress 进度回调 (0.0 ~ 1.0)
     */
    suspend fun upload(
        file: File,
        objectKey: String,
        onProgress: (Double) -> Unit = {}
    ) {
        Log.d(TAG, "开始上传: $objectKey (${file.length()} bytes)")

        suspendCancellableCoroutine { continuation ->
            val putRequest = PutObjectRequest(bucketName, objectKey, file.absolutePath)
            putRequest.progressCallback = OSSProgressCallback<PutObjectRequest> { _, currentSize, totalSize ->
                if (totalSize > 0) {
                    onProgress(currentSize.toDouble() / totalSize)
                }
            }

            oss.asyncPutObject(putRequest, objectCallback(continuation, "上传"))
        }
    }

    /**
     * 从 OSS 下载文件到本地。
     *
     * @param objectKey OSS 对象键
     * @param targetFile 本地目标文件
     * @param onProgress 进度回调 (0.0 ~ 1.0)
     */
    suspend fun download(
        objectKey: String,
        targetFile: File,
        onProgress: (Double) -> Unit = {}
    ) {
        Log.d(TAG, "开始下载: $objectKey")

        val result = suspendCancellableCoroutine<GetObjectResult> { continuation ->
            val getRequest = GetObjectRequest(bucketName, objectKey)
            getRequest.setProgressListener { _, currentSize, totalSize ->
                if (totalSize > 0) {
                    onProgress(currentSize.toDouble() / totalSize.toDouble())
                }
            }

            oss.asyncGetObject(getRequest, objectCallbackWithResult(continuation, "下载"))
        }

        // 把下载的 InputStream 写入文件
        val content = result.objectContent
            ?: throw IllegalStateException("下载结果为空，文件可能不存在或已被删除")
        content.use { input ->
            targetFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }

    /**
     * 列出指定前缀的所有对象（用于获取备份历史列表）
     */
    suspend fun listObjects(prefix: String): List<String> {
        return suspendCancellableCoroutine { continuation ->
            val request = ListObjectsRequest(bucketName)
            request.prefix = prefix

            oss.asyncListObjects(request, object : OSSCompletedCallback<ListObjectsRequest, ListObjectsResult> {
                override fun onSuccess(
                    request: ListObjectsRequest?,
                    result: ListObjectsResult?
                ) {
                    val keys = result?.objectSummaries?.map { it.key } ?: emptyList()
                    continuation.resume(keys)
                }

                override fun onFailure(
                    request: ListObjectsRequest?,
                    clientException: com.alibaba.sdk.android.oss.ClientException?,
                    serviceException: ServiceException?
                ) {
                    val error = when {
                        serviceException != null -> Exception("OSS服务错误 [${serviceException.statusCode}]: ${serviceException.message}")
                        clientException != null -> Exception("OSS客户端错误: ${clientException.message ?: clientException.cause?.message ?: "网络连接失败"}")
                        else -> Exception("列出备份文件失败")
                    }
                    continuation.resumeWithException(error)
                }
            })
        }
    }

    /**
     * 删除指定对象
     */
    suspend fun deleteObject(objectKey: String) {
        suspendCancellableCoroutine { continuation ->
            val request = DeleteObjectRequest(bucketName, objectKey)
            oss.asyncDeleteObject(request, objectCallback(continuation, "删除"))
        }
    }

    private inline fun <reified T : OSSRequest, reified R : OSSResult> objectCallback(
        continuation: kotlinx.coroutines.CancellableContinuation<Unit>,
        operation: String
    ) = object : OSSCompletedCallback<T, R> {
        override fun onSuccess(request: T?, result: R?) {
            Log.d(TAG, "$operation 成功")
            continuation.resume(Unit)
        }

        override fun onFailure(
            request: T?,
            clientException: com.alibaba.sdk.android.oss.ClientException?,
            serviceException: ServiceException?
        ) {
            val error = when {
                serviceException != null -> Exception("OSS服务错误 [${serviceException.statusCode}]: ${serviceException.message}")
                clientException != null -> Exception("OSS客户端错误: ${clientException.message ?: clientException.cause?.message ?: "网络连接失败"}")
                else -> Exception("$operation 失败")
            }
            Log.e(TAG, "$operation 失败", error)
            continuation.resumeWithException(error)
        }
    }

    private inline fun <reified T : OSSRequest, reified R : OSSResult> objectCallbackWithResult(
        continuation: kotlinx.coroutines.CancellableContinuation<R>,
        operation: String
    ) = object : OSSCompletedCallback<T, R> {
        override fun onSuccess(request: T?, result: R?) {
            Log.d(TAG, "$operation 成功")
            if (result != null) {
                continuation.resume(result)
            } else {
                continuation.resumeWithException(Exception("$operation 结果为空"))
            }
        }

        override fun onFailure(
            request: T?,
            clientException: com.alibaba.sdk.android.oss.ClientException?,
            serviceException: ServiceException?
        ) {
            val error = when {
                serviceException != null -> Exception("OSS服务错误 [${serviceException.statusCode}]: ${serviceException.message}")
                clientException != null -> Exception("OSS客户端错误: ${clientException.message ?: clientException.cause?.message ?: "网络连接失败"}")
                else -> Exception("$operation 失败")
            }
            Log.e(TAG, "$operation 失败", error)
            continuation.resumeWithException(error)
        }
    }

    companion object {
        private const val TAG = "OSSBackupService"
    }
}
