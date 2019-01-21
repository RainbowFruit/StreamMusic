package com.example.myshh.whyandroidblocksserver;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.Socket;

public class MainActivity extends AppCompatActivity {

    //String host = "192.168.0.25";
    Socket socket;
    int port = 50005;
    byte[] packet = new byte[313];
    byte[] musicBuffer = new byte[312];
    BufferedInputStream in;
    boolean canRun = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        new Thread(() -> {
            AudioTrack audioTrack = createAudioTrack();
            while (true) {
                try {
                    while (canRun) {
                        while (in.read(packet) > 0) {
                            if (packet[0] == -128) {
                                System.arraycopy(packet, 1, musicBuffer, 0, packet.length - 1);
                                toSpeaker(musicBuffer, audioTrack);
                            } else {
                                System.out.println("Answer: " + packet[0]);
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    canRun = false;
                }
            }
        }).start();
    }

    public void btnConnectClick(View v){
        new Thread(new Runnable() {
            @Override
            public void run() {
                EditText et1 = findViewById(R.id.et1);
                try {
                    System.out.println("Before connection");
                    socket = new Socket(et1.getText().toString(), port);
                    System.out.println("After connection");
                    in = new BufferedInputStream(socket.getInputStream());
                    canRun = true;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public void btnNextClick(View v){
        new Thread(() -> {
            try {
                socket.getOutputStream().write(10);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void btnPrevClick(View v){
        new Thread(() -> {
            try {
                socket.getOutputStream().write(20);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void btngotoMusic(View v) throws Exception {
        EditText editText2 = findViewById(R.id.editText2);
        byte[] buffer = new byte[2];
        buffer[0] = 30;
        buffer[1] = (byte)Integer.parseInt(editText2.getText().toString());
        new Thread(() -> {
            try {
                socket.getOutputStream().write(buffer);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void btnRemove(View v) throws Exception {
        EditText editText2 = findViewById(R.id.editText2);
        byte[] buffer = new byte[2];
        buffer[0] = 40;
        buffer[1] = (byte)Integer.parseInt(editText2.getText().toString());
        new Thread(() -> {
            try {
                socket.getOutputStream().write(buffer);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }




    private void toSpeaker(byte musicBytes[], AudioTrack audioTrack) {
        try
        {
            audioTrack.write(musicBytes, 0, musicBytes.length);
            audioTrack.play();
        } catch (Exception e) {
            System.out.println("Not working in speakers");
            e.printStackTrace();
        }
    }

    private AudioTrack createAudioTrack(){
        return new AudioTrack(AudioManager.STREAM_MUSIC,
                44100,
                AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT,
                312,    //buffer length in bytes
                AudioTrack.MODE_STREAM);
    }
}
