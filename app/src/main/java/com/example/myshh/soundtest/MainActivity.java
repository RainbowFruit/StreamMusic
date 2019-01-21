package com.example.myshh.soundtest;

import android.media.AudioTrack;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private static ReceiverTCP thread = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /*new Thread(() -> {
            try {
                new ReceiverTCP().go();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();*/

        /*int bufferSize = AudioTrack.getMinBufferSize(44100
                , AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT);

        AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                44100,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                7072,    //buffer length in bytes
                AudioTrack.MODE_STREAM);

        System.out.println("minimum buffer size: " + bufferSize);

            byte[] musicBytes = new byte[312];
            Thread thread;
            for(int i = 0; i < 312; i++){
                musicBytes[i] = (byte)i;
            }

            int i = 0;

            while(true){
             toSpeaker(musicBytes, audioTrack);
            }*/
    }

    public void btnInterruptClicked(View v){
        if(thread != null) {
            //thread.interrupt();
            StaticFields.status = false;
        }
    }

    public void btnNextClicked(View v){
        new Thread(() -> {
            try {
                StaticFields.socket.getOutputStream().write((byte)10);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void btnPrevClicked(View v){
        new Thread(() -> {
            try {
                StaticFields.socket.getOutputStream().write((byte)20);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void btnClicked(View v){
        EditText editText = findViewById(R.id.editText);
        EditText editText2 = findViewById(R.id.editText2);
        String host = editText.getText().toString();
        int port = Integer.parseInt(editText2.getText().toString());

        if(thread != null){
            thread.interrupt();
        }

        thread = new ReceiverTCP(host, port);
        StaticFields.status = true;

        thread.start();
    }

    private static void toSpeaker(byte musicBytes[], AudioTrack audioTrack) {
        /*try {
            audioTrack.play();
        } catch (Exception e) {
            System.out.println("Play error");
            e.printStackTrace();
        }*/
        try {

            audioTrack.write(musicBytes, 0, musicBytes.length);
            audioTrack.play();

        } catch (Exception e) {
            System.out.println("Write error");
            e.printStackTrace();
        }
    }
}
