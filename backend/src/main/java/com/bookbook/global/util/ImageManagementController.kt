package com.bookbook.global.util

import org.springframework.context.annotation.Profile
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.*

/**
 * 개발 환경에서만 사용할 수 있는 이미지 관리 API
 */
@RestController
@RequestMapping("/api/dev/images")
@Profile("dev") // 개발 환경에서만 활성화
class ImageManagementController {
    
    companion object {
        private val IMAGE_EXTENSIONS = listOf(".png", ".jpg", ".jpeg", ".gif")
    }
    
    /**
     * uploads 폴더의 모든 이미지 파일 목록 조회
     */
    @GetMapping("/list")
    fun listImages(): ResponseEntity<List<String>> {
        return try {
            val uploadsPath = Paths.get("uploads")

            if (!Files.exists(uploadsPath)) {
                return ResponseEntity.ok(emptyList())
            }

            val imageFiles = Files.list(uploadsPath)
                .filter { Files.isRegularFile(it) }
                .filter { path ->
                    val fileName = path.fileName.toString().lowercase(Locale.getDefault())
                    IMAGE_EXTENSIONS.any { fileName.endsWith(it) }
                }
                .map { it.fileName.toString() }
                .toList()

            ResponseEntity.ok(imageFiles)
        } catch (e: IOException) {
            ResponseEntity.internalServerError().build()
        }
    }

    /**
     * 특정 이미지 파일이 존재하는지 확인
     */
    @GetMapping("/exists/{fileName}")
    fun checkImageExists(@PathVariable fileName: String): ResponseEntity<Boolean> {
        val imagePath = Paths.get("uploads", fileName)
        val exists = Files.exists(imagePath)
        return ResponseEntity.ok(exists)
    }

    /**
     * 기존 이미지를 다른 이름으로 복사 (개발용)
     */
    @PostMapping("/copy")
    fun copyImage(
        @RequestParam sourceFileName: String,
        @RequestParam targetFileName: String
    ): ResponseEntity<String> {
        return try {
            val sourcePath = Paths.get("uploads", sourceFileName)
            val targetPath = Paths.get("uploads", targetFileName)

            if (!Files.exists(sourcePath)) {
                return ResponseEntity.badRequest().body("Source file not found: $sourceFileName")
            }

            Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING)

            ResponseEntity.ok("Image copied: $sourceFileName -> $targetFileName")
        } catch (e: IOException) {
            ResponseEntity.internalServerError().body("Copy failed: ${e.message}")
        }
    }

    /**
     * 누락된 이미지 파일들을 기본 이미지로 생성
     */
    @PostMapping("/create-missing")
    fun createMissingImages(@RequestBody missingFileNames: List<String>): ResponseEntity<String> {
        return try {
            val uploadsPath = Paths.get("uploads")

            // uploads 폴더가 없으면 생성
            if (!Files.exists(uploadsPath)) {
                Files.createDirectories(uploadsPath)
            }

            // 기존 이미지 파일 중 하나를 템플릿으로 사용
            val existingImages = Files.list(uploadsPath)
                .filter { Files.isRegularFile(it) }
                .filter { path ->
                    val fileName = path.fileName.toString().lowercase(Locale.getDefault())
                    IMAGE_EXTENSIONS.any { fileName.endsWith(it) }
                }
                .toList()

            if (existingImages.isEmpty()) {
                return ResponseEntity.badRequest().body("No existing images to use as template")
            }

            val templateImage = existingImages.first()
            var copiedCount = 0

            for (fileName in missingFileNames) {
                val targetPath = Paths.get("uploads", fileName)
                if (!Files.exists(targetPath)) {
                    Files.copy(templateImage, targetPath, StandardCopyOption.REPLACE_EXISTING)
                    copiedCount++
                }
            }

            ResponseEntity.ok("Created $copiedCount missing image files using template: ${templateImage.fileName}")
        } catch (e: IOException) {
            ResponseEntity.internalServerError().body("Failed to create missing images: ${e.message}")
        }
    }
}
