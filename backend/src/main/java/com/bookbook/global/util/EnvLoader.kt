package com.bookbook.global.util

import io.github.cdimascio.dotenv.Dotenv

object EnvLoader {

    fun loadEnv() {
        Dotenv.configure()
            .ignoreIfMissing()
            .systemProperties() // 이 한 줄로 기존의 forEach 루프를 대체합니다.
            .load()
    }
}