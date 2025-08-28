package com.bookbook

import io.github.cdimascio.dotenv.Dotenv
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.scheduling.annotation.EnableScheduling

@EnableJpaAuditing
@EnableScheduling
@SpringBootApplication
class BookbookApplication

fun main(args: Array<String>) {
    try {
        Dotenv.configure()
            .ignoreIfMissing()
            .systemProperties() // 이 한 줄로 기존의 forEach 루프를 대체합니다.
            .load()
    } catch (e: Exception) {
        println(".env 파일 없음 - 기본 설정으로 실행")
    }
    runApplication<BookbookApplication>(*args)
}