import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.net.Socket;

public class ReceiverTCP {

    public static void main(String args[]) throws Exception
    {
        Socket socket = null; //Socket for connection
        int port = 50005; //Port of connection
        String host = "127.0.0.1";

        Thread getCommandThread = null;

        while(true) {
            /////////////////////////////
            //Main loop
            if(getCommandThread != null && getCommandThread.isAlive()){
                getCommandThread.interrupt();
            }

            boolean connected = false;
            while (!connected) { //Connect until succeeded
                try {
                    socket = new Socket(host, port);
                    connected = true;
                    getCommandThread = new ClientGetCommandThread(socket);
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

            BufferedInputStream inFromClient = new BufferedInputStream(socket.getInputStream());
            DataOutputStream outToClient = new DataOutputStream(socket.getOutputStream());

            int receivedBytes; //Amount of bytes in packet
            int sizeOfMusicData = 312;
            byte[] buffer = new byte[sizeOfMusicData + 1]; //Received music bytes from server
            byte[] musicBuffer = new byte[sizeOfMusicData];
            int totalReceivedBytes = 0;
            double totalReceivedPackets = 0;

            while (status) {
                long startTime = System.nanoTime();
                long endTime;
                try {
                    while ((receivedBytes = inFromClient.read(buffer)) > 0) {

                        /*/// Debug info
                        endTime = System.nanoTime();
                        totalReceivedBytes += receivedBytes;
                        totalReceivedPackets++;
                        System.out.println("Total length: " + totalReceivedBytes);
                        System.out.println("Total Received Packets: " + totalReceivedPackets);
                        System.out.println("Elapsed time: " + (endTime - startTime) / 1000000000.0f);
                        System.out.println("Answer: " + buffer[312]);
                        ///*/

                        System.arraycopy(buffer, 0, musicBuffer, 0, sizeOfMusicData);
                        toSpeaker(musicBuffer, sourceDataLine);
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
}
