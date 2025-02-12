package com.example.btvn3;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.pm.PackageManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "API_LOG";
    private static final String API_LAST_ID = "https://57kmt.duckdns.org/android/api.aspx?action=last_id";
    private static final String API_GET_ID = "https://57kmt.duckdns.org/android/api.aspx?action=get_id&id=";
    private static final int REQUEST_NOTIFICATION_PERMISSION = 1;

    private int lastIdStored = -1;
    private Handler handler;
    private RequestQueue requestQueue;
    private TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textView = findViewById(R.id.tvLastId);
        requestQueue = Volley.newRequestQueue(this);
        handler = new Handler();

        createNotificationChannel();

        // Kiểm tra quyền thông báo (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_NOTIFICATION_PERMISSION);
        }

        handler.postDelayed(runnable, 0);
    }

    private final Runnable runnable = new Runnable() {
        @Override
        public void run() {
            fetchLastId();
            handler.postDelayed(this, 30000);
        }
    };

    private void fetchLastId() {
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, API_LAST_ID, null,
                response -> {
                    try {
                        int newLastId = response.getInt("last_id");
                        Log.d(TAG, "Last ID mới: " + newLastId);

                        if (newLastId > lastIdStored) {
                            lastIdStored = newLastId;
                            fetchDataById(newLastId);
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Lỗi xử lý JSON: ", e);
                    }
                }, error -> Log.e(TAG, "Lỗi API (Last ID): " + error.toString()));

        requestQueue.add(request);
    }

    private void fetchDataById(int id) {
        String url = API_GET_ID + id;

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        String message = response.getString("msg");

                        // Cập nhật giao diện
                        runOnUiThread(() -> textView.setText("ID: " + id + "\nMessage: " + message));

                        // Hiển thị thông báo
                        showNotification("New Data", message);
                        playNotificationSound();
                    } catch (JSONException e) {
                        Log.e(TAG, "Lỗi xử lý JSON (Get ID): ", e);
                    }
                }, error -> Log.e(TAG, "Lỗi API (Get ID): " + error.toString()));

        requestQueue.add(request);
    }

    private void showNotification(String title, String message) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "API_CHANNEL")
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

        // Kiểm tra quyền thông báo trên Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Thông báo bị chặn do chưa cấp quyền!");
            return;
        }

        notificationManager.notify(1, builder.build());
    }

    private void playNotificationSound() {
        try {
            Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            Ringtone ringtone = RingtoneManager.getRingtone(getApplicationContext(), soundUri);
            if (ringtone != null) {
                ringtone.play();
            } else {
                Log.e(TAG, "Không thể phát âm thanh, ringtone null!");
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Lỗi quyền phát âm thanh!", e);
        } catch (Exception e) {
            Log.e(TAG, "Không thể phát âm thanh thông báo!", e);
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "API Notifications";
            String description = "Thông báo từ API";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel("API_CHANNEL", name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_NOTIFICATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Quyền thông báo đã được cấp!");
            } else {
                Toast.makeText(this, "Bạn chưa cấp quyền thông báo!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(runnable); // Ngăn memory leak khi ứng dụng đóng
    }
}
