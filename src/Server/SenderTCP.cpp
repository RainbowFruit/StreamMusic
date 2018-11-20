#include <unistd.h>
#include <errno.h>
#include <netinet/in.h>
#include <sys/socket.h>
#include <cstdlib>
#include <cstdio>
#include <error.h>
#include <iostream>
#include <pthread.h>
#include <signal.h>
#include <vector>
#include <string>

#define byte unsigned char
using namespace std;

const int one = 1;
typedef struct  WAV_HEADER
{
    /* RIFF Chunk Descriptor */
    uint8_t         RIFF[4];        // RIFF Header Magic header
    uint32_t        ChunkSize;      // RIFF Chunk Size
    uint8_t         WAVE[4];        // WAVE Header
    /* "fmt" sub-chunk */
    uint8_t         fmt[4];         // FMT header
    uint32_t        Subchunk1Size;  // Size of the fmt chunk
    uint16_t        AudioFormat;    // Audio format 1=PCM,6=mulaw,7=alaw,     257=IBM Mu-Law, 258=IBM A-Law, 259=ADPCM
    uint16_t        NumOfChan;      // Number of channels 1=Mono 2=Sterio
    uint32_t        SamplesPerSec;  // Sampling Frequency in Hz
    uint32_t        bytesPerSec;    // bytes per second
    uint16_t        blockAlign;     // 2=16-bit mono, 4=16-bit stereo
    uint16_t        bitsPerSample;  // Number of bits per sample
    /* "data" sub-chunk */
    uint8_t         Subchunk2ID[4]; // "data"  string
    uint32_t        Subchunk2Size;  // Sampled data length
} wav_hdr;

struct arg_struct {
    int servSock;
    vector<int>* clientSockets;
    const char* musicName;
};


void MusicToBytes(int sock);
void *thread_MusicToBytes(void* args);
void *thread_ReceiveConnections(void* arguments);
void *thread_ListenAtClients(void* arguments);
void receiveFile(int clientSocket);

int main(int argc, char ** argv){

    if(argc!=2)
        error(1,0,"Usage: %s <port>", argv[0]);

    sockaddr_in localAddress{
        .sin_family = AF_INET,
        .sin_port   = htons(atoi(argv[1])),
        .sin_addr   = {htonl(INADDR_ANY)}
    };

    int servSock = socket(PF_INET, SOCK_STREAM, 0);
    setsockopt(servSock, SOL_SOCKET, SO_REUSEADDR, &one, sizeof(one));

    if(bind(servSock, (sockaddr*) &localAddress, sizeof(localAddress))) {
        error(1,errno,"Bind failed!");
    }


	vector<const char*> musicNames;
	musicNames.push_back("1.wav");
	musicNames.push_back("2.wav");

	vector<int> clientSockets;
	struct arg_struct arguments;
	arguments.servSock = servSock;
	arguments.clientSockets = &clientSockets;
	arguments.musicName = musicNames[0];

	//Thread n1
	pthread_t receiver;
	pthread_create (&receiver, NULL, &thread_ReceiveConnections, (void*)&arguments);

	//Thread n2
	pthread_t MusicSendingThread;
	pthread_create (&MusicSendingThread, NULL, &thread_MusicToBytes, (void*)&arguments);

	//Thread n3, listen for commands
	/*pthread_t ListenAtClientsThread;
	pthread_create (&ListenAtClientsThread, NULL, &thread_ListenAtClients, (void*)&arguments);

	pthread_join(receiver, NULL);
	pthread_join(MusicSendingThread, NULL);*/

	static const uint16_t BUFFER_SIZE = 312;
    int8_t* buffer = new int8_t[BUFFER_SIZE + 1];
    int bytesRead = 0;
    bool switcher = false;

	//Receiving signals loop
    while(true){
    	for(unsigned int i = 0; i < clientSockets.size(); i++){
    		if((bytesRead = read(clientSockets[i], buffer, BUFFER_SIZE + 1)) > 0){
    			cout << "Received command number: " << (int)*(buffer + 312) << endl;
    			//Read 100
				if((int)*(buffer + 312) == 100){
					//receiveFile((*clientSockets)[i]);
				} else if((int)*(buffer + 312) == 10){
					switcher = !switcher;
					arguments.musicName = musicNames[switcher];
					pthread_cancel(MusicSendingThread);
					//pthread_kill(MusicSendingThread, 3);
					pthread_join(MusicSendingThread, NULL);
					pthread_create (&MusicSendingThread, NULL, &thread_MusicToBytes, (void*)&arguments);
				}
    		} else if (bytesRead < 0) {
    			perror("ListenAtClients read error");	
    			if(errno == ECONNRESET){
            		cout << "Deleting socket" << endl;
            		clientSockets.erase(clientSockets.begin() + i);
		        }
		      }
    	}
    }
}







void *thread_ListenAtClients(void* arguments){
	struct arg_struct *args = (struct arg_struct *)arguments;
    vector<int>* clientSockets = args -> clientSockets;
    static const uint16_t BUFFER_SIZE = 312;
    int8_t* buffer = new int8_t[BUFFER_SIZE + 1];
    int bytesRead = 0;

    while(true){
    	for(unsigned int i = 0; i < (*clientSockets).size(); i++){
    		if((bytesRead = read((*clientSockets)[i], buffer, BUFFER_SIZE + 1)) > 0){
    			cout << "Received command number: " << (int)*(buffer + 312) << endl;
    			//Read 100
				if((int)*(buffer + 312) == 100){
					//receiveFile((*clientSockets)[i]);
				}
    		} else if (bytesRead < 0) {
    			perror("thread_ListenAtClients read error");	
    			if(errno == ECONNRESET){
            		cout << "Deleting socket" << endl;
            		(*clientSockets).erase((*clientSockets).begin() + i);
		        }
    		}
    	}
    }
}



/*void receiveFile(int clientSocket){
	static const uint16_t BUFFER_SIZE = 312;
    int8_t* packetBuffer = new int8_t[BUFFER_SIZE + 1];
    int bytesRead = 0;
    int receiving = 1;
    unsigned int fileSize = 0;
    int8_t* musicBuffer = NULL;
    int offset = 0;

    while(receiving) {
		if((bytesRead = read(clientSocket, packetBuffer, BUFFER_SIZE + 1)) > 0){
			//Read 110
			if((int)*(packetBuffer + 312) == 110){
				cout << "Received music name: ";
				for(int i = 0; i < BUFFER_SIZE; i++){
					cout << (char)*(packetBuffer + i);
				}
				cout << endl;
			  //Read 111
			} else if((int)*(packetBuffer + 312) == 111){
				fileSize = (fileSize << 8) + (unsigned char)*(packetBuffer);
				fileSize = (fileSize << 8) + (unsigned char)*(packetBuffer + 1);
				fileSize = (fileSize << 8) + (unsigned char)*(packetBuffer + 2);
				fileSize = (fileSize << 8) + (unsigned char)*(packetBuffer + 3);
				cout << "Received music size " << fileSize << endl;
				musicBuffer = new int8_t[fileSize];
			} else if((int)*(packetBuffer + 312) == 115){
				cout << "Receiving music bytes..." << endl;
				for(int i = 0; i < BUFFER_SIZE; i++){
					*(musicBuffer + offset++) = *(packetBuffer + i);
				}
			} else if((int)*(packetBuffer + 312) == 119){
				cout << "Finished receiving data, received bytes: " << offset - 1 << endl;
				receiving = 0;
			}
		}
    }
}*/



void *thread_ReceiveConnections(void* arguments){
    struct arg_struct *args = (struct arg_struct *)arguments;
    vector<int>* clientSockets = args -> clientSockets;
    int servSock = args -> servSock;
    int sock = 0;

	listen(servSock, 1);
	cout << "Server started" << endl;
	while(true){
        sock = accept(servSock, nullptr, nullptr);
        if(sock == -1){
            perror("Accept error");
        } else {
            (*clientSockets).push_back(sock);
            cout << "Accepted connection" << endl;
        }
    }

    pthread_exit(NULL);
    return NULL;
}


void *thread_MusicToBytes(void* arguments){

    //vector<int>* clientSockets = static_cast<vector<int>*>(args);

	struct arg_struct *args = (struct arg_struct *)arguments;
    vector<int>* clientSockets = args -> clientSockets;
	int bytesWrote;
    wav_hdr wavHeader;
    int headerSize = sizeof(wav_hdr);
    const char* filePath = args -> musicName;
    
    //cout << "Name: " << *(args -> musicName);
    FILE* wavFile = fopen(filePath, "r");
    if (wavFile == nullptr)
    {
        fprintf(stderr, "Unable to open wave file: %s\n", filePath);
        return 0;
    }

    //Read the header
    size_t bytesRead = fread(&wavHeader, 1, headerSize, wavFile);
    cout << "Header Read " << bytesRead << " bytes." << endl;
    if (bytesRead > 0)
    {
        static const uint16_t BUFFER_SIZE = 312;
        int8_t* buffer = new int8_t[BUFFER_SIZE + 1];
        while ((bytesRead = fread(buffer, sizeof buffer[0], BUFFER_SIZE / (sizeof buffer[0]), wavFile)) > 0)
        {
        	buffer[312] = -128;
            for(unsigned int i = 0; i < clientSockets->size(); i++){
		            if((bytesWrote = write((*clientSockets)[i], buffer, bytesRead + 1)) < 0){
		            	perror("Write error");
		            	if(errno == ECONNRESET){
		            		cout << "Deleting socket" << endl;
		            		(*clientSockets).erase((*clientSockets).begin() + i);
		            	}
		            }else if (bytesWrote < 0) {
						perror("thread_ListenAtClients read error");
					}
            }
			usleep(1635);
            //cout << "Sent " << bytesRead << " bytes." << endl;
        }
        delete [] buffer;
        buffer = nullptr;

    }
    fclose(wavFile);

    pthread_exit(NULL);
    return NULL;
}

