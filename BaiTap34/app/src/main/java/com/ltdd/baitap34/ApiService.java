package com.ltdd.baitap34;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface ApiService {

    @Multipart
    @POST("updateimages.php") // Endpoint mới
    Call<UploadResponse> uploadProfile(
            @Part(Const.KEY_USERNAME) RequestBody username, // Sử dụng KEY_USERNAME từ Const
            @Part MultipartBody.Part avatar // Sử dụng KEY_AVATAR từ Const khi tạo Part trong MainActivity
    );
}