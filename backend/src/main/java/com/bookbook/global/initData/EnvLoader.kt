package com.bookbook.global.initData

import io.github.cdimascio.dotenv.Dotenv
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered.HIGHEST_PRECEDENCE
import org.springframework.core.annotation.Order

@Configuration
@Order(HIGHEST_PRECEDENCE)
class EnvLoader {
    
    companion object {
        init {
            Dotenv.configure()
                .ignoreIfMissing()
                .systemProperties()
                .load()
        }
    }
}