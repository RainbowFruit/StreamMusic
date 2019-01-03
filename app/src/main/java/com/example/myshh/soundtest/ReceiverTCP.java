package com.example.myshh.soundtest;//package Client;

//import Client.ClientGetCommandThread;

import android.media.*;

import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ReceiverTCP extends Thread {

    String host;
    int port;
    //boolean status = true;

    ReceiverTCP(String host, int port){
        this.host = host;
        this.port = port;
    }

    public void run(){
        Socket socket = null; //Socket for connection
        //int port = 50005; //Port of connection
        // String host = "192.168.0.13";

        Thread getCommandThread = null;

        System.out.println("Thread started");

            /////////////////////////////
            //Main loop
            boolean connected = false;
            while (!connected) { //Connect until succeeded
                try {
                    socket = new Socket("192.168.0.101", 50005);
                    StaticFields.socket = socket;
                    //socket.connect(new InetSocketAddress("192.168.0.13", 50005));
                    connected = true;
                } catch (Exception e) {
                    System.out.println("Unable to connect to server");
                    e.printStackTrace();
                }
            }
            System.out.println("Connected");

            int sampleRate = 44100;

            BufferedInputStream inFromClient = null;
            try {
                inFromClient = new BufferedInputStream(socket.getInputStream());
                DataOutputStream outToClient = new DataOutputStream(socket.getOutputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }

            int receivedBytes; //Amount of bytes in packet
            int sizeOfMusicData = 312;
            byte[] buffer = new byte[sizeOfMusicData + 1]; //Received music bytes from server
            byte[] musicBuffer = new byte[sizeOfMusicData];
            int totalReceivedBytes = 0;
            double totalReceivedPackets = 0;

            AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_STEREO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    sizeOfMusicData,    //buffer length in bytes
                    AudioTrack.MODE_STREAM);

            while (StaticFields.status) {
                long startTime = System.nanoTime();
                long endTime;
                try {
                    while ((receivedBytes = inFromClient.read(buffer)) > 0) {

                        /// Debug info
                        endTime = System.nanoTime();
                        totalReceivedBytes += receivedBytes;
                        totalReceivedPackets++;
                        System.out.println("Total length: " + totalReceivedBytes);
                        System.out.println("Total Received Packets: " + totalReceivedPackets);
                        System.out.println("Elapsed time: " + (endTime - startTime) / 1000000000.0f);
                        System.out.println("Answer: " + buffer[sizeOfMusicData]);
                        ////

                        if(buffer[0] == -128){
                            System.arraycopy(buffer, 1, musicBuffer, 0, sizeOfMusicData);
                            toSpeaker(musicBuffer, audioTrack);
                        }
                        if(!StaticFields.status){
                            return;
                        }
                    }
                } catch (Exception e){
                    e.printStackTrace();
                    break;
                }
            }
            ///////////////////////////
            ///////////////////////////
        System.out.print("Thread finished");
    }

    @Override
    public void interrupt() {
        super.interrupt();
    }

    /*public void go(String host, int port) throws Exception
    {
        Socket socket = null; //Socket for connection
        //int port = 50005; //Port of connection
       // String host = "192.168.0.13";

        Thread getCommandThread = null;

        System.out.println("Thread started");
        while(true) {
            /////////////////////////////
            //Main loop
            boolean connected = false;
            while (!connected) { //Connect until succeeded
                try {
                    socket = new Socket("192.168.0.13", 50005);
                    //socket.connect(new InetSocketAddress("192.168.0.13", 50005));
                    connected = true;
                } catch (Exception e) {
                    System.out.println("Unable to connect to server");
                    e.printStackTrace();
                }
            }
            System.out.println("Connected");

            boolean status = true;
            int sampleRate = 44100;

            BufferedInputStream inFromClient = new BufferedInputStream(socket.getInputStream());
            DataOutputStream outToClient = new DataOutputStream(socket.getOutputStream());

            int receivedBytes; //Amount of bytes in packet
            int sizeOfMusicData = 10000;
            byte[] buffer = new byte[sizeOfMusicData + 1]; //Received music bytes from server
            byte[] musicBuffer = new byte[sizeOfMusicData];
            int totalReceivedBytes = 0;
            double totalReceivedPackets = 0;

            AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_STEREO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    sizeOfMusicData,    //buffer length in bytes
                    AudioTrack.MODE_STREAM);

            while (status) {
                long startTime = System.nanoTime();
                long endTime;
                try {
                    while ((receivedBytes = inFromClient.read(buffer)) > 0) {

                        /// Debug info
                        endTime = System.nanoTime();
                        totalReceivedBytes += receivedBytes;
                        totalReceivedPackets++;
                        System.out.println("Total length: " + totalReceivedBytes);
                        System.out.println("Total Received Packets: " + totalReceivedPackets);
                        System.out.println("Elapsed time: " + (endTime - startTime) / 1000000000.0f);
                        System.out.println("Answer: " + buffer[sizeOfMusicData]);
                        ////

                        System.arraycopy(buffer, 0, musicBuffer, 0, sizeOfMusicData);
                        toSpeaker(musicBuffer, audioTrack);
                    }
                } catch (Exception e){
                    e.printStackTrace();
                    break;
                }
            }
        ///////////////////////////
        ///////////////////////////
        }
    }*/

    private static void toSpeaker(byte musicBytes[], AudioTrack audioTrack) {
        try
        {
            audioTrack.write(musicBytes, 0, musicBytes.length);
            audioTrack.play();
        } catch (Exception e) {
            System.out.println("Not working in speakers");
            e.printStackTrace();
        }
    }
}
