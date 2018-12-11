package Server;

import MusicFunctions.MusicToArray;

import java.io.DataOutputStream;
import java.sql.Time;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ServerMusicSendingThread extends Thread {

    private int sizeOfMusicData = 312;
    private int sizeOfBuffer = sizeOfMusicData + 1; //One byte for command
    private byte answer = -128;
    private byte[] musicBuffer = new byte[sizeOfBuffer];
    private int totalSendBytes = 0;
    private byte[] sound = MusicToArray.convert("1.wav");
    private List<DataOutputStream> outputStreamList = null;

    ServerMusicSendingThread(List<DataOutputStream> list){
        outputStreamList = list;
    }

    @Override
    public void run() {
        super.run();

        for (int i = 0; i < sound.length; i++) {
            //musicBuffer[(i % sizeOfMusicData) + 1] = sound[i];
            musicBuffer[(i % sizeOfMusicData) + 1] = sound[i];
            //System.out.println("Writing to index: " + ((i % sizeOfMusicData) + 1));
            if (i % sizeOfMusicData == sizeOfMusicData - 1) {
                musicBuffer[0] = -128;
                try {
                    for (DataOutputStream out : outputStreamList) {
                        try {
                            out.write(musicBuffer, 0, sizeOfBuffer);
                        } catch (Exception e) {
                            System.out.println("Unable to write to " + out);
                            outputStreamList.remove(out);
                        }
                    }
                    totalSendBytes += musicBuffer.length;
                    //TimeUnit.MILLISECONDS.sleep(1);
                    //TimeUnit.NANOSECONDS.sleep(200);
                    //System.out.println("Wrote: " + musicBuffer[0] + ", in integer: " + (double)musicBuffer[0]);
                    for(int a = 0; a < sizeOfBuffer; a++){
                        System.out.print(musicBuffer[a] + ", ");
                    }
                    System.out.println();
                    //TimeUnit.SECONDS.sleep(1);
                    //TimeUnit.MILLISECONDS.sleep(500);
                    //System.in.read();
                } catch (Exception e) {
                    System.out.println("Writing to socket failed");
                    e.printStackTrace();
                }
            }
        }
        System.out.println("Sent bytes: " + totalSendBytes);
    }
}
