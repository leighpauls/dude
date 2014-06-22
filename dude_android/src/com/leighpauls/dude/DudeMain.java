package com.leighpauls.dude;

import android.app.Activity;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class DudeMain extends Activity {

    private static final String UPLOAD_URI = "http://192.168.0.109:8080/send_dude";
    private final String OUTPUT_FILE = Environment.getExternalStorageDirectory().getAbsolutePath()
            + "/dude_audio.amr-wb";
    private boolean mRecording = false;
    private MediaRecorder mMediaRecorder = null;
    private Button mStartRecordingButton;
    private Button mStopRecordingButton;

    private void startRecording() {
        if (mRecording) {
            Toast.makeText(this, "Already recording", Toast.LENGTH_SHORT).show();
            return;
        }
        mRecording = true;
        mStartRecordingButton.setEnabled(false);

        mMediaRecorder = new MediaRecorder();
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.AMR_WB);
        mMediaRecorder.setOutputFile(OUTPUT_FILE);
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_WB);

        try {
            mMediaRecorder.prepare();
        } catch (IOException e) {
            Toast.makeText(DudeMain.this, "Failed to open temporary file", Toast.LENGTH_SHORT).show();
            cleanUpRecording();
            return;
        }

        mMediaRecorder.start();

        mStopRecordingButton.setEnabled(true);
        Toast.makeText(this, "Recording", Toast.LENGTH_SHORT).show();

    }

    private void stopRecording() {
        if (!mRecording) {
            Toast.makeText(this, "not currently recording", Toast.LENGTH_SHORT).show();
            return;
        }
        mStopRecordingButton.setEnabled(false);
        mMediaRecorder.stop();

        // play the recorded audio
        final MediaPlayer mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                mediaPlayer.release();
            }
        });
        try {
            mediaPlayer.setDataSource(OUTPUT_FILE);
            mediaPlayer.prepare();
            mediaPlayer.start();
        } catch (IOException e) {
            Toast.makeText(this, "Unable to play media", Toast.LENGTH_SHORT).show();
            mediaPlayer.release();
        }

        Toast.makeText(this, "Done recording... uploading", Toast.LENGTH_SHORT).show();

        // upload the sound file
        AsyncTask<Void, Void, String> uploadTask = new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {
                try {
                    File file = new File(OUTPUT_FILE);
                    HttpClient httpClient = new DefaultHttpClient();
                    HttpPost httpPost = new HttpPost(UPLOAD_URI);

                    MultipartEntity multipartEntity = new MultipartEntity(
                            HttpMultipartMode.BROWSER_COMPATIBLE);
                    multipartEntity.addPart("dude_sound", new FileBody(file));
                    httpPost.setEntity(multipartEntity);

                    HttpResponse response = httpClient.execute(httpPost);
                    return "Server Response: " + response.getStatusLine();

                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    return "Couldn't find file to upload";

                } catch (ClientProtocolException e) {
                    e.printStackTrace();
                    return "upload protocol error";
                } catch (IOException e) {
                    e.printStackTrace();
                    return "upload failed";
                }
            }

            @Override
            protected void onPostExecute(String message) {
                Toast.makeText(DudeMain.this, message, Toast.LENGTH_SHORT).show();
            }
        };
        uploadTask.execute();

        cleanUpRecording();
    }

    private void cleanUpRecording() {
        if (mMediaRecorder != null) {
            mMediaRecorder.release();
            mMediaRecorder = null;
        }

        mRecording = false;
        mStartRecordingButton.setEnabled(true);
        mStopRecordingButton.setEnabled(false);
        Toast.makeText(this, "Cleaned up", Toast.LENGTH_SHORT).show();
    }

    /** Starts recording audio */
    private View.OnClickListener mStartClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            startRecording();
        }
    };

    /** Stops recording audio and processes it */
    private View.OnClickListener mStopClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            stopRecording();
        }
    };

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        mStartRecordingButton = (Button) findViewById(R.id.start_recording_btn);
        mStartRecordingButton.setOnClickListener(mStartClickListener);

        mStopRecordingButton = (Button) findViewById(R.id.stop_recording_btn);
        mStopRecordingButton.setOnClickListener(mStopClickListener);
    }

}
