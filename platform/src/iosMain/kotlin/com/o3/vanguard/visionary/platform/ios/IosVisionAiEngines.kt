package com.o3.vanguard.visionary.platform.ios

import com.o3.vanguard.visionary.core.common.AppError
import com.o3.vanguard.visionary.core.common.AppResult
import com.o3.vanguard.visionary.domain.model.OcrResult
import com.o3.vanguard.visionary.domain.model.OcrTextLine
import com.o3.vanguard.visionary.domain.port.ImageLabelEngine
import com.o3.vanguard.visionary.domain.port.OcrEngine
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.create
import platform.Vision.VNClassificationObservation
import platform.Vision.VNClassifyImageRequest
import platform.Vision.VNImageRequestHandler
import platform.Vision.VNRecognizeTextRequest
import platform.Vision.VNRecognizedText
import platform.Vision.VNRecognizedTextObservation
import platform.Vision.VNRequestTextRecognitionLevelAccurate

class IosOcrEngine : OcrEngine {

    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    override suspend fun recognizeText(uri: String): AppResult<OcrResult> = try {
        val nsData = loadDataFromUri(uri)
            ?: return AppResult.Err(AppError.Io("Could not load image data for $uri"))

        val textLines = mutableListOf<OcrTextLine>()
        val fullTextBuilder = StringBuilder()

        val request = VNRecognizeTextRequest { request, error ->
            if (error != null) return@VNRecognizeTextRequest
            val observations = request?.results ?: return@VNRecognizeTextRequest

            for (obs in observations) {
                val observation = obs as? VNRecognizedTextObservation ?: continue
                val topCandidate = observation.topCandidates(1UL).firstOrNull() as? VNRecognizedText ?: continue
                val lineText = topCandidate.string.trim()
                if (lineText.isNotBlank()) {
                    if (fullTextBuilder.isNotEmpty()) fullTextBuilder.append("\n")
                    fullTextBuilder.append(lineText)

                    val rect = observation.boundingBox.useContents {
                        com.o3.vanguard.visionary.domain.model.NormalizedRect(
                            left = origin.x.toFloat(),
                            top = (1.0 - (origin.y + size.height)).toFloat(),
                            right = (origin.x + size.width).toFloat(),
                            bottom = (1.0 - origin.y).toFloat(),
                        )
                    }

                    textLines.add(
                        OcrTextLine(
                            text = lineText,
                            confidence = topCandidate.confidence,
                            boundingBox = rect,
                        )
                    )
                }
            }
        }

        request.recognitionLevel = VNRequestTextRecognitionLevelAccurate
        request.usesLanguageCorrection = true

        val handler = VNImageRequestHandler(data = nsData, options = emptyMap<Any?, Any>())
        handler.performRequests(listOf(request), null)

        AppResult.Ok(OcrResult(fullTextBuilder.toString(), textLines))
    } catch (e: Throwable) {
        AppResult.Err(AppError.Io(e.message ?: "iOS Vision OCR failed"))
    }
}

class IosImageLabelEngine : ImageLabelEngine {

    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    override suspend fun labelImage(uri: String): AppResult<List<String>> = try {
        val nsData = loadDataFromUri(uri)
            ?: return AppResult.Err(AppError.Io("Could not load image data for $uri"))

        val labels = mutableListOf<String>()

        val request = VNClassifyImageRequest { request, error ->
            if (error != null) return@VNClassifyImageRequest
            val observations = request?.results ?: return@VNClassifyImageRequest

            for (obs in observations) {
                val observation = obs as? VNClassificationObservation ?: continue
                if (observation.confidence >= 0.5f) {
                    val label = observation.identifier.trim()
                    if (label.isNotBlank()) {
                        labels.add(label)
                    }
                }
            }
        }

        val handler = VNImageRequestHandler(data = nsData, options = emptyMap<Any?, Any>())
        handler.performRequests(listOf(request), null)

        AppResult.Ok(labels.distinct())
    } catch (e: Throwable) {
        AppResult.Err(AppError.Io(e.message ?: "iOS Vision Image Labeling failed"))
    }
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private suspend fun loadDataFromUri(uri: String): NSData? {
    if (uri.startsWith("ph://")) {
        val bytes = PhAssetResolver.fetchImageData(uri) ?: return null
        if (bytes.isEmpty()) return null
        return bytes.usePinned { pinned ->
            NSData.create(bytes = pinned.addressOf(0), length = bytes.size.toULong())
        }
    }
    val path = uri.removePrefix("file://")
    return NSFileManager.defaultManager.contentsAtPath(path)
}
