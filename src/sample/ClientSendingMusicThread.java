package sample;

import javafx.application.Platform;

import java.io.IOException;
import java.net.Socket;
import java.sql.Time;
import java.util.Arrays;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class ClientSendingMusicThread extends Thread {

    private int sizeOfMusicData = 100;
    private int sizeOfBuffer = sizeOfMusicData + 1; //One byte for command
    private byte answer = -128;
    private byte[] musicBuffer = new byte[sizeOfMusicData];
	private byte[] buffer = new byte[sizeOfBuffer];
    private int totalSendBytes = 0;
    private byte[] sound;
    private Socket socketToServer = null;
    //Scanner in = new Scanner(System.in);
	private String title;

    ClientSendingMusicThread(Socket socket, String title){
        this.socketToServer = socket;
        this.title = title;
    }

    @Override
    public void run() {
        super.run();
        
        Arrays.fill(buffer, (byte)0);

        try {
			sound = MusicToArray.convert(title);
		} catch (Exception e){
        	System.out.println("Failed to open file");
			Platform.runLater(()-> StaticFields.myController.updatetextView("Failed to open file"));
			buffer[0] = 119;
			try {
				socketToServer.getOutputStream().write(buffer, 0, sizeOfBuffer);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			return;
		}
        /*System.out.println("Podaj nazwe pliku: ");
		in.nextLine();
		String name = in.nextLine();
		byte[] bytename = name.getBytes();*/
        byte[] bytename = title.getBytes();
		for(int i = 1; i <= bytename.length-4; i++)
			buffer[i] = bytename[i-1];
		buffer[0] = 110;
		try {
			socketToServer.getOutputStream().write(buffer, 0, sizeOfBuffer);
		} catch (Exception e) {
			e.printStackTrace();
		}
			
		int wroteBytes = 0;
		int sentPackets = 0;
		int mod = 0;
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

					TimeUnit.NANOSECONDS.sleep(1);
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
