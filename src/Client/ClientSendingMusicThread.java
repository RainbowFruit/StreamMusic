package Client;

import MusicFunctions.MusicToArray;

import java.util.concurrent.TimeUnit;
import java.net.Socket;
import java.nio.ByteBuffer;

public class ClientSendingMusicThread extends Thread {

    private int sizeOfMusicData = 312;
    private int sizeOfBuffer = sizeOfMusicData + 1; //One byte for command
    private byte answer = -128;
    private byte[] musicBuffer = new byte[sizeOfMusicData];
	private byte[] buffer = new byte[sizeOfBuffer];
    private int totalSendBytes = 0;
    private byte[] sound = MusicToArray.convert("2.wav");
    private Socket socketToServer = null;

    ClientSendingMusicThread(Socket socket){
        socketToServer = socket;
    }

    @Override
    public void run() {
        super.run();
        
        /*byte[] fileSize = ByteBuffer.allocate(4).putInt(sound.length).array();
        System.out.print("Sound length: " + sound.length);
        for(int i = 0; i < fileSize.length; i++){
        	System.out.print(fileSize[i] + " ");
        	temp[i] = fileSize[i];
        }
        temp[sizeOfMusicData] = 111;
        try {
        	socketToServer.getOutputStream().write(temp, 0, sizeOfBuffer);
        	TimeUnit.MILLISECONDS.sleep(1000);
        } catch (Exception e) {
        	e.printStackTrace();
        }*/
		
		int wroteBytes = 0;
		//DataOutputStream out = new DataOutputStream(new BufferedOutputStream(socketToServer.getOutputStream()));
        for (int i = 0; i < sound.length; i++) {
            musicBuffer[i % sizeOfMusicData] = sound[i];
            wroteBytes++;
            if (i % sizeOfMusicData == sizeOfMusicData - 1) {
				//System.out.println("Wrote " + wroteBytes + " bytes.");
				System.arraycopy(musicBuffer, 0, buffer, 1, sizeOfMusicData);
                buffer[0] = 115;
                try {
                	totalSendBytes += wroteBytes;
                	wroteBytes = 0;
                    socketToServer.getOutputStream().write(buffer, 0, sizeOfBuffer);
                    if(totalSendBytes % 10 == 0)
                    	System.out.println("Sent " + totalSendBytes + " bytes");
                    //TimeUnit.NANOSECONDS.sleep(1);
					//TimeUnit.SECONDS.sleep(20);
                } catch (Exception e) {
                    System.out.println("Writing to socket failed");
                    e.printStackTrace();
                }
            }
        }
        System.out.println("Sent bytes: " + totalSendBytes);
    }
}
