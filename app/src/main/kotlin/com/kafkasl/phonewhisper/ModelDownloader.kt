package com.kafkasl.phonewhisper

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.Request
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import java.io.*
import java.util.concurrent.TimeUnit

data class Model(
    val name: String,
    /** Directory name under files/models (also tar.bz2 asset name for GitHub-hosted models). */
    val archive: String,
    val sizeMb: Int,
    val qualityRes: Int,
    val recommended: Boolean = false,
    /** Non-null: download individual files from this HuggingFace repo instead of a GitHub tar.bz2. */
    val hfRepo: String? = null,
    /** Repo-relative file paths, only used when hfRepo != null. */
    val hfFiles: List<String> = emptyList(),
)

/** Fork decision: cloud (OpenAI) transcription/cleanup is disabled. */
const val CLOUD_ENABLED = false

val MODEL_CATALOG = listOf(
    Model("Qwen3-ASR 0.6B", "sherpa-onnx-qwen3-asr-0.6B-int8-2026-03-25",
        838, R.string.quality_qwen06, recommended = true),
    Model("Qwen3-ASR 1.7B", "sherpa-onnx-qwen3-asr-1.7B-int8",
        2290, R.string.quality_qwen17,
        hfRepo = "ilmina/qwen3-asr-1.7b-sherpa-onnx",
        hfFiles = listOf(
            "conv_frontend.onnx",
            "encoder.int8.onnx",
            "decoder.int8.onnx",
            "tokenizer/merges.txt",
            "tokenizer/vocab.json",
            "tokenizer/tokenizer_config.json",
        )),
)

sealed class DownloadState {
    data class Downloading(val progress: Float) : DownloadState()
    object Extracting : DownloadState()
    object Done : DownloadState()
    data class Error(val message: String) : DownloadState()
}

object ModelDownloader {
    private const val BASE_URL =
        "https://github.com/k2-fsa/sherpa-onnx/releases/download/asr-models"
    private val client = OkHttpClient.Builder()
        .readTimeout(60, TimeUnit.SECONDS).build()

    fun modelDir(ctx: Context, model: Model) =
        File(ctx.filesDir, "models/${model.archive}")

    fun isInstalled(ctx: Context, model: Model): Boolean {
        val dir = modelDir(ctx, model)
        if (!dir.exists()) return false
        // A truncated onnx file passes "exists" but aborts the process natively
        // (Ort::Exception / SIGABRT) on load — validate minimum sizes instead.
        fun ok(f: File?, minMb: Long) = f != null && f.isFile && f.length() >= minMb * 1024 * 1024
        val files = dir.listFiles().orEmpty()
        val encoder = files.firstOrNull { it.name.startsWith("encoder") && it.name.endsWith(".onnx") }
        val decoder = files.firstOrNull { it.name.startsWith("decoder") && it.name.endsWith(".onnx") }
        return ok(File(dir, "conv_frontend.onnx"), 20) &&
            ok(encoder, 50) && ok(decoder, 200) &&
            File(dir, "tokenizer").isDirectory
    }

    /** Download and extract model. Callbacks fire on background thread. */
    fun download(ctx: Context, model: Model, onState: (DownloadState) -> Unit) {
        if (model.hfRepo != null) {
            downloadFromHf(ctx, model, onState)
            return
        }
        val url = "$BASE_URL/${model.archive}.tar.bz2"
        val tmpFile = File(ctx.cacheDir, "${model.archive}.tar.bz2")
        val outDir = File(ctx.filesDir, "models")

        Thread {
            try {
                downloadFile(url, tmpFile, onState)
                onState(DownloadState.Extracting)
                extractTarBz2(tmpFile, outDir)
                onState(DownloadState.Done)
            } catch (e: Exception) {
                onState(DownloadState.Error(e.message ?: "Unknown error"))
            } finally {
                tmpFile.delete()
            }
        }.start()
    }

    /** Download each repo file into the model dir; progress is aggregated across files. */
    private fun downloadFromHf(ctx: Context, model: Model, onState: (DownloadState) -> Unit) {
        val dir = modelDir(ctx, model)
        Thread {
            try {
                dir.mkdirs()
                val n = model.hfFiles.size
                model.hfFiles.forEachIndexed { i, rel ->
                    val dest = File(dir, rel)
                    dest.parentFile?.mkdirs()
                    val url = "https://huggingface.co/${model.hfRepo}/resolve/main/$rel"
                    downloadFile(url, dest) { st ->
                        when (st) {
                            is DownloadState.Downloading ->
                                onState(DownloadState.Downloading((i + st.progress) / n))
                            else -> onState(st)
                        }
                    }
                }
                File(dir, ".complete").createNewFile()
                onState(DownloadState.Done)
            } catch (e: Exception) {
                onState(DownloadState.Error(e.message ?: "Unknown error"))
            }
        }.start()
    }

    fun delete(ctx: Context, model: Model) =
        modelDir(ctx, model).deleteRecursively()

    private fun downloadFile(
        url: String, dest: File, onState: (DownloadState) -> Unit
    ) {
        val response = client.newCall(Request.Builder().url(url).build()).execute()
        if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
        val body = response.body ?: throw IOException("Empty response")
        val total = body.contentLength()
        var downloaded = 0L

        body.byteStream().use { src ->
            FileOutputStream(dest).use { dst ->
                val buf = ByteArray(16384)
                var n: Int
                while (src.read(buf).also { n = it } != -1) {
                    dst.write(buf, 0, n)
                    downloaded += n
                    if (total > 0)
                        onState(DownloadState.Downloading(downloaded.toFloat() / total))
                }
            }
        }
    }

    /** Extract tar.bz2 to outDir. Validates paths to prevent traversal. */
    fun extractTarBz2(archive: File, outDir: File) {
        outDir.mkdirs()
        val bzIn = BZip2CompressorInputStream(BufferedInputStream(FileInputStream(archive)))
        TarArchiveInputStream(bzIn).use { tar ->
            generateSequence { tar.nextEntry }.forEach { entry ->
                val dest = File(outDir, entry.name)
                require(dest.canonicalPath.startsWith(outDir.canonicalPath)) {
                    "Path traversal: ${entry.name}"
                }
                if (entry.isDirectory) dest.mkdirs()
                else {
                    dest.parentFile?.mkdirs()
                    FileOutputStream(dest).use { tar.copyTo(it) }
                }
            }
        }
    }
}
