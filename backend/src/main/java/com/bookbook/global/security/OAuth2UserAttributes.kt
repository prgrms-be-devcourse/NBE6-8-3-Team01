package com.bookbook.global.security

data class OAuth2UserAttributes(
    val attributes: Map<String, Any>?,
    val nameAttributeKey: String,
    val nickname: String?,
    val email: String?,
    val id: String
) {
    companion object {
        fun of(registrationId: String, userNameAttributeName: String, attributes: Map<String, Any>): OAuth2UserAttributes {
            return when (registrationId) {
                "naver" -> ofNaver(userNameAttributeName, attributes)
                "kakao" -> ofKakao(userNameAttributeName, attributes)
                else -> ofGoogle(userNameAttributeName, attributes)
            }
        }

        private fun ofGoogle(userNameAttributeName: String, attributes: Map<String, Any>): OAuth2UserAttributes {
            return OAuth2UserAttributes(
                attributes = attributes,
                nameAttributeKey = userNameAttributeName,
                nickname = attributes["name"] as String?,
                email = attributes["email"] as String?,
                id = attributes[userNameAttributeName] as String
            )
        }

        @Suppress("UNCHECKED_CAST")
        private fun ofNaver(userNameAttributeName: String, attributes: Map<String, Any>): OAuth2UserAttributes {
            val response = attributes[userNameAttributeName] as? Map<String, Any> ?: emptyMap()
            return OAuth2UserAttributes(
                attributes = response,
                nameAttributeKey = "id", // 네이버에서 사용자를 식별하는 키는 'id'입니다.
                nickname = response["nickname"] as String?,
                email = response["email"] as String?,
                id = response["id"] as String
            )
        }

        @Suppress("UNCHECKED_CAST")
        private fun ofKakao(userNameAttributeName: String, attributes: Map<String, Any>): OAuth2UserAttributes {
            val kakaoAccount = attributes["kakao_account"] as? Map<String, Any>
            val profile = kakaoAccount?.get("profile") as? Map<String, Any> ?: emptyMap()

            val nickname = profile["nickname"] as String?
            val id = attributes["id"].toString()
            val email = kakaoAccount?.get("email") as String?

            return OAuth2UserAttributes(
                attributes = attributes,
                nameAttributeKey = userNameAttributeName,
                nickname = nickname,
                email = email,
                id = id
            )
        }
    }
}