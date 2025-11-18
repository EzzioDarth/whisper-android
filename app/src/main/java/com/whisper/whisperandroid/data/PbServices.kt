package com.whisper.whisperandroid.data

import com.google.api.HttpBody
import com.google.api.Page
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.GET
import retrofit2.http.Query
import kotlin.jvm.JvmSuppressWildcards
import retrofit2.http.DELETE


interface PbServices {

    // PocketBase login endpoint
    @POST("/api/collections/users/auth-with-password")
    suspend fun auth(@Body body: PbAuthReq): PbAuthResp
        // 🔹 Register a new user
    @POST("/api/collections/users/records")
    suspend fun registerUser(
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): PbUser

	
    // Update user info
    @PATCH("/api/collections/users/records/{id}")
    suspend fun updateUser(
        @Path("id") id: String,
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): PbUser

    // Register a device token (example)
    @POST("/api/collections/device_tokens/records")
    suspend fun addDeviceToken(
        @Header("Authorization") bearer: String,
        @Body body: Map<String, String>
    ): PbRecordId
    @GET("/api/collections/users/records")
    suspend fun listUsers(
        @Header("Authorization") bearer: String,
        @Query("page") page: Int = 1,
        @Query("perPage") perPage: Int = 50,
        @Query("filter") filter: String? = null
    ): PbListResp<PbUser>
    //rooms
    @GET("/api/collections/chat_rooms/records")
    suspend fun listRooms(
        @Header("Authorization") bearer: String,
        @Query("filter") filter: String
    ): PbListResp<PbRoom>
    @POST("/api/collections/chat_rooms/records")
    suspend fun createRoom(
        @Header("Authorization") bearer: String,
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): PbRoom
    //messages
    @GET("/api/collections/messages/records")
    suspend fun listMessages(
        @Header("Authorization") bearer: String,
        @Query("filter") filter: String,
        @Query("sort") sort: String = "created",
        @Query("page") page: Int = 1,
    	@Query("perPage") perPage: Int = 200
    ): PbListResp<PbMessage>
    @POST("/api/collections/messages/records")
    suspend fun sendMessage(
        @Header("Authorization") bearer: String,
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): PbMessage
    //delete messages 
	@DELETE("/api/collections/messages/records/{id}")
    suspend fun deleteMessage(
        @Header("Authorization") bearer: String,
        @Path("id") id: String
    )


}
