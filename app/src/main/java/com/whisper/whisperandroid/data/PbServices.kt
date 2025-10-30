package com.whisper.whisperandroid.data

import com.google.api.Page
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.GET
import retrofit2.http.Query

interface PbServices {

    // PocketBase login endpoint
    @POST("/api/collections/users/auth-with-password")
    suspend fun auth(@Body body: PbAuthReq): PbAuthResp

    // Update user info
    @PATCH("/api/collections/users/records/{id}")
    suspend fun updateUser(
        @Path("id") id: String,
        @Body body: Map<String, Any>
    ): PbUser

    // Register a device token (example)
    @POST("/api/collections/device_tokens/records")
    suspend fun addDeviceToken(
        @Header("Authorization") bearer: String,
        @Body body: Map<String, String>
    ): PbRecordId
    @GET("/api/collections/users/records")
    suspend fun listUsers(
        @Header("Autorization") bearer: String,
        @Query("page") page: Int = 1,
        @Query("perPage") perPage: Int = 50,
        @Query("filter") filter: String? = null
    ): PbListResp<PbUser>

}
