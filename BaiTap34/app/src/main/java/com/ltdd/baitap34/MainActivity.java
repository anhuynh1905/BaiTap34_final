package com.ltdd.baitap34;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.ltdd.baitap34.databinding.ActivityMainBinding;

import java.io.File;
import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private ActivityMainBinding binding; // Sử dụng View Binding
    private Uri mUri; // Uri của ảnh được chọn
    private ProgressDialog progressDialog;

    // ActivityResultLauncher cho việc chọn ảnh
    private final ActivityResultLauncher<Intent> activityResultLauncherGallery =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Intent data = result.getData();
                    mUri = data.getData();
                    if (mUri != null) {
                        try {
                            // Hiển thị ảnh đã chọn lên imgChoose
                            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), mUri);
                            binding.imgChoose.setImageBitmap(bitmap);
                        } catch (IOException e) {
                            Log.e(TAG, "Error loading bitmap: ", e);
                            Toast.makeText(this, "Không thể tải ảnh", Toast.LENGTH_SHORT).show();
                        }
                    }
                } else {
                    Toast.makeText(this, "Chưa chọn ảnh", Toast.LENGTH_SHORT).show();
                }
            });

    // ActivityResultLauncher cho việc xin quyền
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    openGallery(); // Quyền đã được cấp, mở thư viện
                } else {
                    // Quyền bị từ chối
                    Toast.makeText(this, "Bạn cần cấp quyền truy cập bộ nhớ để chọn ảnh", Toast.LENGTH_LONG).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater()); // Khởi tạo View Binding
        setContentView(binding.getRoot()); // Sử dụng root view từ binding

        setupProgressDialog();
        setupButtonClickListeners();
    }

    private void setupProgressDialog() {
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Đang tải lên, vui lòng chờ...");
        progressDialog.setCancelable(false); // Không cho hủy ngang
    }

    private void setupButtonClickListeners() {
        // Nút chọn ảnh
        binding.btnChoose.setOnClickListener(v -> checkPermissionsAndOpenGallery());

        // Nút Upload
        binding.btnUpload.setOnClickListener(v -> {
            String username = binding.editUserName.getText().toString().trim();
            if (mUri == null) {
                Toast.makeText(this, "Vui lòng chọn ảnh trước", Toast.LENGTH_SHORT).show();
            } else if (username.isEmpty()) {
                binding.editUserName.setError("Vui lòng nhập username");
                Toast.makeText(this, "Vui lòng nhập username", Toast.LENGTH_SHORT).show();
            } else {
                uploadProfileImage(username);
            }
        });
    }

    // Bước 1: Kiểm tra quyền trước khi mở Gallery
    private void checkPermissionsAndOpenGallery() {
        String permission;
        // Xác định quyền cần xin dựa trên phiên bản Android
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13+
            permission = Manifest.permission.READ_MEDIA_IMAGES;
        } else { // Dưới Android 13
            permission = Manifest.permission.READ_EXTERNAL_STORAGE;
        }

        // Kiểm tra xem quyền đã được cấp chưa
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            // Quyền đã được cấp -> Mở thư viện
            openGallery();
        } else {
            // Quyền chưa được cấp -> Yêu cầu quyền
            requestPermissionLauncher.launch(permission);
        }
    }

    // Bước 2: Mở thư viện ảnh
    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*"); // Chỉ chọn ảnh
        activityResultLauncherGallery.launch(intent);
    }

    // Bước 3: Upload ảnh và username
    private void uploadProfileImage(String username) {
        progressDialog.show(); // Hiển thị dialog chờ

        // Lấy đường dẫn thực của file từ Uri sử dụng RealPathUtil
        String imagePath = RealPathUtil.getRealPath(this, mUri);
        if (imagePath == null) {
            Log.e(TAG, "Could not get real path for URI: " + mUri);
            Toast.makeText(this, "Không thể lấy đường dẫn ảnh", Toast.LENGTH_SHORT).show();
            progressDialog.dismiss();
            return;
        }

        File file = new File(imagePath);

        // Tạo RequestBody cho username (kiểu text)
        RequestBody requestUsername = RequestBody.create(MediaType.parse("multipart/form-data"), username);

        // Tạo RequestBody cho file ảnh
        RequestBody requestFile = RequestBody.create(MediaType.parse("image/*"), file); // Hoặc MediaType.parse(getContentResolver().getType(mUri))

        // Tạo MultipartBody.Part cho file ảnh
        // Chú ý: Key "avatar" phải khớp với KEY_AVATAR trong Const và tên mà server PHP mong đợi ($_FILES['avatar'])
        MultipartBody.Part body = MultipartBody.Part.createFormData(Const.KEY_AVATAR, file.getName(), requestFile);

        // Gọi API sử dụng Retrofit
        ApiService apiService = ApiClient.getApiService();
        Call<UploadResponse> call = apiService.uploadProfile(requestUsername, body);

        call.enqueue(new Callback<UploadResponse>() {
            @Override
            public void onResponse(Call<UploadResponse> call, Response<UploadResponse> response) {
                progressDialog.dismiss(); // Ẩn dialog
                if (response.isSuccessful() && response.body() != null) {
                    UploadResponse uploadResponse = response.body();
                    if (uploadResponse.isSuccess()) {
                        Toast.makeText(MainActivity.this, "Upload thành công: " + uploadResponse.getMessage(), Toast.LENGTH_LONG).show();

                        // Cập nhật UI với dữ liệu trả về
                        binding.tvUsername.setText("Username: " + username); // Hiển thị username đã gửi đi

                        // Tải ảnh đã upload bằng Glide vào imgMultipart
                        String imageUrl = uploadResponse.getImageUrl();
                        if (imageUrl != null && !imageUrl.isEmpty()) {
                            // Quan trọng: Nếu URL trả về là tương đối (vd: "uploads/image.jpg"), cần nối với BASE_URL
                            String fullImageUrl = imageUrl.startsWith("http") ? imageUrl : Const.BASE_URL + imageUrl;

                            Log.d(TAG, "Loading image from URL: " + fullImageUrl); // Log URL đầy đủ

                            Glide.with(MainActivity.this)
                                    .load(fullImageUrl)
                                    .placeholder(R.drawable.ic_launcher_background) // Ảnh chờ tải
                                    .error(android.R.drawable.stat_notify_error) // Ảnh khi lỗi
                                    .into(binding.imgMultipart);
                        } else {
                            Log.w(TAG,"Image URL from server is null or empty.");
                            binding.imgMultipart.setImageResource(android.R.drawable.ic_menu_report_image); // Ảnh mặc định nếu URL rỗng
                        }

                    } else {
                        // Server trả về success = false
                        Toast.makeText(MainActivity.this, "Upload thất bại: " + uploadResponse.getMessage(), Toast.LENGTH_LONG).show();
                        Log.e(TAG,"Upload failed on server: " + uploadResponse.getMessage());
                    }
                } else {
                    // Lỗi HTTP (vd: 404, 500) hoặc response.body() là null
                    String errorBody = "Unknown error";
                    try {
                        if (response.errorBody() != null) errorBody = response.errorBody().string();
                    } catch (IOException e) {
                        Log.e(TAG,"Error reading error body", e);
                    }
                    Toast.makeText(MainActivity.this, "Lỗi Upload: " + response.code() + " - " + errorBody, Toast.LENGTH_LONG).show();
                    Log.e(TAG,"Upload error: " + response.code() + " | Response: " + errorBody);
                }
            }

            @Override
            public void onFailure(Call<UploadResponse> call, Throwable t) {
                progressDialog.dismiss(); // Ẩn dialog
                Toast.makeText(MainActivity.this, "Lỗi mạng hoặc kết nối: " + t.getMessage(), Toast.LENGTH_LONG).show();
                Log.e(TAG, "API Call failed: ", t);
            }
        });
    }
}