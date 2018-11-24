package Client;

import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;

public class ClientGetCommandThread extends Thread {

    private Socket socketToServer = null;
    int sizeOfMusicData = 312;
    byte[] packet = new byte[sizeOfMusicData + 1];
    Scanner in = new Scanner(System.in);
	int command = 0;

    ClientGetCommandThread(Socket socket){
        socketToServer = socket;
    }

	private void sendData(){
		try {
			//Clear array
			for(int i = 0; i < packet.length; i++){
				packet[i] = 0;
			}
			
			//Send 100
			packet[0] = 100;
			socketToServer.getOutputStream().write(packet, 0, sizeOfMusicData + 1);
			
			//Wait for 105
			while(command != 105){
				try {
					socketToServer.getInputStream().read(packet);
					command = packet[0];
					System.out.println("Answer from server: " + command);
				} catch(Exception e) {
					e.printStackTrace();
				}
			}
			
			//Send 110
			System.out.println("Podaj nazwe pliku: ");
			in.nextLine();
			String name = in.nextLine();
			byte[] bytename = name.getBytes();
			for(int i = 0; i < bytename.length; i++)
				packet[i] = bytename[i];
			packet[0] = 110;
			socketToServer.getOutputStream().write(packet, 0, sizeOfMusicData + 1);
			//
			
			Thread UploadMusicToServer = new ClientSendingMusicThread(socketToServer);
			UploadMusicToServer.start();
			UploadMusicToServer.join();
			
			//Send 119 - Finish
			packet[0] = 119;
			socketToServer.getOutputStream().write(packet, 0, sizeOfMusicData + 1);	
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
	}
	
    @Override
    public void run() {
        super.run();
        while(true){
            packet[0] = (byte)in.nextInt();
            if(socketToServer.isConnected()){
				if(packet[0] == 100){
					sendData();
				} else {
		            try {
		                socketToServer.getOutputStream().write(packet, 0, 312 + 1);
		            } catch (IOException e) {
		                e.printStackTrace();
		                return;
		            }
                }
            }
        }
    }
}
