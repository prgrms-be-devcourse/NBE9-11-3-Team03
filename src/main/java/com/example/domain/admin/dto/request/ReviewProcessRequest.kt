package com.example.domain.admin.dto.request

@JvmRecord
data class ReviewProcessRequest(
    @JvmField val action: String //BLIND, DISMISS
)
