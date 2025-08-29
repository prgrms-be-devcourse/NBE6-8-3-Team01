package com.bookbook.global.security

import com.bookbook.domain.user.repository.UserRepository
import com.bookbook.domain.user.service.UserService
import org.slf4j.LoggerFactory
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Suppress("UNCHECKED_CAST")
@Service
class CustomOAuth2UserService(
    private val userRepository: UserRepository,
    private val userService: UserService
) : OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    companion object {
        private val log = LoggerFactory.getLogger(CustomOAuth2UserService::class.java)
    }

    @Transactional
    override fun loadUser(userRequest: OAuth2UserRequest): OAuth2User {
        val delegate: OAuth2UserService<OAuth2UserRequest, OAuth2User> = DefaultOAuth2UserService()
        val oAuth2User = delegate.loadUser(userRequest)

        val registrationId = userRequest.clientRegistration.registrationId
        val userNameAttributeName = userRequest.clientRegistration.providerDetails.userInfoEndpoint.userNameAttributeName

        val attributes = OAuth2UserAttributes.of(registrationId, userNameAttributeName, oAuth2User.attributes)

        val username = registrationId + "_" + attributes.id
        var isNewUser = false
        var isRegistrationCompleted: Boolean

        val existingUser = userRepository.findByUsername(username)
        if (existingUser.isEmpty) {
            isNewUser = true
        }

        val user = userService.findOrCreateUser(username, attributes.email, attributes.nickname)

        isRegistrationCompleted = user.isRegistrationCompleted()

        log.info("DEBUG: User processed. Username: {}, ID: {}, Nickname: {}, Email: {}, isNewUser: {}, isRegistrationCompleted: {}",
            user.username, user.id, user.nickname, user.email, isNewUser, isRegistrationCompleted)

        val authorities = listOf<GrantedAuthority>(SimpleGrantedAuthority(user.role.name))
        val userAttributes = oAuth2User.attributes

        return CustomOAuth2User(
            authorities,
            userAttributes,
            userNameAttributeName,
            user.username,
            user.nickname,
            user.id,
            user.isRegistrationCompleted(),
            user.role
        )
    }
}