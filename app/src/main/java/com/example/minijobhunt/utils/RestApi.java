package com.example.minijobhunt.utils;

import java.util.List;
import java.util.Map;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface RestApi {

    @POST("register")
    Call<ResponseBody> register(@Body Map<String, String> body);

    @POST("login")
    Call<ResponseBody> login(@Body Map<String, String> body);

    @POST("logout")
    Call<ResponseBody> logout(@Header("Authorization") String token);

    @POST("save-fcm-token")
    Call<ResponseBody> saveFcmToken(
            @Header("Authorization") String token,
            @Body Map<String, String> body
    );

    // ================= PROFILE =================
    @POST("freelancer-profiles")
    Call<ResponseBody> createProfile(
            @Header("Authorization") String token,
            @Body Map<String, Object> body
    );

    @PUT("freelancer-profiles/{id}")
    Call<ResponseBody> updateProfile(
            @Header("Authorization") String token,
            @Path("id") int id,
            @Body Map<String, Object> body
    );

    @GET("my-profile")
    Call<ResponseBody> getMyProfile(@Header("Authorization") String token);

    @GET("freelancer-profiles/{id}")
    Call<ResponseBody> getFreelancerProfile(
            @Header("Authorization") String token,
            @Path("id") int id
    );

    // ================= VERIFICATION =================

    @Multipart
    @POST("verifications")
    Call<ResponseBody> submitVerification(
            @Header("Authorization") String token,
            @Part("user_id") RequestBody userId,
            @Part("full_name") RequestBody fullName,
            @Part MultipartBody.Part citizenship_front,
            @Part MultipartBody.Part citizenship_back,
            @Part MultipartBody.Part pan_card
    );

    @GET("verifications/{id}")
    Call<ResponseBody> getVerification(
            @Header("Authorization") String token,
            @Path("id") int id
    );

    @Multipart
    @POST("verifications/{id}")
    Call<ResponseBody> updateVerification(
            @Header("Authorization") String token,
            @Path("id") int id,
            @Part("_method") RequestBody method,
            @Part("full_name") RequestBody fullName,
            @Part MultipartBody.Part citizenship_front,
            @Part MultipartBody.Part citizenship_back,
            @Part MultipartBody.Part pan_card
    );

    // ================= TASK CATEGORIES =================

    @GET("task-categories")
    Call<ResponseBody> getCategories(@Header("Authorization") String token);

    // ================= TASKS =================

    @GET("tasks")
    Call<ResponseBody> getTasks(
            @Header("Authorization") String token,
            @Query("lat") Double lat,
            @Query("lng") Double lng,
            @Query("radius") Float radius
    );

    @POST("tasks")
    Call<ResponseBody> createTask(
            @Header("Authorization") String token,
            @Body Map<String, Object> body
    );

    @GET("tasks/{id}")
    Call<ResponseBody> getTask(
            @Header("Authorization") String token,
            @Path("id") int id
    );

    @PUT("tasks/{id}")
    Call<ResponseBody> updateTask(
            @Header("Authorization") String token,
            @Path("id") int id,
            @Body Map<String, Object> body
    );

    @DELETE("tasks/{id}")
    Call<ResponseBody> deleteTask(
            @Header("Authorization") String token,
            @Path("id") int id
    );

    // ================= PROPOSALS =================

    @GET("proposals")
    Call<ResponseBody> getProposals(@Header("Authorization") String token);

    @Multipart
    @POST("proposals")
    Call<ResponseBody> submitProposal(
            @Header("Authorization") String token,
            @Part("task_id") RequestBody taskId,
            @Part("user_id") RequestBody userId,
            @Part("description") RequestBody description,
            @Part("takes_time") RequestBody takesTime,
            @Part MultipartBody.Part achievement
    );

    @GET("proposals/{id}")
    Call<ResponseBody> getProposal(
            @Header("Authorization") String token,
            @Path("id") int id
    );

    @Multipart
    @POST("proposals/{id}")
    Call<ResponseBody> updateProposal(
            @Header("Authorization") String token,
            @Path("id") int id,
            @Part("_method") RequestBody method,
            @Part("description") RequestBody description,
            @Part("takes_time") RequestBody takesTime,
            @Part("status") RequestBody status,
            @Part MultipartBody.Part achievement
    );

    @DELETE("proposals/{id}")
    Call<ResponseBody> deleteProposal(
            @Header("Authorization") String token,
            @Path("id") int id
    );

    // ================= CONTRACTS =================

    @GET("contracts")
    Call<ResponseBody> getContracts(@Header("Authorization") String token);

    @POST("contracts")
    Call<ResponseBody> createContract(
            @Header("Authorization") String token,
            @Body Map<String, Object> body
    );

    @GET("contracts/{id}")
    Call<ResponseBody> getContract(
            @Header("Authorization") String token,
            @Path("id") int id
    );

    @Multipart
    @POST("contracts/{id}")
    Call<ResponseBody> updateContractMultipart(
            @Header("Authorization") String token,
            @Path("id") int id,
            @Part("_method") RequestBody method,
            @Part("status") RequestBody status,
            @Part MultipartBody.Part work_file
    );

    @PUT("contracts/{id}")
    Call<ResponseBody> updateContractStatus(
            @Header("Authorization") String token,
            @Path("id") int id,
            @Body Map<String, Object> body
    );

    // ================= CHATS =================

    @GET("chats")
    Call<ResponseBody> getChats(@Header("Authorization") String token);

    @POST("chats")
    Call<ResponseBody> createChat(
            @Header("Authorization") String token,
            @Body Map<String, Object> body
    );

    @GET("chats/{id}")
    Call<ResponseBody> getChat(
            @Header("Authorization") String token,
            @Path("id") int id
    );

    // ================= MESSAGES =================

    @GET("chats/{chatId}/messages")
    Call<ResponseBody> getChatMessages(
            @Header("Authorization") String token,
            @Path("chatId") int chatId
    );

    @POST("messages")
    Call<ResponseBody> sendMessage(
            @Header("Authorization") String token,
            @Body Map<String, Object> body
    );

    @PUT("chats/{id}/read")
    Call<ResponseBody> markChatAsRead(
            @Header("Authorization") String token,
            @Path("id") int chatId
    );

    // ================= SUBMISSIONS =================

    @GET("submissions")
    Call<ResponseBody> getSubmissions(@Header("Authorization") String token);

    @Multipart
    @POST("submissions")
    Call<ResponseBody> submitWork(
            @Header("Authorization") String token,
            @Part("contract_id") RequestBody contractId,
            @Part("freelancer_id") RequestBody freelancerId,
            @Part("message") RequestBody message,
            @Part List<MultipartBody.Part> attachments
    );

    @GET("submissions/{id}")
    Call<ResponseBody> getSubmission(
            @Header("Authorization") String token,
            @Path("id") int id
    );

    @PUT("submissions/{id}")
    Call<ResponseBody> updateSubmission(
            @Header("Authorization") String token,
            @Path("id") int id,
            @Body Map<String, Object> body
    );

    @DELETE("submissions/{id}")
    Call<ResponseBody> deleteSubmission(
            @Header("Authorization") String token,
            @Path("id") int id
    );

    // ================= PAYMENTS =================

    @GET("payments")
    Call<ResponseBody> getPayments(@Header("Authorization") String token);

    @POST("payments")
    Call<ResponseBody> storePayment(
            @Header("Authorization") String token,
            @Body Map<String, Object> body
    );

    @GET("payments/{id}")
    Call<ResponseBody> getPayment(
            @Header("Authorization") String token,
            @Path("id") int id
    );

    @PUT("payments/{id}")
    Call<ResponseBody> updatePayment(
            @Header("Authorization") String token,
            @Path("id") int id,
            @Body Map<String, Object> body
    );

    @DELETE("payments/{id}")
    Call<ResponseBody> deletePayment(
            @Header("Authorization") String token,
            @Path("id") int id
    );

    @PUT("payments/{id}/release")
    Call<ResponseBody> releasePayment(
            @Header("Authorization") String token,
            @Path("id") int id
    );

    @PUT("payments/{id}/refund")
    Call<ResponseBody> refundPayment(
            @Header("Authorization") String token,
            @Path("id") int id
    );

    // ================= REVIEWS =================

    @GET("reviews")
    Call<ResponseBody> getReviews(
            @Header("Authorization") String token,
            @Query("freelancer_id") Integer freelancerId
    );

    @POST("reviews")
    Call<ResponseBody> submitReview(
            @Header("Authorization") String token,
            @Body Map<String, Object> body
    );

    @GET("reviews/{id}")
    Call<ResponseBody> getReview(
            @Header("Authorization") String token,
            @Path("id") int id
    );

    @PUT("reviews/{id}")
    Call<ResponseBody> updateReview(
            @Header("Authorization") String token,
            @Path("id") int id,
            @Body Map<String, Object> body
    );

    @DELETE("reviews/{id}")
    Call<ResponseBody> deleteReview(
            @Header("Authorization") String token,
            @Path("id") int id
    );

    @GET("reviews")
    Call<ResponseBody> getFreelancerReviews(
            @Header("Authorization") String token,
            @Query("freelancer_id") int freelancerId
    );
    @GET("freelancer-profiles")
    Call<ResponseBody> getFreelancers(
            @Header("Authorization") String token,
            @Query("search") String search
    );

    // ================= CONFLICTS =================

    @GET("conflicts")
    Call<ResponseBody> getConflicts(@Header("Authorization") String token);

    @Multipart
    @POST("conflicts")
    Call<ResponseBody> raiseConflict(
            @Header("Authorization") String token,
            @Part("contract_id") RequestBody contractId,
            @Part("raised_by") RequestBody raisedBy,
            @Part("title") RequestBody title,
            @Part("reason") RequestBody reason,
            @Part MultipartBody.Part attachment
    );

    @GET("conflicts/{id}")
    Call<ResponseBody> getConflict(
            @Header("Authorization") String token,
            @Path("id") int id
    );

    @GET("conflicts/user/{userId}")
    Call<ResponseBody> getMyConflicts(
            @Header("Authorization") String token,
            @Path("userId") int userId
    );

    // ================= CONFLICT REPLIES =================

    @GET("conflict-replies/conflict/{conflictId}")
    Call<ResponseBody> getConflictReplies(
            @Header("Authorization") String token,
            @Path("conflictId") int conflictId
    );

    @Multipart
    @POST("conflict-replies")
    Call<ResponseBody> submitConflictReply(
            @Header("Authorization") String token,
            @Part("conflict_id") RequestBody conflictId,
            @Part("user_id") RequestBody userId,
            @Part("message") RequestBody message,
            @Part MultipartBody.Part attachment
    );
}
