package com.bookbook.domain.rent.service

import com.bookbook.global.exception.ServiceException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import kotlin.math.max
import kotlin.math.min

// 09.01 양현준
// 책 제목 파싱 서비스
@Service
class BookTitleParsingService {

    companion object {
        private val log = LoggerFactory.getLogger(BookTitleParsingService::class.java)

        // 책과 관련된 불필요한 키워드들
        private val NOISE_KEYWORDS = listOf(
            "베스트셀러", "신간", "추천", "도서", "책", "북", "book",
            "저자", "글쓴이", "지은이", "옮긴이", "번역", "출판사",
            "ISBN", "정가", "가격", "원", "페이지", "쪽"
        )

        // 제목에서 제거할 특수문자 패턴
        private val SPECIAL_CHARS_REGEX = Regex("[『』「」〈〉《》\\[\\]()（）]")
    }

    // OCR 텍스트에서 책 제목을 추출하는 메인 함수 (개선된 버전)
    fun extractBookTitle(ocrText: String): Pair<String?, Double> {
        try {
            if (ocrText.isBlank()) {
                return Pair(null, 0.0)
            }

            log.info("OCR 원본 텍스트: $ocrText")

            // 개선된 로직: ImageOcrService에서 이미 위치 기반으로 제목을 우선 추출했으므로
            // 여기서는 추가적인 정제와 검증을 담당

            // 1단계: 텍스트 전처리 (개선된 버전)
            val cleanedLines = preprocessText(ocrText)

            // 2단계: 후보 제목들 추출 (위치 정보 활용)
            val titleCandidates = extractTitleCandidatesWithPosition(cleanedLines, ocrText)

            // 3단계: 최적의 제목 선택
            val bestTitle = selectBestTitle(titleCandidates)

            log.info("추출된 책 제목: ${bestTitle.first}, 신뢰도: ${bestTitle.second}")

            return bestTitle

        } catch (e: Exception) {
            log.error("책 제목 추출 중 오류 발생", e)
            throw ServiceException("500-4", "책 제목 추출 중 오류가 발생했습니다")
        }
    }

    // 텍스트 전처리
    private fun preprocessText(text: String): List<String> {
        return text.split("\\n")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .filter { it.length >= 2 }  // 너무 짧은 텍스트 제거
            .filter { it.length <= 50 } // 너무 긴 텍스트 제거 (제목일 가능성 낮음)
    }

    /**
     * 제목 후보들 추출 (기존 방식)
     */
    private fun extractTitleCandidates(lines: List<String>): List<Pair<String, Double>> {
        val candidates = mutableListOf<Pair<String, Double>>()

        lines.forEachIndexed { index, line ->
            val cleanLine = cleanTitle(line)
            if (cleanLine.isNotBlank()) {
                val confidence = calculateTitleConfidence(cleanLine, index, lines.size)
                candidates.add(Pair(cleanLine, confidence))
            }
        }

        return candidates.sortedByDescending { it.second }
    }

    /**
     * 제목 후보들 추출 (위치 정보 활용, 개선된 버전)
     */
    private fun extractTitleCandidatesWithPosition(lines: List<String>, originalText: String): List<Pair<String, Double>> {
        val candidates = mutableListOf<Pair<String, Double>>()

        // 개선된 로직: ImageOcrService에서 이미 위치 기반으로 제목을 우선 추출했으므로
        // 첫 번째 줄(가장 상단)에 높은 우선순위 부여

        lines.forEachIndexed { index, line ->
            val cleanLine = cleanTitle(line)
            if (cleanLine.isNotBlank()) {
                // 위치 기반 신뢰도 계산 (상단일수록 높은 점수)
                val confidence = calculateTitleConfidenceWithPosition(cleanLine, index, lines.size, originalText)
                candidates.add(Pair(cleanLine, confidence))

                log.info("제목 후보 $index: '$cleanLine' (신뢰도: ${(confidence * 100).toInt()}%)")
            }
        }

        return candidates.sortedByDescending { it.second }
    }

    // 제목 정리 (특수문자, 노이즈 제거)
    private fun cleanTitle(title: String): String {
        var cleaned = title

        // 특수문자 제거
        cleaned = SPECIAL_CHARS_REGEX.replace(cleaned, "")

        // 노이즈 키워드 제거
        NOISE_KEYWORDS.forEach { noise ->
            cleaned = cleaned.replace(noise, "", ignoreCase = true)
        }

        return cleaned.trim()
    }

    // 제목 신뢰도 계산 (기존 방식)
    private fun calculateTitleConfidence(title: String, position: Int, totalLines: Int): Double {
        var confidence = 0.0

        // 1. 위치 점수 (상단에 있을수록 높은 점수)
        val positionScore = max(0.0, 1.0 - (position.toDouble() / totalLines)) * 0.4
        confidence += positionScore

        // 2. 길이 점수 (적당한 길이의 제목일수록 높은 점수)
        val lengthScore = when (title.length) {
            in 3..20 -> 0.3
            in 21..30 -> 0.2
            in 2..2 -> 0.1
            else -> 0.0
        }
        confidence += lengthScore

        // 3. 한글 비율 점수
        val koreanRatio = title.count { it.code in 0xAC00..0xD7AF }.toDouble() / title.length
        confidence += koreanRatio * 0.2

        // 4. 노이즈 키워드 포함 시 감점
        val hasNoise = NOISE_KEYWORDS.any { title.contains(it, ignoreCase = true) }
        if (hasNoise) {
            confidence -= 0.3
        }

        // 5. 숫자나 특수문자 과다 포함 시 감점
        val specialCharRatio = title.count { !it.isLetterOrDigit() }.toDouble() / title.length
        if (specialCharRatio > 0.3) {
            confidence -= 0.2
        }

        return max(0.0, min(1.0, confidence))
    }

    // 제목 신뢰도 계산 (위치 정보 활용, 개선된 버전)
    private fun calculateTitleConfidenceWithPosition(title: String, position: Int, totalLines: Int, originalText: String): Double {
        var confidence = 0.0

        // 1. 위치 점수 (상단에 있을수록 높은 점수) - 가중치 증가
        val positionScore = max(0.0, 1.0 - (position.toDouble() / totalLines)) * 0.6 // 0.4 → 0.6으로 증가
        confidence += positionScore

        // 2. 길이 점수 (적당한 길이의 제목일수록 높은 점수)
        val lengthScore = when (title.length) {
            in 3..20 -> 0.25
            in 21..30 -> 0.15
            in 2..2 -> 0.05
            else -> 0.0
        }
        confidence += lengthScore

        // 3. 한글 비율 점수
        val koreanRatio = title.count { it.code in 0xAC00..0xD7AF }.toDouble() / title.length
        confidence += koreanRatio * 0.15

        // 4. 노이즈 키워드 포함 시 감점 (가중치 증가)
        val hasNoise = NOISE_KEYWORDS.any { title.contains(it, ignoreCase = true) }
        if (hasNoise) {
            confidence -= 0.4 // 0.3 → 0.4로 증가
        }

        // 5. 숫자나 특수문자 과다 포함 시 감점
        val specialCharRatio = title.count { !it.isLetterOrDigit() }.toDouble() / title.length
        if (specialCharRatio > 0.3) {
            confidence -= 0.25
        }

        // 6. 새로운 개선사항: 출판사명이나 장르 키워드 포함 시 추가 감점
        val publisherKeywords = listOf("한강", "문학동네", "민음사", "창비", "열린책들", "시집", "소설", "에세이")
        val hasPublisherInfo = publisherKeywords.any { title.contains(it, ignoreCase = true) }
        if (hasPublisherInfo) {
            confidence -= 0.3
            log.info("출판사/장르 키워드 감지로 감점: '$title'")
        }

        // 7. 새로운 개선사항: 마침표나 쉼표로 구분된 경우 첫 번째 부분 우선
        if (title.contains(".") || title.contains(",")) {
            val firstPart = title.split("[.,]".toRegex())[0].trim()
            if (firstPart.length >= 3) {
                confidence += 0.1 // 첫 번째 부분이 의미있는 길이면 보너스
                log.info("구분자로 분리된 첫 번째 부분 우선: '$firstPart'")
            }
        }

        return max(0.0, min(1.0, confidence))
    }

    // 최적의 제목 선택
    private fun selectBestTitle(candidates: List<Pair<String, Double>>): Pair<String?, Double> {
        if (candidates.isEmpty()) {
            return Pair(null, 0.0)
        }

        val best = candidates.first()

        // 신뢰도가 너무 낮으면 null 반환
        return if (best.second >= 0.3) {
            best
        } else {
            Pair(null, best.second)
        }
    }
}