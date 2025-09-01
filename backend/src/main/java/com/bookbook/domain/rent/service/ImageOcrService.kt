package com.bookbook.domain.rent.service

import com.google.cloud.vision.v1.*
import com.google.protobuf.ByteString
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import kotlin.jvm.java

// 09.01 양현준
// Google Vision API 사용을 위한 서비스
@Service
class ImageOcrService {

    @Value("\${GOOGLE_CLOUD_PROJECT_ID}")
    private lateinit var projectId: String

    companion object {
        private val log = LoggerFactory.getLogger(ImageOcrService::class.java)
    }

    // Google Cloud Vision API를 사용하여 이미지에서 텍스트 추출
    fun extractTextFromImage(imageFile: MultipartFile): String {
        try {
            // Vision API 클라이언트 생성
            ImageAnnotatorClient.create().use { vision ->

                // 이미지 파일을 ByteString으로 변환
                val imgBytes = ByteString.copyFrom(imageFile.bytes)
                val img = Image.newBuilder().setContent(imgBytes).build()

                // OCR 요청 생성
                val feature = Feature.newBuilder()
                    .setType(Feature.Type.TEXT_DETECTION)
                    .build()

                val request = AnnotateImageRequest.newBuilder()
                    .addFeatures(feature)
                    .setImage(img)
                    .build()

                // Vision API 호출
                val response = vision.batchAnnotateImages(listOf(request))
                val responses = response.responsesList

                if (responses.isNotEmpty()) {
                    val annotation = responses[0]

                    if (annotation.hasError()) {
                        log.error("Vision API 오류: ${annotation.error.message}")
                        throw RuntimeException("Vision API 오류: ${annotation.error.message}")
                    }

                    // 전체 텍스트 추출
                    return if (annotation.textAnnotationsList.isNotEmpty()) {
                        annotation.textAnnotationsList[0].description
                    } else {
                        ""
                    }
                }

                return ""
            }
        } catch (e: IOException) {
            log.error("OCR 처리 중 IO 오류 발생", e)
            throw RuntimeException("OCR 처리 실패: ${e.message}")
        } catch (e: Exception) {
            log.error("OCR 처리 중 예상치 못한 오류 발생", e)
            throw RuntimeException("OCR 처리 실패: ${e.message}")
        }
    }

    // 이미지 파일 유효성 검사
    fun validateImageFile(file: MultipartFile): Boolean {
        // 파일 크기 체크 (10MB 제한)
        val maxSizeBytes = 10 * 1024 * 1024
        if (file.size > maxSizeBytes) {
            throw IllegalArgumentException("파일 크기가 너무 큽니다. 최대 10MB까지 업로드 가능합니다.")
        }

        // 파일 형식 체크
        val supportedTypes = listOf("image/jpeg", "image/jpg", "image/png", "image/gif", "image/bmp", "image/webp")
        if (file.contentType !in supportedTypes) {
            throw IllegalArgumentException("지원하지 않는 파일 형식입니다. JPG, PNG, GIF, BMP, WEBP 파일만 업로드 가능합니다.")
        }

        return true
    }

}