package com.projects.ca_cse227;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.res.ResourcesCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    MediaPlayer mediaPlayer = new MediaPlayer();
    String URL = "https://firebasestorage.googleapis.com/v0/b/mangarx-68c33.appspot.com/o/jack_sparrow__remix_.mp3?alt=media&token=e3570a1d-838b-4969-ba1d-be3d2d2d453c";
    TextView textView;
    private ProgressBar progressBar;
    private ImageButton imageButton;

    @Override
    protected void onPause() {
        super.onPause();
        mediaPlayer.release();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        progressBar = findViewById(R.id.progressBar);
        imageButton = findViewById(R.id.playPause);
        textView = findViewById(R.id.title);
        progressBar.setVisibility(View.GONE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mediaPlayer.setAudioAttributes(
                    new AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build()
            );
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            imageButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (mediaPlayer.isPlaying() && mediaPlayer != null) {
                        imageButton.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_baseline_play_circle_outline_24, getTheme()));
                        mediaPlayer.stop();
                        mediaPlayer.reset();
                    } else {
                        if (isNetworkAvailable()) {
                            Log.d(TAG, "onClick: Network " + isNetworkAvailable());
                            downloadFile(URL);
                        } else {
                            Toast.makeText(MainActivity.this, "No Internet Connection", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            });
        }

    }


    private void downloadFile(String url) {
        Log.d(TAG, "downloadFile: " + url);

        new Media_Player().execute(url);

    }

    private boolean isNetworkAvailable() {
        boolean available = false;
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isAvailable())
            available = true;
        return available;
    }

    @Override
    protected void onStart() {
        super.onStart();
        //checking the write permission in the app
        checkWritePermission();
    }


    //Function to check the write permission

    void checkWritePermission() {
        if ((ActivityCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                PackageManager.PERMISSION_GRANTED)) {
            // permission already granted
            boolean flag = true;
        } else {
            // requesting permission using request code 1
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // if the request code matches
        if (requestCode == 1) {
            // checking permissions array
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // if denied calling that function again
                checkWritePermission();
            } else {
                // if the permission denied
                Toast.makeText(this, "Write permission denied by user ", Toast.LENGTH_LONG).show();
            }
        }
    }


    private class Media_Player extends AsyncTask<String, Void, Uri> {
        private static final String TAG = "Media_Player";

        @Override
        protected Uri doInBackground(String... strings) {
            java.net.URL url = null;
            File file = null;
            try {
                url = new URL(strings[0]);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
            try {
                assert url != null;
                HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
                httpURLConnection.setReadTimeout(5000);
                httpURLConnection.setRequestMethod("GET");
                httpURLConnection.setDoInput(true);
                httpURLConnection.connect();
                int code = httpURLConnection.getResponseCode();
                Log.d(TAG, "doInBackground: " + httpURLConnection.getResponseMessage());
                if (code == HttpURLConnection.HTTP_OK) {
                    Log.d(TAG, "doInBackground: " + httpURLConnection.getContentType());
                    File sdcard = Environment.getExternalStorageDirectory();
                    file = new File(sdcard, "music.mp3");
                    Log.d(TAG, "doInBackground: " + httpURLConnection.getContentLength());

                    FileOutputStream fileOutput = new FileOutputStream(file);
                    InputStream inputStream = httpURLConnection.getInputStream();

                    byte[] buffer = new byte[481208];
                    int bufferLength;

                    while ((bufferLength = inputStream.read(buffer)) > 0) {
                        fileOutput.write(buffer, 0, bufferLength);
                    }

                    fileOutput.close();
                    inputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            return Uri.fromFile(file);

        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            textView.setText(R.string.buffering);
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected void onPostExecute(Uri uri) {
            super.onPostExecute(uri);
            progressBar.setVisibility(View.GONE);
            Log.d(TAG, "onPostExecute: " + uri);
            try {
                mediaPlayer.setDataSource(getApplicationContext(), uri);
            } catch (IOException e) {
                e.printStackTrace();
            }
            mediaPlayer.prepareAsync();
            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(final MediaPlayer mediaPlayer) {
                    textView.setText(R.string.done);
                    mediaPlayer.start();
                    imageButton.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_baseline_stop_24, getTheme()));
                }
            });
        }


    }
}