package com.bookbook.global.util

import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.*

/**
 * 이미지를 적절한 위치로 복사하는 유틸리티
 */
object ImageMigrationUtil {
    
    private val IMAGE_EXTENSIONS = listOf(".png", ".jpg", ".jpeg", ".gif")
    
    fun copyFromStaticToUploads() {
        val sourcePath = Paths.get("src/main/resources/static/images")
        val targetPath = Paths.get("uploads")

        try {
            // uploads 경로가 없으면 생성
            if (!Files.exists(targetPath)) {
                Files.createDirectories(targetPath)
                println("uploads 폴더 생성: $targetPath")
            }

            // 소스 폴더가 존재하는지 확인
            if (!Files.exists(sourcePath)) {
                println("소스 이미지 폴더가 존재하지 않습니다: $sourcePath")
                return
            }

            Files.list(sourcePath).use { files ->
                files.filter { file ->
                    val fileName = file.fileName.toString().lowercase(Locale.getDefault())
                    IMAGE_EXTENSIONS.any { fileName.endsWith(it) }
                }.forEach { file ->
                    try {
                        val targetFile = targetPath.resolve(file.fileName)
                        // 파일이 이미 존재하면 덮어쓰기
                        Files.copy(file, targetFile, StandardCopyOption.REPLACE_EXISTING)
                        println("이미지 복사 완료: ${file.fileName} -> uploads/")
                    } catch (e: IOException) {
                        System.err.println("파일 복사 실패: ${file.fileName} - ${e.message}")
                    }
                }
            }
            println("이미지 복사 완료!")
        } catch (e: IOException) {
            System.err.println("복사 중 오류 발생: ${e.message}")
        }
    }

    // 테스트용 main 메소드
    fun main(args: Array<String>) {
        println("=== static/images에서 uploads로 이미지 복사 ===")
        copyFromStaticToUploads()
    }
}
