import java.io.DataOutputStream;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.net.Socket;
import java.nio.ByteBuffer;

public class ClientSendingMusicThread extends Thread {

    private int sizeOfMusicData = 312;
    private int sizeOfBuffer = sizeOfMusicData + 1; //One byte for command
    private byte answer = -128;
    private byte[] temp = new byte[sizeOfBuffer];
    private int totalSendBytes = 0;
    private byte[] sound = MusicToArray.convert("2.wav");
    private Socket socketToServer = null;

    ClientSendingMusicThread(Socket socket){
        socketToServer = socket;
    }

    @Override
    public void run() {
        super.run();
        
        byte[] fileSize = ByteBuffer.allocate(4).putInt(sound.length).array();
        System.out.print("Sound length: " + sound.length);
        for(int i = 0; i < fileSize.length; i++){
        	System.out.print(fileSize[i] + " ");
        	temp[i] = fileSize[i];
        }
        temp[312] = 111;
        try {
        	socketToServer.getOutputStream().write(temp, 0, sizeOfBuffer);
        	TimeUnit.MILLISECONDS.sleep(2000);
        } catch (Exception e) {
        	e.printStackTrace();
        }

        for (int i = 0; i < sound.length; i++) {
            temp[i % sizeOfMusicData] = sound[i];
            if (i % sizeOfMusicData == sizeOfMusicData - 1) {
                temp[312] = 115;
                try {
                	totalSendBytes += temp.length;
                    socketToServer.getOutputStream().write(temp, 0, sizeOfBuffer);
                    if(totalSendBytes % 10 == 0)
                    	System.out.println("Sent " + totalSendBytes + " bytes");
                    TimeUnit.NANOSECONDS.sleep(1);
                } catch (Exception e) {
                    System.out.println("Writing to socket failed");
                    e.printStackTrace();
                }
            }
        }
        System.out.println("Sent bytes: " + totalSendBytes);
    }
}
