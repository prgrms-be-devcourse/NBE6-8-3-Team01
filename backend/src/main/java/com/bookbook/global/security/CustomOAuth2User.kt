package com.bookbook.global.security

import com.bookbook.domain.user.enums.Role
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.oauth2.core.user.DefaultOAuth2User
import java.io.Serializable

class CustomOAuth2User(
    authorities: Collection<GrantedAuthority>,
    attributes: Map<String, Any>,
    nameAttributeKey: String,

     val username: String,
     val nickname: String?,
     val userId: Long,
     val isRegistrationCompleted: Boolean,
     val role: Role
) : DefaultOAuth2User(
    authorities,
    attributes,
    nameAttributeKey
), Serializable {
    override fun getName(): String {
        return this.username
    }
}