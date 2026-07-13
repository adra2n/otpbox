package com.otpbox.data.sync

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

@Serializable
data class GistFile(val content: String)

@Serializable
data class GistFileResponse(val content: String? = null, val filename: String? = null)

@Serializable
data class GistRequest(
    val description: String,
    @SerialName("public") val isPublic: Boolean,
    val files: Map<String, GistFile>
)

@Serializable
data class GistResponse(
    val id: String,
    val files: Map<String, GistFileResponse> = emptyMap()
)

interface GitHubApi {

    @GET("gists/{id}")
    suspend fun getGist(
        @Header("Authorization") auth: String,
        @Path("id") id: String
    ): GistResponse

    @POST("gists")
    suspend fun createGist(
        @Header("Authorization") auth: String,
        @Body body: GistRequest
    ): GistResponse

    @PATCH("gists/{id}")
    suspend fun updateGist(
        @Header("Authorization") auth: String,
        @Path("id") id: String,
        @Body body: GistRequest
    ): GistResponse

    companion object {
        const val BASE_URL = "https://api.github.com/"
        const val FILE_NAME = "otpbox-backup.json"
        const val DESCRIPTION = "OTPBox encrypted backup"
    }
}
