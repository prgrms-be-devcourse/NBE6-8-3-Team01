// E:\dev_project\NBE6-8-3-Team01\backend\src\main\java\com\bookbook\domain\rent\service\ImageOcrService.kt
package com.bookbook.domain.rent.service

import com.bookbook.global.exception.ServiceException
import com.google.api.gax.core.FixedCredentialsProvider
import com.google.auth.oauth2.GoogleCredentials
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

    // 이미지에서 텍스트를 추출하는 메인 함수
    fun extractTextFromImage(imageFile: MultipartFile): String {
        try {
            // Vision API 클라이언트 생성 (.env 파일의 인증 정보 사용)
            val credentialsPath = System.getProperty("GOOGLE_APPLICATION_CREDENTIALS")
            val credentials = GoogleCredentials.fromStream(java.io.FileInputStream(credentialsPath))
            val settings = ImageAnnotatorSettings.newBuilder()
                .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
                .build()
            ImageAnnotatorClient.create(settings).use { vision ->
                
                // 이미지 파일을 ByteString으로 변환
                val imgBytes = ByteString.copyFrom(imageFile.bytes)
                val img = Image.newBuilder().setContent(imgBytes).build()
                
                // 핵심 분석 기능만 요청 (비용 효율적)
                // 1. 문서 텍스트 감지: 일반 TEXT_DETECTION보다 정확한 텍스트 인식 및 위치 정보 보존
                val documentTextFeature = Feature.newBuilder()
                    .setType(Feature.Type.DOCUMENT_TEXT_DETECTION)
                    .build()
                
                // 2. 이미지 속성 분석: 주요 색상, 밝기 등 이미지의 시각적 특성 분석 (책 상태 추정에 활용)
                val imagePropsFeature = Feature.newBuilder()
                    .setType(Feature.Type.IMAGE_PROPERTIES)
                    .build()
                
                // 3. 웹 검색: 유사한 이미지나 책 표지 검색으로 결과 검증
                val webFeature = Feature.newBuilder()
                    .setType(Feature.Type.WEB_DETECTION)
                    .setMaxResults(5) // 최대 5개의 웹 검색 결과
                    .build()
                
                // 핵심 분석 기능만 요청 (비용 효율적)
                val request = AnnotateImageRequest.newBuilder()
                    .addFeatures(documentTextFeature) // 문서 텍스트 (정확한 텍스트 추출 + 위치 정보)
                    .addFeatures(imagePropsFeature)    // 이미지 속성 (책 상태 분석용)
                    .addFeatures(webFeature)           // 웹 검색 (결과 검증, 한글 책 우선 검색)
                    .setImage(img)                      // 분석할 이미지
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
                    
                    // Vision API 분석 결과 로깅
                    logVisionApiResults(annotation)
                    
                    // 1. 웹 검색 결과에서 책 제목 추출 시도
                    if (annotation.hasWebDetection()) {
                        val webDetection = annotation.webDetection
                        val webTitle = extractTitleFromWebDetection(webDetection)
                        
                        if (webTitle.isNotBlank()) {
                            log.info("웹 검색 결과에서 책 제목 추출 성공: '$webTitle'")
                            return webTitle
                        }
                    }
                    
                    // 2. 웹 검색 결과가 없으면 텍스트 추출 요청
                    log.info("웹 검색 결과에서 책 제목을 찾을 수 없어 텍스트 추출을 시도합니다")
                    return extractTextWithFallback(vision, img)
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

    // 이미지 파일 검증
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

    // 웹 검색 결과가 없을 때 텍스트 추출을 시도하는 fallback 함수
    private fun extractTextWithFallback(vision: ImageAnnotatorClient, img: Image): String {
        try {
            log.info("=== 텍스트 추출 fallback 시작 ===")
            
            // DOCUMENT_TEXT_DETECTION 요청 생성
            val documentTextFeature = Feature.newBuilder()
                .setType(Feature.Type.DOCUMENT_TEXT_DETECTION)
                .build()
            
            val request = AnnotateImageRequest.newBuilder()
                .addFeatures(documentTextFeature)
                .setImage(img)
                .build()
            
            // Vision API 호출
            val response = vision.batchAnnotateImages(listOf(request))
            val responses = response.responsesList
            
            if (responses.isNotEmpty()) {
                val annotation = responses[0]
                
                if (annotation.hasError()) {
                    log.error("텍스트 추출 fallback 중 Vision API 오류: ${annotation.error.message}")
                    return ""
                }
                
                // DOCUMENT_TEXT_DETECTION 결과에서 텍스트 추출
                if (annotation.hasFullTextAnnotation()) {
                    val fullTextAnnotation = annotation.fullTextAnnotation
                    if (fullTextAnnotation.pagesList.isNotEmpty()) {
                        val page = fullTextAnnotation.pagesList[0]
                        if (page.blocksList.isNotEmpty()) {
                            val blocks = page.blocksList
                            
                            log.info("=== DOCUMENT_TEXT_DETECTION 결과 ===")
                            log.info("총 블록 수: ${blocks.size}")
                            
                            // 상단에 위치한 텍스트를 우선적으로 제목 후보로 선정
                            val titleCandidates = mutableListOf<String>()
                            
                            blocks.forEach { block ->
                                try {
                                    if (block.hasBoundingBox() && block.boundingBox.verticesList.isNotEmpty()) {
                                        val y = block.boundingBox.verticesList[0].y
                                        val text = extractTextFromBlock(block)
                                        
                                        if (text.isNotBlank()) {
                                            log.info("텍스트: '$text', Y좌표: $y")
                                            
                                            // 상단 100px 이내에 있는 텍스트를 제목 후보로 선정
                                            if (y < 100) {
                                                titleCandidates.add(text)
                                                log.info("제목 후보 추가: '$text' (Y: $y)")
                                            }
                                        }
                                    }
                                } catch (e: Exception) {
                                    log.warn("블록 처리 중 오류 발생: ${e.message}")
                                }
                            }
                            
                                                         // 제목 후보가 있으면 우선 반환, 없으면 전체 텍스트 반환
                             return if (titleCandidates.isNotEmpty()) {
                                 val titleText = titleCandidates.joinToString(" ")
                                 log.info("제목 후보에서 추출된 텍스트: '$titleText'")
                                 titleText
                             } else {
                                 val allText = blocks.joinToString(" ") { extractTextFromBlock(it) }
                                 log.info("전체 텍스트 반환: '$allText'")
                                 allText
                             }
                        }
                    }
                }
            }
            
            log.warn("텍스트 추출 fallback에서도 결과를 얻을 수 없습니다")
            return ""
            
        } catch (e: Exception) {
            log.error("텍스트 추출 fallback 중 오류 발생", e)
            return ""
        }
    }

    // 웹 검색 결과에서 책 제목을 추출하는 함수 (한글 우선)
    private fun extractTitleFromWebDetection(webDetection: com.google.cloud.vision.v1.WebDetection): String {
        return try {
            log.info("=== 웹 검색 결과 분석 시작 ===")
            
            // 웹 엔티티에서 책 제목 후보 추출
            val webEntities = webDetection.webEntitiesList
            if (webEntities.isNotEmpty()) {
                log.info("웹 엔티티 수: ${webEntities.size}")
                
                // 신뢰도 기준을 40%로 낮춤 (더 많은 후보 확보)
                val highConfidenceEntities = webEntities
                    .filter { it.score > 0.4 } // 신뢰도 40% 이상으로 확장
                    .sortedByDescending { it.score } // 신뢰도 높은 순으로 정렬
                
                log.info("신뢰도 40% 이상 엔티티: ${highConfidenceEntities.map { "${it.description}(${(it.score * 100).toInt()}%)" }}")
                
                // 한글 포함 여부를 확인하는 함수
                fun containsKorean(text: String): Boolean {
                    return text.any { it.code in 0xAC00..0xD7AF || it.code in 0x1100..0x11FF || it.code in 0x3130..0x318F }
                }
                
                // 책 제목으로 보이는 엔티티 찾기 (한글 우선)
                val bookTitleCandidates = highConfidenceEntities
                    .filter { entity ->
                        val description = entity.description.lowercase()
                        // 책 제목으로 보이는 키워드가 포함된 경우
                        !description.contains("publisher") &&
                        !description.contains("author") &&
                        !description.contains("isbn") &&
                        !description.contains("price") &&
                        !description.contains("page") &&
                        !description.contains("used") &&
                        !description.contains("good") &&
                        !description.contains("condition") &&
                        !description.contains("sale") &&
                        !description.contains("buy") &&
                        !description.contains("sell") &&
                        description.length in 2..80 // 길이 제한을 넓게 (2~80자)
                    }
                
                if (bookTitleCandidates.isNotEmpty()) {
                    // 한글이 포함된 후보를 우선적으로 선택
                    val koreanCandidates = bookTitleCandidates.filter { containsKorean(it.description) }
                    val nonKoreanCandidates = bookTitleCandidates.filter { !containsKorean(it.description) }
                    
                    // 한글 후보가 있으면 한글 후보 중에서 선택, 없으면 일반 후보에서 선택
                    val bestCandidate = if (koreanCandidates.isNotEmpty()) {
                        // 한글 후보 중에서 한글 문자가 더 많이 포함된 것을 우선 선택
                        val bestKoreanCandidate = koreanCandidates.maxByOrNull { candidate ->
                            candidate.description.count { char ->
                                char.code in 0xAC00..0xD7AF || char.code in 0x1100..0x11FF || char.code in 0x3130..0x318F
                            }
                        } ?: koreanCandidates.first()
                        
                        log.info("한글 후보 중에서 선택 (한글 문자 수: ${bestKoreanCandidate.description.count { char -> char.code in 0xAC00..0xD7AF || char.code in 0x1100..0x11FF || char.code in 0x3130..0x318F }}): ${koreanCandidates.map { it.description }}")
                        bestKoreanCandidate
                    } else {
                        log.info("한글 후보가 없어 일반 후보 중에서 선택: ${nonKoreanCandidates.map { it.description }}")
                        nonKoreanCandidates.first()
                    }
                    
                    log.info("웹 엔티티에서 선택된 제목: '${bestCandidate.description}' (신뢰도: ${(bestCandidate.score * 100).toInt()}%, 한글 포함: ${containsKorean(bestCandidate.description)})")
                    return bestCandidate.description
                }
            }
            
            log.info("웹 검색 결과에서 적절한 제목을 찾을 수 없습니다")
            ""
            
        } catch (e: Exception) {
            log.error("웹 검색 결과 분석 중 오류 발생: ${e.message}")
            ""
        }
    }
    
    
     

     


     // Block에서 텍스트를 추출하는 헬퍼 함수
     private fun extractTextFromBlock(block: com.google.cloud.vision.v1.Block): String {
        return try {
            // Block의 paragraphs를 통해 텍스트 추출
            if (block.paragraphsList.isNotEmpty()) {
                val paragraphTexts = block.paragraphsList.mapNotNull { paragraph ->
                    if (paragraph.wordsList.isNotEmpty()) {
                        val wordTexts = paragraph.wordsList.mapNotNull { word ->
                            if (word.symbolsList.isNotEmpty()) {
                                word.symbolsList.joinToString("") { it.text }
                            } else null
                        }
                        wordTexts.joinToString("")
                    } else null
                }
                paragraphTexts.joinToString(" ")
            } else {
                // paragraphs가 없으면 빈 문자열 반환
                ""
            }
        } catch (e: Exception) {
            log.warn("Block에서 텍스트 추출 중 오류: ${e.message}")
            ""
        }
    }



    // Vision API 분석 결과 로깅 함수 (핵심 기능만)
    private fun logVisionApiResults(annotation: com.google.cloud.vision.v1.AnnotateImageResponse) {
        try {
            // 이미지 속성 결과 (책 상태 분석용)
            if (annotation.hasImagePropertiesAnnotation()) {
                val props = annotation.imagePropertiesAnnotation
                if (props.hasDominantColors()) {
                    val dominantColor = props.dominantColors.colorsList.firstOrNull()
                    dominantColor?.let { color ->
                        log.info("=== 이미지 분석 ===")
                        log.info("주요 색상: R=${color.color.red.toInt()}, G=${color.color.green.toInt()}, B=${color.color.blue.toInt()}")
                        log.info("색상 비율: ${(color.pixelFraction * 100).toInt()}%")
                    }
                }
            }
            
            // 웹 검색 결과 (새로 추가)
            if (annotation.hasWebDetection()) {
                val webDetection = annotation.webDetection
                log.info("=== 웹 검색 결과 ===")
                
                if (webDetection.webEntitiesList.isNotEmpty()) {
                    log.info("웹 엔티티:")
                    webDetection.webEntitiesList.take(3).forEach { entity ->
                        log.info("- ${entity.description}: ${(entity.score * 100).toInt()}%")
                    }
                }
                
                if (webDetection.fullMatchingImagesList.isNotEmpty()) {
                    log.info("완전 일치 이미지: ${webDetection.fullMatchingImagesList.size}개")
                }
                
                if (webDetection.partialMatchingImagesList.isNotEmpty()) {
                    log.info("부분 일치 이미지: ${webDetection.partialMatchingImagesList.size}개")
                }
            }
            
            // DOCUMENT_TEXT_DETECTION 결과 요약
            if (annotation.hasFullTextAnnotation()) {
                val fullText = annotation.fullTextAnnotation
                log.info("=== DOCUMENT_TEXT_DETECTION 요약 ===")
                log.info("텍스트 길이: ${fullText.text.length}자")
                if (fullText.pagesList.isNotEmpty()) {
                    log.info("페이지 수: ${fullText.pagesList.size}")
                }
            }
            
        } catch (e: Exception) {
            log.warn("Vision API 결과 로깅 중 오류: ${e.message}")
        }
    }
}