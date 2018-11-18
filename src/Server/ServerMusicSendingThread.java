import java.io.DataOutputStream;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ServerMusicSendingThread extends Thread {

    private int sizeOfMusicData = 312;
    private int sizeOfBuffer = sizeOfMusicData + 1; //One byte for command
    private byte answer = -128;
    private byte[] temp = new byte[sizeOfBuffer];
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
            temp[i % sizeOfMusicData] = sound[i];
            if (i % sizeOfMusicData == sizeOfMusicData - 1) {
                temp[312] = answer++;
                try {
                    for (DataOutputStream out : outputStreamList) {
                        try {
                            out.write(temp, 0, sizeOfBuffer);
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
