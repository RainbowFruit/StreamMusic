package Client;

import MusicFunctions.MusicToArray;

import java.util.concurrent.TimeUnit;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Scanner;

public class ClientSendingMusicThread extends Thread {

    private int sizeOfMusicData = 312;
    private int sizeOfBuffer = sizeOfMusicData + 1; //One byte for command
    private byte answer = -128;
    private byte[] musicBuffer = new byte[sizeOfMusicData];
	private byte[] buffer = new byte[sizeOfBuffer];
    private int totalSendBytes = 0;
    private byte[] sound = MusicToArray.convert("2.wav");
    private Socket socketToServer = null;
    Scanner in = new Scanner(System.in);

    ClientSendingMusicThread(Socket socket){
        socketToServer = socket;
    }

    @Override
    public void run() {
        super.run();
        
        Arrays.fill(buffer, (byte)0);
        System.out.println("Podaj nazwe pliku: ");
		in.nextLine();
		String name = in.nextLine();
		byte[] bytename = name.getBytes();
		for(int i = 1; i <= bytename.length; i++)
			buffer[i] = bytename[i-1];
		buffer[0] = 110;
		try {
			socketToServer.getOutputStream().write(buffer, 0, sizeOfBuffer);
		} catch (Exception e) {
			e.printStackTrace();
		}
			
		int wroteBytes = 0;
		int sentPackets = 0;
		try {
		    for (int i = 0; i < sound.length; i++) {
		        musicBuffer[i % sizeOfMusicData] = sound[i];
		        wroteBytes++;
		        if (i % sizeOfMusicData == sizeOfMusicData - 1) {
					System.arraycopy(musicBuffer, 0, buffer, 1, sizeOfMusicData);
		            buffer[0] = 115;	            
	            	totalSendBytes += wroteBytes;
	            	wroteBytes = 0;
	                socketToServer.getOutputStream().write(buffer, 0, sizeOfBuffer);
	                sentPackets++;
	                if(totalSendBytes % 10 == 0)
	                	System.out.println("Sent " + totalSendBytes + " bytes");
	                //TimeUnit.NANOSECONDS.sleep(1);
		        }
		    }
			//Finished uploading to server
		    buffer[0] = 119;
        	socketToServer.getOutputStream().write(buffer, 0, sizeOfBuffer);
        } catch (Exception e) {
            System.out.println("Writing to socket failed");
            e.printStackTrace();
        }
        System.out.println("Sent bytes: " + totalSendBytes + " sent packets: " + sentPackets);
    }
}
