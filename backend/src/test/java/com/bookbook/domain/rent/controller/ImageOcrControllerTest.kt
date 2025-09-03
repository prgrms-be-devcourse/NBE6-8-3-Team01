package com.bookbook.domain.rent.controller

import com.bookbook.domain.rent.dto.response.ImageOcrResponseDto
import com.bookbook.domain.rent.dto.response.BookSearchResponseDto
import com.bookbook.domain.rent.service.BookTitleParsingService
import com.bookbook.domain.rent.service.ImageOcrService
import com.bookbook.domain.rent.service.OcrBookSearchService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@ExtendWith(MockitoExtension::class)
@DisplayName("ImageOcrController 유닛 테스트")
class ImageOcrControllerTest {

    @Mock
    private lateinit var imageOcrService: ImageOcrService

    @Mock
    private lateinit var bookTitleParsingService: BookTitleParsingService

    @Mock
    private lateinit var ocrBookSearchService: OcrBookSearchService

    @InjectMocks
    private lateinit var imageOcrController: ImageOcrController

    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(imageOcrController).build()
    }

    @Test
    @DisplayName("OCR 도서 검색 성공 - 검색 결과 있음")
    fun ocrBookSearch_Success_WithResults() {
        // ===== GIVEN: 테스트 데이터 및 Mock 설정 =====
        
        // 1. Mock 이미지 파일 생성 (파일 업로드 처리 검증용)
        val mockFile = MockMultipartFile(
            "file",                    // @RequestParam("file")과 매칭되는 파라미터명
            "test-book.jpg",          // 실제 파일명
            "image/jpeg",             // MIME 타입
            "test image content".toByteArray()  // 파일 내용 (실제로는 이미지 바이트)
        )

        // 2. Mock 서비스 응답 생성 (서비스 호출 검증용)
        val mockResponse = ImageOcrResponseDto(
            extractedText = "테스트 도서 - 테스트 저자 지음",  // OCR로 추출된 전체 텍스트
            detectedBookTitle = "테스트 도서",               // 파싱된 책 제목
            confidence = 0.95,                             // OCR 신뢰도 (95%)
            searchResults = listOf(                         // 알라딘 검색 결과 2개
                BookSearchResponseDto(
                    bookTitle = "테스트 도서",
                    author = "테스트 저자",
                    publisher = "테스트 출판사",
                    pubDate = "2024-01-01",
                    category = "소설",
                    bookDescription = "테스트 도서 설명입니다.",
                    coverImageUrl = "https://test.com/cover1.jpg"
                ),
                BookSearchResponseDto(
                    bookTitle = "테스트 도서 2",
                    author = "테스트 저자 2",
                    publisher = "테스트 출판사 2",
                    pubDate = "2024-01-02",
                    category = "소설",
                    bookDescription = "테스트 도서 2 설명입니다.",
                    coverImageUrl = "https://test.com/cover2.jpg"
                )
            )
        )

        // 3. Mock 서비스 동작 설정 (서비스 호출 검증용)
        // "ocrBookSearchService.processImageAndSearch(mockFile)가 호출되면 mockResponse를 반환해줘"
        `when`(ocrBookSearchService.processImageAndSearch(mockFile)).thenReturn(mockResponse)

        // ===== WHEN: 실제 HTTP 요청 시뮬레이션 =====
        
        // 4. MockMvc를 통한 HTTP 요청 실행 (컨트롤러 엔드포인트 검증용)
        val resultActions = mockMvc.perform(
            multipart("/api/v1/bookbook/ocr-book-search")  // POST /api/v1/bookbook/ocr-book-search
                .file(mockFile)                            // multipart 파일 첨부
                .contentType(MediaType.MULTIPART_FORM_DATA) // Content-Type: multipart/form-data
        )

        // ===== THEN: 응답 검증 =====
        
        resultActions
            .andDo(print())  // 테스트 실행 시 콘솔에 요청/응답 로그 출력
            
            // 5. HTTP 상태 코드 검증 (컨트롤러가 올바른 엔드포인트로 요청을 받음)
            .andExpect(status().isOk)  // HTTP 200 OK
            
            // 6. 응답 메시지 생성 로직 검증 (RsData 구조 검증)
            .andExpect(jsonPath("$.resultCode").value("200-1"))  // 성공 코드
            .andExpect(jsonPath("$.msg").value("책을 찾았습니다! '테스트 도서' 검색 결과 2건"))  // 성공 메시지
            
            // 7. JSON 응답 구조 검증 (데이터 구조 일치성 검증)
            .andExpect(jsonPath("$.data.detectedBookTitle").value("테스트 도서"))  // 감지된 제목
            .andExpect(jsonPath("$.data.searchResults").isArray())               // 검색 결과가 배열인지
            
            // 8. 검색 결과 데이터 검증 (데이터 포함 여부 및 개수 검증)
            .andExpect(jsonPath("$.data.searchResults.length()").value(2))      // 배열 길이가 2인지
    }

    @Test
    @DisplayName("OCR 도서 검색 성공 - 제목만 감지, 검색 결과 없음")
    fun ocrBookSearch_Success_TitleOnly() {
        // ===== GIVEN: 테스트 데이터 및 Mock 설정 =====
        
        // 1. Mock 이미지 파일 생성 (파일 업로드 처리 검증용)
        val mockFile = MockMultipartFile(
            "file",                    // @RequestParam("file")과 매칭되는 파라미터명
            "test-book.jpg",          // 실제 파일명
            "image/jpeg",             // MIME 타입
            "test image content".toByteArray()  // 파일 내용 (실제로는 이미지 바이트)
        )

        // 2. Mock 서비스 응답 생성 (서비스 호출 검증용)
        // 시나리오: OCR로 책 제목은 감지했지만 알라딘 검색 결과가 없는 경우
        val mockResponse = ImageOcrResponseDto(
            extractedText = "감지된 도서 - 저자 지음",  // OCR로 추출된 전체 텍스트
            detectedBookTitle = "감지된 도서",         // 파싱된 책 제목 (성공)
            confidence = 0.85,                        // OCR 신뢰도 (85%)
            searchResults = emptyList()               // 알라딘 검색 결과 없음 (빈 리스트)
        )

        // 3. Mock 서비스 동작 설정 (서비스 호출 검증용)
        // "ocrBookSearchService.processImageAndSearch(mockFile)가 호출되면 mockResponse를 반환해줘"
        `when`(ocrBookSearchService.processImageAndSearch(mockFile)).thenReturn(mockResponse)

        // ===== WHEN: 실제 HTTP 요청 시뮬레이션 =====
        
        // 4. MockMvc를 통한 HTTP 요청 실행 (컨트롤러 엔드포인트 검증용)
        val resultActions = mockMvc.perform(
            multipart("/api/v1/bookbook/ocr-book-search")  // POST /api/v1/bookbook/ocr-book-search
                .file(mockFile)                            // multipart 파일 첨부
                .contentType(MediaType.MULTIPART_FORM_DATA) // Content-Type: multipart/form-data
        )

        // ===== THEN: 응답 검증 =====
        
        resultActions
            .andDo(print())  // 테스트 실행 시 콘솔에 요청/응답 로그 출력
            
            // 5. HTTP 상태 코드 검증 (컨트롤러가 올바른 엔드포인트로 요청을 받음)
            .andExpect(status().isOk)  // HTTP 200 OK
            
            // 6. 응답 메시지 생성 로직 검증 (RsData 구조 및 메시지 로직 검증)
            .andExpect(jsonPath("$.resultCode").value("200-1"))  // 성공 코드
            .andExpect(jsonPath("$.msg").value("책 제목은 감지했지만 검색 결과가 없습니다: '감지된 도서'"))  // 부분 성공 메시지
            
            // 7. JSON 응답 구조 검증 (데이터 구조 일치성 검증)
            .andExpect(jsonPath("$.data.detectedBookTitle").value("감지된 도서"))  // 감지된 제목 확인
            
            // 8. 검색 결과 데이터 검증 (빈 결과 처리 검증)
            .andExpect(jsonPath("$.data.searchResults").isEmpty)  // 검색 결과가 빈 배열인지 확인
    }

    @Test
    @DisplayName("OCR 도서 검색 실패 - 제목 감지 실패")
    fun ocrBookSearch_Failure_NoTitleDetected() {
        // ===== GIVEN: 테스트 데이터 및 Mock 설정 =====
        
        // 1. Mock 이미지 파일 생성 (파일 업로드 처리 검증용)
        val mockFile = MockMultipartFile(
            "file",                    // @RequestParam("file")과 매칭되는 파라미터명
            "test-book.jpg",          // 실제 파일명
            "image/jpeg",             // MIME 타입
            "test image content".toByteArray()  // 파일 내용 (실제로는 이미지 바이트)
        )

        // 2. Mock 서비스 응답 생성 (서비스 호출 검증용)
        // 시나리오: OCR로 텍스트는 추출했지만 책 제목을 파싱하지 못한 경우
        val mockResponse = ImageOcrResponseDto(
            extractedText = "이미지에서 텍스트를 추출할 수 없습니다.",  // OCR로 추출된 텍스트 (의미 없는 텍스트)
            detectedBookTitle = null,                                // 파싱된 책 제목 (실패 - null)
            confidence = 0.0,                                        // OCR 신뢰도 (0% - 매우 낮음)
            searchResults = emptyList()                             // 알라딘 검색 결과 없음 (빈 리스트)
        )

        // 3. Mock 서비스 동작 설정 (서비스 호출 검증용)
        // "ocrBookSearchService.processImageAndSearch(mockFile)가 호출되면 mockResponse를 반환해줘"
        `when`(ocrBookSearchService.processImageAndSearch(mockFile)).thenReturn(mockResponse)

        // ===== WHEN: 실제 HTTP 요청 시뮬레이션 =====
        
        // 4. MockMvc를 통한 HTTP 요청 실행 (컨트롤러 엔드포인트 검증용)
        val resultActions = mockMvc.perform(
            multipart("/api/v1/bookbook/ocr-book-search")  // POST /api/v1/bookbook/ocr-book-search
                .file(mockFile)                            // multipart 파일 첨부
                .contentType(MediaType.MULTIPART_FORM_DATA) // Content-Type: multipart/form-data
        )

        // ===== THEN: 응답 검증 =====
        
        resultActions
            .andDo(print())  // 테스트 실행 시 콘솔에 요청/응답 로그 출력
            
            // 5. HTTP 상태 코드 검증 (컨트롤러가 올바른 엔드포인트로 요청을 받음)
            .andExpect(status().isOk)  // HTTP 200 OK
            
            // 6. 응답 메시지 생성 로직 검증 (RsData 구조 및 실패 메시지 로직 검증)
            .andExpect(jsonPath("$.resultCode").value("200-1"))  // 성공 코드 (HTTP는 성공이지만 비즈니스 로직은 실패)
            .andExpect(jsonPath("$.msg").value("책 제목을 감지하지 못했습니다. 수동으로 검색해주세요."))  // 실패 안내 메시지
            
            // 7. JSON 응답 구조 검증 (데이터 구조 일치성 및 null 처리 검증)
            .andExpect(jsonPath("$.data.detectedBookTitle").isEmpty)  // 감지된 제목이 null인지 확인
            
            // 8. 검색 결과 데이터 검증 (빈 결과 처리 검증)
            .andExpect(jsonPath("$.data.searchResults").isEmpty)  // 검색 결과가 빈 배열인지 확인
    }

    @Test
    @DisplayName("OCR 도서 검색 - 빈 파일 처리")
    fun ocrBookSearch_EmptyFile() {
        // ===== GIVEN: 테스트 데이터 및 Mock 설정 =====
        
        // 1. Mock 빈 이미지 파일 생성 (빈 파일 업로드 처리 검증용)
        val mockFile = MockMultipartFile(
            "file",                    // @RequestParam("file")과 매칭되는 파라미터명
            "empty.jpg",              // 실제 파일명
            "image/jpeg",             // MIME 타입
            ByteArray(0)              // 파일 내용 (빈 바이트 배열 - 0바이트)
        )

        // 2. Mock 서비스 응답 생성 (서비스 호출 검증용)
        // 시나리오: 빈 파일로 인해 OCR 처리가 불가능한 경우
        val mockResponse = ImageOcrResponseDto(
            extractedText = "",                                    // OCR로 추출된 텍스트 (빈 문자열)
            detectedBookTitle = null,                              // 파싱된 책 제목 (실패 - null)
            confidence = 0.0,                                      // OCR 신뢰도 (0% - 처리 불가)
            searchResults = emptyList()                           // 알라딘 검색 결과 없음 (빈 리스트)
        )

        // 3. Mock 서비스 동작 설정 (서비스 호출 검증용)
        // "ocrBookSearchService.processImageAndSearch(mockFile)가 호출되면 mockResponse를 반환해줘"
        `when`(ocrBookSearchService.processImageAndSearch(mockFile)).thenReturn(mockResponse)

        // ===== WHEN: 실제 HTTP 요청 시뮬레이션 =====
        
        // 4. MockMvc를 통한 HTTP 요청 실행 (컨트롤러 엔드포인트 검증용)
        val resultActions = mockMvc.perform(
            multipart("/api/v1/bookbook/ocr-book-search")  // POST /api/v1/bookbook/ocr-book-search
                .file(mockFile)                            // multipart 파일 첨부 (빈 파일)
                .contentType(MediaType.MULTIPART_FORM_DATA) // Content-Type: multipart/form-data
        )

        // ===== THEN: 응답 검증 =====
        
        resultActions
            .andDo(print())  // 테스트 실행 시 콘솔에 요청/응답 로그 출력
            
            // 5. HTTP 상태 코드 검증 (컨트롤러가 올바른 엔드포인트로 요청을 받음)
            .andExpect(status().isOk)  // HTTP 200 OK
            
            // 6. 응답 메시지 생성 로직 검증 (RsData 구조 및 빈 파일 처리 메시지 로직 검증)
            .andExpect(jsonPath("$.resultCode").value("200-1"))  // 성공 코드 (HTTP는 성공이지만 비즈니스 로직은 실패)
            .andExpect(jsonPath("$.msg").value("책 제목을 감지하지 못했습니다. 수동으로 검색해주세요."))  // 빈 파일 안내 메시지
    }

    @Test
    @DisplayName("OCR 도서 검색 - 다양한 이미지 형식 지원")
    fun ocrBookSearch_VariousImageFormats() {
        // ===== GIVEN: 테스트 데이터 및 Mock 설정 =====
        
        // 1. Mock PNG 이미지 파일 생성 (다양한 이미지 형식 지원 검증용)
        val mockPngFile = MockMultipartFile(
            "file",                    // @RequestParam("file")과 매칭되는 파라미터명
            "test-book.png",          // 실제 파일명 (PNG 확장자)
            "image/png",              // MIME 타입 (PNG 형식)
            "test png content".toByteArray()  // 파일 내용 (실제로는 PNG 이미지 바이트)
        )

        // 2. Mock 서비스 응답 생성 (서비스 호출 검증용)
        // 시나리오: PNG 형식 이미지로 OCR 처리가 성공하고 검색 결과가 있는 경우
        val mockResponse = ImageOcrResponseDto(
            extractedText = "PNG 이미지 도서 - PNG 저자 지음",  // OCR로 추출된 전체 텍스트
            detectedBookTitle = "PNG 이미지 도서",               // 파싱된 책 제목 (성공)
            confidence = 0.92,                                 // OCR 신뢰도 (92%)
            searchResults = listOf(                             // 알라딘 검색 결과 1개
                BookSearchResponseDto(
                    bookTitle = "PNG 이미지 도서",
                    author = "PNG 저자",
                    publisher = "PNG 출판사",
                    pubDate = "2024-01-03",
                    category = "소설",
                    bookDescription = "PNG 이미지 도서 설명입니다.",
                    coverImageUrl = "https://test.com/png-cover.jpg"
                )
            )
        )

        // 3. Mock 서비스 동작 설정 (서비스 호출 검증용)
        // "ocrBookSearchService.processImageAndSearch(mockPngFile)가 호출되면 mockResponse를 반환해줘"
        `when`(ocrBookSearchService.processImageAndSearch(mockPngFile)).thenReturn(mockResponse)

        // ===== WHEN: 실제 HTTP 요청 시뮬레이션 =====
        
        // 4. MockMvc를 통한 HTTP 요청 실행 (컨트롤러 엔드포인트 검증용)
        val resultActions = mockMvc.perform(
            multipart("/api/v1/bookbook/ocr-book-search")  // POST /api/v1/bookbook/ocr-book-search
                .file(mockPngFile)                         // multipart 파일 첨부 (PNG 형식)
                .contentType(MediaType.MULTIPART_FORM_DATA) // Content-Type: multipart/form-data
        )

        // ===== THEN: 응답 검증 =====
        
        resultActions
            .andDo(print())  // 테스트 실행 시 콘솔에 요청/응답 로그 출력
            
            // 5. HTTP 상태 코드 검증 (컨트롤러가 올바른 엔드포인트로 요청을 받음)
            .andExpect(status().isOk)  // HTTP 200 OK
            
            // 6. 응답 메시지 생성 로직 검증 (RsData 구조 및 성공 메시지 로직 검증)
            .andExpect(jsonPath("$.resultCode").value("200-1"))  // 성공 코드
            .andExpect(jsonPath("$.msg").value("책을 찾았습니다! 'PNG 이미지 도서' 검색 결과 1건"))  // 성공 메시지 (PNG 형식 지원 확인)
            
            // 7. JSON 응답 구조 검증 (데이터 구조 일치성 및 PNG 형식 처리 검증)
            .andExpect(jsonPath("$.data.detectedBookTitle").value("PNG 이미지 도서"))  // 감지된 제목 확인 (PNG 형식에서도 정상 작동)
    }
}
