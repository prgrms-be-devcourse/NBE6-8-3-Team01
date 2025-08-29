package com.bookbook.global.rsdata

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * API 응답 공통 형식
 */
data class RsData<T>(
    @JsonProperty("resultCode")
    val resultCode: String,
    
    @JsonProperty("msg")
    val msg: String,
    
    @JsonProperty("data")
    val data: T? = null
) {
    @get:JsonProperty("statusCode")
    val statusCode: Int
        get() = resultCode.split("-", limit = 2)[0].toInt()

    @get:JsonProperty("success")
    val isSuccess: Boolean
        get() = resultCode.startsWith("200")

    val isFail: Boolean
        get() = !isSuccess

    companion object {
        @JvmStatic
        fun <T> of(resultCode: String, msg: String, data: T?): RsData<T?> = RsData(resultCode, msg, data)

        @JvmStatic
        fun <T> of(resultCode: String, msg: String): RsData<T?> = RsData(resultCode, msg, null)
    }
}