package com.bookbook.domain.rent.service

import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*

// 25.08.29 현준
// 이미지 업로드 서비스
@Service
class ImageUploadService {

    companion object {
        private const val UPLOAD_DIR = "uploads"
    }

    @Throws(IOException::class)
    fun uploadImage(file: MultipartFile): String {

        // 파일명 중복 방지를 위해 UUID 사용
        val originalFilename = file.originalFilename // 원본 파일 이름 가져오기
        var fileExtension = ""
        if (originalFilename != null && originalFilename.contains(".")) { // 파일 이름에 확장자가 있는지 확인
            fileExtension = originalFilename.substring(originalFilename.lastIndexOf(".")) // 확장자 추출
        }
        val newFilename = UUID.randomUUID().toString() + fileExtension

        // 파일 저장 경로 설정
        val uploadPath = Paths.get(UPLOAD_DIR)
        if (!Files.exists(uploadPath)) { // 디렉토리가 존재하지 않으면 생성
            Files.createDirectories(uploadPath) // 디렉토리 생성
        }

        val filePath = uploadPath.resolve(newFilename) // 파일 경로 설정
        Files.copy(file.inputStream, filePath) // 파일 저장

        // 저장된 이미지의 URL 반환 - uploads 경로 사용
        return "/uploads/$newFilename"
    }
}
