package com.whisper.whisperandroid.data


import retrofit2.http.*
import retrofit2.Call

interface PbService {
    @POST("/api/collections/users/auth-with-password")
    fun auth(@Body body: PbAuthReq): Call<PbAuthResp>

    @PATCH("/api/collections/users/records/{id}")
    fun updateUser(@Path("id") id: String, @Body body: Map<String, Any>): Call<PbUser>

    @POST("/api/collections/chat_rooms/records")
    fun createRoom(@Header("Authorization") bearer: String, @Body body: PbCreateRoomReq): Call<PbRecordId>

    @POST("/api/collections/chat_participants/records")
    fun addParticipant(@Header("Authorization") bearer: String, @Body body: PbParticipantReq): Call<PbRecordId>

    @POST("/api/collections/messages/records")
    fun sendMessage(@Header("Authorization") bearer: String, @Body body: PbMessageReq): Call<PbRecordId>

    @GET("/api/collections/messages/records")
    fun listMessages(
        @Header("Authorization") bearer: String,
        @Query("filter") filter: String,
        @Query("sort") sort: String = "created"
    ): Call<PbListResp<PbMessage>>
}
