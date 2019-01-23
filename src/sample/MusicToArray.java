package sample;

import javax.sound.sampled.*;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

public class MusicToArray {

    //Convert music into byte Array
    public static byte[] convert(String pathname) throws Exception{

        int BUFFER_LENGTH = 1024;
        File file = new File(pathname);
        AudioInputStream inputAIS;
        AudioFileFormat fileFormat;

        //try {
        inputAIS = AudioSystem.getAudioInputStream(file);
        fileFormat = AudioSystem.getAudioFileFormat(file);
       //} catch (UnsupportedAudioFileException | IOException e) {
           // e.printStackTrace();
           // return new byte[10];
        //}

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
}
