package com.example.project8_2;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    private static final int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 1;
    private String siteUrl = "https://juyeop.pythonanywhere.com/api_root/Post/";
    private TextView textView;
    private RecyclerView recyclerView;
    private ImageAdapter imageAdapter;
    private List<ImageData> imageList = new ArrayList<>(); // ImageData 타입으로 수정
    private Uri selectedImageUri;
    private CloadImage taskDownload;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setTitle("이미지 뷰어");

        textView = findViewById(R.id.textView);
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        requestPermissionsIfNecessary();
    }

    private void requestPermissionsIfNecessary() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO
            }, MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
        } else {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE
            }, MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadImages();
            } else {
                Toast.makeText(this, "외부 저장소 권한이 필요합니다. 권한을 허용해주세요.", Toast.LENGTH_LONG).show();
            }
        }
    }

    // 서버에서 이미지 동기화
    private void loadImages() {
        if (taskDownload != null && taskDownload.getStatus() == AsyncTask.Status.RUNNING) {
            taskDownload.cancel(true);
        }
        taskDownload = new CloadImage();
        taskDownload.execute(siteUrl);
        Toast.makeText(getApplicationContext(), "Download 시작", Toast.LENGTH_LONG).show();
    }

    public void onClickDownload(View v) {
        loadImages();
    }

    // 이미지를 선택하고 사용자 입력 다이얼로그 표시
    private ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    showInputDialog(); // 사용자 입력을 받을 다이얼로그를 띄웁니다.
                }
            }
    );

    public void onClickNewPost(View view) {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }

    // 사용자 입력을 위한 다이얼로그
    private void showInputDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("게시물 정보 입력");

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_post_info, null);
        builder.setView(dialogView);

        final EditText titleInput = dialogView.findViewById(R.id.editTitle);
        final EditText textInput = dialogView.findViewById(R.id.editText);

        builder.setPositiveButton("게시", (dialog, which) -> {
            String title = titleInput.getText().toString();
            String text = textInput.getText().toString();
            uploadPostWithImage(title, text, selectedImageUri);
        });

        builder.setNegativeButton("취소", (dialog, which) -> dialog.dismiss());

        builder.show();
    }

    // 서버로 이미지와 텍스트 업로드
    private void uploadPostWithImage(String title, String text, Uri imageUri) {
        OkHttpClient client = new OkHttpClient();

        File imageFile = new File(getRealPathFromURI(imageUri));
        String currentDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(new Date());

        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("author", "1")
                .addFormDataPart("title", title)
                .addFormDataPart("text", text)
                .addFormDataPart("created_date", currentDate)
                .addFormDataPart("published_date", currentDate)
                .addFormDataPart("image", imageFile.getName(),
                        RequestBody.create(imageFile, MediaType.parse("image/jpeg")))
                .build();

        Request request = new Request.Builder()
                .url(siteUrl)
                .post(requestBody)
                .addHeader("Authorization", "Token 5f16ab618d07687de55f0df65fc5c9ff5838683e") // 본인의 토큰 사용
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "업로드 실패: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    Log.e("Upload Error", e.getMessage(), e);
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    runOnUiThread(() -> {
                        Toast.makeText(MainActivity.this, "게시물 업로드 성공!", Toast.LENGTH_LONG).show();
                        loadImages(); // 업로드 성공 시 즉시 동기화
                    });
                } else {
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "업로드 실패: " + response.code(), Toast.LENGTH_LONG).show());
                }
            }

        });
    }

    // URI로부터 실제 파일 경로 얻기
    private String getRealPathFromURI(Uri contentUri) {
        String result = null;
        Cursor cursor = getContentResolver().query(contentUri, null, null, null, null);
        if (cursor != null) {
            cursor.moveToFirst();
            int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
            if (idx != -1) {
                result = cursor.getString(idx);
            } else {
                Log.e("getRealPathFromURI", "Invalid column index for DATA");
            }
            cursor.close();
        } else {
            Log.e("getRealPathFromURI", "Cursor is null");
        }
        return result;
    }

    // 서버에서 이미지를 다운로드하여 표시
    private class CloadImage extends AsyncTask<String, Integer, List<ImageData>> {
        @Override
        protected List<ImageData> doInBackground(String... urls) {
            List<ImageData> imageDataList = new ArrayList<>();
            try {
                String apiUrl = urls[0];
                String token = "8128cfe837315ac5ef4fd866dfd93b47dd49912e"; // 토큰 필요 시 수정
                URL urlAPI = new URL(apiUrl);
                HttpURLConnection conn = (HttpURLConnection) urlAPI.openConnection();
                conn.setRequestProperty("Authorization", "Token " + token);
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(3000);
                conn.setReadTimeout(3000);

                if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    InputStream is = conn.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                    StringBuilder result = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        result.append(line);
                    }
                    is.close();

                    JSONArray aryJson = new JSONArray(result.toString());
                    for (int i = 0; i < aryJson.length(); i++) {
                        JSONObject postJson = aryJson.getJSONObject(i);
                        int id = postJson.getInt("id");  // 게시물 ID 파싱
                        String title = postJson.getString("title");
                        String text = postJson.getString("text");
                        String imageUrl = postJson.getString("image");

                        if (!imageUrl.isEmpty()) {
                            URL myImageUrl = new URL(imageUrl);
                            HttpURLConnection imageConn = (HttpURLConnection) myImageUrl.openConnection();
                            InputStream imgStream = imageConn.getInputStream();
                            Bitmap imageBitmap = BitmapFactory.decodeStream(imgStream);
                            imageDataList.add(new ImageData(id, title, text, imageBitmap));  // ID 포함하여 생성
                            imgStream.close();
                        }
                    }
                }
            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }
            return imageDataList;
        }


        @Override
        protected void onPostExecute(List<ImageData> images) {
            if (images.isEmpty()) {
                textView.setText("불러올 이미지가 없습니다.");
            } else {
                textView.setText("이미지 로드 성공!");
                imageList.clear();
                imageList.addAll(images);
                imageAdapter = new ImageAdapter(imageList, MainActivity.this);
                recyclerView.setAdapter(imageAdapter);
            }
        }
    }

    // 이미지를 저장하는 메서드 추가
    public void saveImageToDevice(Bitmap bitmap, String title) {
        File directory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "MyAppImages");
        if (!directory.exists()) {
            directory.mkdirs();
        }
        String fileName = title + "_" + System.currentTimeMillis() + ".jpg";
        File file = new File(directory, fileName);

        try (FileOutputStream out = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            Toast.makeText(this, "이미지 저장 성공: " + file.getAbsolutePath(), Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(this, "이미지 저장 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    public void deletePost(int postId, int position) {
        OkHttpClient client = new OkHttpClient();
        String deleteUrl = siteUrl + postId + "/"; // 삭제할 게시물의 URL 생성

        Request request = new Request.Builder()
                .url(deleteUrl)
                .delete()
                .addHeader("Authorization", "Token 5f16ab618d07687de55f0df65fc5c9ff5838683e") // 본인의 토큰 사용
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "삭제 실패: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    Log.e("Delete Error", e.getMessage(), e);
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    runOnUiThread(() -> {
                        Toast.makeText(MainActivity.this, "삭제 성공!", Toast.LENGTH_LONG).show();
                        imageAdapter.removeItem(position); // RecyclerView에서 아이템 삭제
                    });
                } else {
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "삭제 실패: " + response.code(), Toast.LENGTH_LONG).show());
                }
            }
        });
    }
}
