package com.Ben.closetstylist.network

import retrofit2.http.Body
import retrofit2.http.POST

interface ClaudeApiService {
    @POST("v1/messages")
    suspend fun createMessage(@Body request: ClaudeRequest): ClaudeResponse
}
