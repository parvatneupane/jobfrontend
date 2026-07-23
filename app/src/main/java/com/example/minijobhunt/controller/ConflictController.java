package com.example.minijobhunt.controller;

import com.example.minijobhunt.utils.App;

import java.io.File;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;

public class ConflictController {

    public Call<ResponseBody> getConflicts(String token) {
        return App.api.getConflicts(token);
    }

    public Call<ResponseBody> raiseConflict(
            String token,
            int contractId,
            int raisedBy,
            String title,
            String reason,
            File attachmentFile
    ) {
        RequestBody contractIdBody = RequestBody.create(MediaType.parse("text/plain"), String.valueOf(contractId));
        RequestBody raisedByBody = RequestBody.create(MediaType.parse("text/plain"), String.valueOf(raisedBy));
        RequestBody titleBody = RequestBody.create(MediaType.parse("text/plain"), title);
        RequestBody reasonBody = RequestBody.create(MediaType.parse("text/plain"), reason);

        MultipartBody.Part attachmentPart = null;
        if (attachmentFile != null) {
            RequestBody fileBody = RequestBody.create(MediaType.parse("application/octet-stream"), attachmentFile);
            attachmentPart = MultipartBody.Part.createFormData("attachment", attachmentFile.getName(), fileBody);
        }

        return App.api.raiseConflict(token, contractIdBody, raisedByBody, titleBody, reasonBody, attachmentPart);
    }

    public Call<ResponseBody> getConflict(String token, int id) {
        return App.api.getConflict(token, id);
    }

    public Call<ResponseBody> getMyConflicts(String token, int userId) {
        return App.api.getMyConflicts(token, userId);
    }

    // ================= REPLIES =================

    public Call<ResponseBody> getConflictReplies(String token, int conflictId) {
        return App.api.getConflictReplies(token, conflictId);
    }

    public Call<ResponseBody> submitReply(
            String token,
            int conflictId,
            int userId,
            String message,
            File attachmentFile
    ) {
        RequestBody conflictIdBody = RequestBody.create(MediaType.parse("text/plain"), String.valueOf(conflictId));
        RequestBody userIdBody = RequestBody.create(MediaType.parse("text/plain"), String.valueOf(userId));
        RequestBody messageBody = RequestBody.create(MediaType.parse("text/plain"), message);

        MultipartBody.Part attachmentPart = null;
        if (attachmentFile != null) {
            RequestBody fileBody = RequestBody.create(MediaType.parse("application/octet-stream"), attachmentFile);
            attachmentPart = MultipartBody.Part.createFormData("attachment", attachmentFile.getName(), fileBody);
        }

        return App.api.submitConflictReply(token, conflictIdBody, userIdBody, messageBody, attachmentPart);
    }
}
