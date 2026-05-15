package com.example.domain.member.dto.response

import com.fasterxml.jackson.annotation.JsonIgnore

@JvmRecord
data class TokenReissueResponse(
    val accessToken: String?,
    @get:JsonIgnore
    val refreshToken: String?,
) {
    companion object {
        @JvmStatic
        fun of(accessToken: String?, refreshToken: String?): TokenReissueResponse =
            TokenReissueResponse(accessToken, refreshToken)
    }
}
