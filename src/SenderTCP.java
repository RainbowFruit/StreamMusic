import javax.sound.sampled.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class SenderTCP {

    //Convert music into byte Array
    private static byte[] musictoArray(String pathname) {

        int BUFFER_LENGTH = 1024;
        File file = new File(pathname);
        AudioInputStream inputAIS;
        AudioFileFormat fileFormat;

        try {
            inputAIS = AudioSystem.getAudioInputStream(file);
            fileFormat = AudioSystem.getAudioFileFormat(file);
        } catch (UnsupportedAudioFileException | IOException e) {
            e.printStackTrace();
            return new byte[10];
        }

        AudioFormat audioFormat = fileFormat.getFormat();
        System.out.println(" Frame rate: " + audioFormat.getFrameRate() + " Frame size: " + audioFormat.getFrameSize() + " Channels: " + audioFormat.getChannels() + " Sample size in bits: " + audioFormat.getSampleSizeInBits() + " Encoding: " + audioFormat.getEncoding() + " Sample rate: " + audioFormat.getSampleRate());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int nBufferSize = BUFFER_LENGTH * audioFormat.getFrameSize();
        int nBytesRead = 0;
        byte[] abBuffer = new byte[nBufferSize];

        while (true) {
            try {
                nBytesRead = inputAIS.read(abBuffer);
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (nBytesRead == -1) {
                break;
            }
            baos.write(abBuffer, 0, nBytesRead);
        }
        //Music in byte array
        return baos.toByteArray();
    }

    public static void main(String[] args) {
        List<DataOutputStream> outputStreamList = new ArrayList<>();

        new Thread(() -> {
            while(true){
                try {
                    ServerSocket socket = new ServerSocket(50005);
                    System.out.println("Server started at port: 50005");
                    while(true) {
                        Socket connectionSocket = socket.accept();
                        System.out.println("Accepted connection from " + connectionSocket.getInetAddress());
                        outputStreamList.add(new DataOutputStream(new BufferedOutputStream(connectionSocket.getOutputStream())));
                    }
                } catch (IOException e){
                    e.printStackTrace();
                    return;
            }
        }}).start();

        int SIZE = 312;
        byte[] temp = new byte[SIZE];
        int totalSendBytes = 0;
        byte[] sound = musictoArray("1.wav");

        for (int i = 0; i < sound.length; i++) {
            temp[i % SIZE] = sound[i];
            if (i % SIZE == 0) {
                try {
                    for (DataOutputStream out : outputStreamList) {
                        try {
                            out.write(temp, 0, SIZE);
                        } catch (Exception e) {
                            System.out.println("Unable to write to " + out);
                            outputStreamList.remove(out);
                        }
                    }
                    totalSendBytes += temp.length;
                    TimeUnit.MILLISECONDS.sleep(1);
                } catch (Exception e) {
                    System.out.println("Writing to socket failed");
                    e.printStackTrace();
                }
            }
        }
        System.out.println("Sent bytes: " + totalSendBytes);
    }
}