package sample;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ReceiverThread extends Thread {

    String host = null;

    ReceiverThread(String host){
        this.host = host;
    }

    @Override
    public void run(){
        try
        {
            //Socket socket = null; //Socket for connection
            int port = 50005; //Port of connection
            //String host = "127.0.0.1";
            //String host = "192.168.0.101";


            Thread getCommandThread = null;
            Thread SendingMusicThread = null;

            while(true) {
                /////////////////////////////
                //Main loop
                if(getCommandThread != null && getCommandThread.isAlive()){
                    getCommandThread.interrupt();
                }

                boolean connected = false;
                System.out.println("flag 1");
                while (!connected) { //Connect until succeeded
                    try {
                        StaticFields.socket = new Socket(host, port);
                        connected = true;
                        System.out.println("Connected");
                        getCommandThread = new ClientGetCommandThread(StaticFields.socket);
                        getCommandThread.start();
                    } catch (Exception e) {
                        System.out.println("Unable to connect to server");
                    }
                }

                boolean status = true;
                float sampleRate = 44100.0f;
                AudioFormat format = new AudioFormat(sampleRate, 16, 2, true, false); //Format of music
                DataLine.Info dataLineInfo = new DataLine.Info(SourceDataLine.class, format);
                SourceDataLine sourceDataLine = (SourceDataLine) AudioSystem.getLine(dataLineInfo);
                sourceDataLine.open(format);
                sourceDataLine.start();

                BufferedInputStream inFromClient = new BufferedInputStream(StaticFields.socket.getInputStream());
                DataOutputStream outToClient = new DataOutputStream(StaticFields.socket.getOutputStream());

                int receivedBytes; //Amount of bytes in packet
                int sizeOfMusicData = 312;
                byte[] buffer = new byte[sizeOfMusicData + 1]; //Received music bytes from server
                byte[] musicBuffer = new byte[sizeOfMusicData];
                int totalReceivedBytes = 0;
                double totalReceivedPackets = 0;
                List<String> musicNames = new ArrayList<>();

                while (status) {
                    long startTime = System.nanoTime();
                    long endTime;
                    try {
                        while ((receivedBytes = inFromClient.read(buffer)) > 0) {

                            //// Debug info
                            endTime = System.nanoTime();
                            totalReceivedBytes += receivedBytes;
                            totalReceivedPackets++;
                            System.out.println("Total length: " + totalReceivedBytes);
                            System.out.println("Bytes per second: " + totalReceivedBytes/((endTime - startTime) / 1000000000.0f) + " / 176375");
                            System.out.println("Total Received Packets: " + totalReceivedPackets);
                            System.out.println("Elapsed time: " + (endTime - startTime) / 1000000000.0f);
                            System.out.println("Answer: " + buffer[0]);
                            ////

                            if(buffer[0] == -128){
                                System.arraycopy(buffer, 1, musicBuffer, 0, sizeOfMusicData);
                                toSpeaker(musicBuffer, sourceDataLine);
                            } else if (buffer[0] == 105) {
                                SendingMusicThread = new ClientSendingMusicThread(StaticFields.socket, StaticFields.musicTitleToSend);
                                SendingMusicThread.start();
                            } else if (buffer[0] == 124) {
                                //Clear queue
                                musicNames.clear();
                            } else if (buffer[0] == 125) {
                                //Add to queue
                                musicNames.add(getMusicName(buffer));
                            } else if (buffer[0] == 126) {
                                //Print queue
                                printList(musicNames);
                                StaticFields.myController.updatelistView(musicNames);
                            }
                        }
                    } catch (Exception e){
                        e.printStackTrace();
                        break;
                    }
                }
                sourceDataLine.drain();
                sourceDataLine.close();
                ///////////////////////////
                ///////////////////////////
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    private static void printList(List<String> musicNames){
        System.out.println("Queue contains: ");
        for(int i = 0; i < musicNames.size(); i++){
            System.out.println((i+1) + ": " + musicNames.get(i));
        }
    }

    private static void toSpeaker(byte musicBytes[], SourceDataLine sourceDataLine) {
        try
        {
            sourceDataLine.write(musicBytes, 0, musicBytes.length);
        } catch (Exception e) {
            System.out.println("Not working in speakers");
            e.printStackTrace();
        }
    }

    private static String getMusicName(byte buffer[]) throws Exception {
        int i = 1;
        while(buffer[++i] != 0);
        return new String(Arrays.copyOfRange(buffer, 1, i), "UTF-8");
    }
}
