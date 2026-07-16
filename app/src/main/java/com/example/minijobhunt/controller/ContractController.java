package com.example.minijobhunt.controller;

import com.example.minijobhunt.utils.App;

import java.util.Map;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;

public class ContractController {

    public Call<ResponseBody> createContract(String token, Map<String, Object> body) {
        return App.api.createContract(token, body);
    }

    public Call<ResponseBody> getContracts(String token) {
        return App.api.getContracts(token);
    }

    public Call<ResponseBody> getContract(String token, int id) {
        return App.api.getContract(token, id);
    }

    public Call<ResponseBody> updateContractMultipart(
            String token,
            int id,
            RequestBody method,
            RequestBody status,
            MultipartBody.Part workFile
    ) {
        return App.api.updateContractMultipart(token, id, method, status, workFile);
    }

    public Call<ResponseBody> updateContractStatus(String token, int id, Map<String, Object> body) {
        return App.api.updateContractStatus(token, id, body);
    }
}
