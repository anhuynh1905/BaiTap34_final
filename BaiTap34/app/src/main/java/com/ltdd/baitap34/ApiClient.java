package com.ltdd.baitap34;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {

    private static Retrofit retrofit = null;

    public static ApiService getApiService() {
        if (retrofit == null) {
            // Cho phép GSON xử lý null hoặc các định dạng JSON linh hoạt hơn
            Gson gson = new GsonBuilder()
                    .setLenient() // Giúp xử lý JSON không chuẩn một chút
                    .create();

            retrofit = new Retrofit.Builder()
                    .baseUrl(Const.BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .build();
        }
        return retrofit.create(ApiService.class);
    }
}
