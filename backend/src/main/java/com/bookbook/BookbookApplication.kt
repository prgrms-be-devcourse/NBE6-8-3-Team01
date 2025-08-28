package com.bookbook

import io.github.cdimascio.dotenv.Dotenv
import io.github.cdimascio.dotenv.DotenvEntry
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
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
            .load()
            .entries()
            .forEach { entry: DotenvEntry ->
                System.setProperty(entry.key, entry.value)
            }
    } catch (e: Exception) {
        println(".env 파일 없음 - 기본 설정으로 실행")
    }
    SpringApplication.run(BookbookApplication::class.java, *args)
}
