// E:\dev_project\NBE6-8-3-Team01\backend\src\main\java\com\bookbook\domain\rent\service\ImageOcrService.kt
package com.bookbook.domain.rent.service

import com.bookbook.global.exception.ServiceException
import com.google.cloud.vision.v1.*
import com.google.protobuf.ByteString
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.io.IOException

@Service
class ImageOcrService {

    @Value("\${google.cloud.project-id}")
    private lateinit var projectId: String

    companion object {
        private val log = LoggerFactory.getLogger(ImageOcrService::class.java)
    }

    /**
     * 이미지에서 텍스트를 추출하는 메인 함수
     */
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
                        throw ServiceException("500-1", "Google Vision API 처리 중 오류가 발생했습니다: ${annotation.error.message}")
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
            throw ServiceException("500-2", "이미지 처리 중 오류가 발생했습니다")
        } catch (e: ServiceException) {
            // ServiceException은 그대로 재발생
            throw e
        } catch (e: Exception) {
            log.error("OCR 처리 중 예상치 못한 오류 발생", e)
            throw ServiceException("500-3", "OCR 처리 중 예상치 못한 오류가 발생했습니다")
        }
    }

    /**
     * 이미지 파일 검증
     */
    fun validateImageFile(file: MultipartFile) {
        // 파일 크기 체크 (10MB 제한)
        val maxSizeBytes = 10 * 1024 * 1024
        if (file.size > maxSizeBytes) {
            throw ServiceException("400-2", "파일 크기가 너무 큽니다. 최대 10MB까지 업로드 가능합니다.")
        }
        
        // 파일 형식 체크
        val supportedTypes = listOf("image/jpeg", "image/jpg", "image/png", "image/gif", "image/bmp", "image/webp")
        if (file.contentType !in supportedTypes) {
            throw ServiceException("400-3", "지원하지 않는 파일 형식입니다. JPG, PNG, GIF, BMP, WEBP 파일만 업로드 가능합니다.")
        }
        
        // 빈 파일 체크
        if (file.isEmpty) {
            throw ServiceException("400-4", "파일이 비어있습니다.")
        }
    }
}