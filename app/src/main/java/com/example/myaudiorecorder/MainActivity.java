package com.example.myaudiorecorder;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.ContextWrapper;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_AUDIO_PERMISSION_CODE = 101;
    private static final int REQUEST_INTERNET_PERMISSION_CODE = 1000;

    MediaRecorder mediaRecorder;
    MediaPlayer mediaPlayer;
    Button startBtn;
    Button stopBtn;
    boolean isRecording = false;
    boolean isPlaying = false;
    int seconds = 0;
    String path = null;
    int dummySeconds = 0;
    int playableSeconds = 0;
    String TAG = "MainActivity";

//    Usable to perform heavy tasks in background
    ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getSupportActionBar().hide();

        startBtn = findViewById(R.id.start_button);
        mediaPlayer = new MediaPlayer();

        startBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void  onClick(View view){
                if(checkRecordingPermission()){
                    if(!isRecording){
                        isRecording = true;
                        startBtn.setText("Stop");
                        executorService.execute(new Runnable() {
                            @Override
                            public void run() {
                                mediaRecorder = new MediaRecorder();
                                mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                                mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
                                mediaRecorder.setOutputFile(getRecordingFilePath());
                                path = getRecordingFilePath();
                                Log.d(TAG, "run: Output path is set at: "+getRecordingFilePath());
                                mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

                                try {
                                    mediaRecorder.prepare();
                                } catch (IOException e) {
                                    Log.d(TAG, "run: Some error in preparing media recorder");
                                    e.printStackTrace();
                                }

                                mediaRecorder.start();
                                        
                            }
                        });
                    }else{
                        startBtn.setText("Uploading...");
                        executorService.execute(new Runnable() {
                            @Override
                            public void run() {
                                mediaRecorder.stop();
                                mediaRecorder.release();
                                mediaRecorder = null;
                                playableSeconds = seconds;
                                dummySeconds = seconds;
                                seconds = 0;
                                isRecording = false;

                                doMultiPartRequest();
                            }
                        });
                        startBtn.setText("Start");

                    }
                }else{
                    requestRecordingPermission();
                }
            }
        });
    }

    private void doMultiPartRequest() {
        String path = getRecordingFilePath();
        Log.d(TAG, "doMultiPartRequest: file path is :"+ path);
        File audioFile = new File(path);
        if(audioFile.isFile()){
            Log.d(TAG, "doMultiPartRequest: Given file is a file.");
            doActualRequest(path);
        }
    }

    private void doActualRequest(String path) {
        OkHttpClient client = new OkHttpClient().newBuilder()
                .build();
        Log.d(TAG, "doActualRequest: Got client instance");
        MediaType mediaType = MediaType.parse("text/plain");
        RequestBody body = new MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("voice",path,
                        RequestBody.create(MediaType.parse("application/octet-stream"),
                                new File(path)))
                .addFormDataPart("viewer","1")
                .build();
        Log.d(TAG, "doActualRequest: Created request body");
        Request request = new Request.Builder()
                .url("https://1890-103-77-229-137.in.ngrok.io/voices/api/audio-recording/")
                .method("POST", body)
                .addHeader("Cookie", "csrftoken=QqqQk4DnIilXyxTtD7p2Hc1JL8jG9MCUdHjOjxdIlCWdrFykztRyDPuQ9z1jFCDG")
                .build();
        Log.d(TAG, "doActualRequest: Created actual request");
        try {
            Log.d(TAG, "doActualRequest: Sending response");
            Response response = client.newCall(request).execute();
            Log.d(TAG, "doActualRequest: RESPONSE:"+response.body().string());
        } catch (IOException e) {
            Log.e(TAG, "doActualRequest: ERROR:"+e.toString());
            e.printStackTrace();
        }
    }

    private String getRecordingFilePath() {
        ContextWrapper contextWrapper = new ContextWrapper(getApplicationContext());
        File music = contextWrapper.getExternalFilesDir(Environment.DIRECTORY_MUSIC);
        File file = new File(music, "testFile"+".mp3");
        return file.getPath();
    }

    private void requestRecordingPermission() {
        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_AUDIO_PERMISSION_CODE);
        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.INTERNET}, REQUEST_INTERNET_PERMISSION_CODE);
    }

    public boolean checkRecordingPermission() {
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_DENIED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET) == PackageManager.PERMISSION_DENIED ){
            requestRecordingPermission();
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == REQUEST_AUDIO_PERMISSION_CODE || requestCode == REQUEST_INTERNET_PERMISSION_CODE){
            if(grantResults.length>0){
                boolean permissionToRecord = grantResults[0] + grantResults[1] ==PackageManager.PERMISSION_GRANTED;
                if(permissionToRecord){
                    Toast.makeText(getApplicationContext(), "Permission Given", Toast.LENGTH_SHORT).show();
                }
                else{
                    Toast.makeText(getApplicationContext(), "Permission Denied", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
}