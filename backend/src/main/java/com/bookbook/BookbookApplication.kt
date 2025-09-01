package com.bookbook

import com.bookbook.global.util.EnvLoader
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.scheduling.annotation.EnableScheduling

@EnableJpaAuditing
@EnableScheduling
@SpringBootApplication
class BookbookApplication

fun main(args: Array<String>) {
    EnvLoader.loadEnv()
    runApplication<BookbookApplication>(*args)
}