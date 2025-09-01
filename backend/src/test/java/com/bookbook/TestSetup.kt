package com.bookbook

import com.bookbook.domain.suspend.entity.SuspendedUser
import com.bookbook.domain.suspend.repository.SuspendedUserRepository
import com.bookbook.domain.user.entity.User
import com.bookbook.domain.user.repository.UserRepository
import com.bookbook.global.util.EnvLoader
import jakarta.annotation.PostConstruct
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional

@Component
@ActiveProfiles("test")
class TestSetup (
    private val userRepository: UserRepository,
    private val suspendedUserRepository: SuspendedUserRepository
){

    @PostConstruct
    @Order(Ordered.HIGHEST_PRECEDENCE)
    private fun loadEnv(){
        EnvLoader.loadEnv()
    }

    @Transactional
    @PostConstruct
    fun createDummyUser() {
        for (i in 1..5) {
            val user = User(
                username = "user$i",
                password = "password-$i",
                nickname = "nickname-$i",
                email = "email_$i@test.com",
                address = "서울시",
                registrationCompleted = true
            )

            userRepository.save(user)
        }
    }

    @Transactional
    fun setSuspendedUsers() {
        val users = userRepository.findAll()
        val period = 7

        users.filter { !it.isAdmin && it.id > 3 }.forEach {
            it.suspend(period)

            val suspendedUser = SuspendedUser(
                user = it,
                reason = "그냥 유저 ${it.username}을 ${period}일 동안 정지함",
            )

            suspendedUserRepository.save(suspendedUser)
        }
    }
}