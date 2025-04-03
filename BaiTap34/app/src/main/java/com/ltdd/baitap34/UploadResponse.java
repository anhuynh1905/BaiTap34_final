package com.ltdd.baitap34;

import com.google.gson.annotations.SerializedName;

public class UploadResponse {

    @SerializedName("success") // Phải khớp với key JSON trả về từ server
    private boolean success;

    @SerializedName("message") // Phải khớp với key JSON trả về từ server
    private String message;

    @SerializedName("url")     // Phải khớp với key JSON trả về từ server (URL ảnh đã upload)
    private String imageUrl;

    // Getters
    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public String getImageUrl() {
        return imageUrl;
    }
}