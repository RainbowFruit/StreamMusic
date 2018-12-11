#include <unistd.h>
#include <errno.h>
#include <netinet/in.h>
#include <sys/socket.h>
#include <sys/epoll.h>
#include <cstdlib>
#include <cstdio>
#include <error.h>
#include <iostream>
#include <pthread.h>
#include <signal.h>
#include <vector>
#include <cstring>

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
    int epollfd;
    int currentClientSocket;
    bool isMusicThreadRunning;
    unsigned int* pCurrentMusic;
	vector<int>* pClientSockets;
	vector<const char*>* pMusicNames;
    epoll_event *ee;   
};

const char* getMusic(struct arg_struct* arguments);
void deleteFromVector(vector<int>* pClientSockets, int fd);
int8_t* subarray(int8_t* buffer);
int8_t* getMusicName(int8_t* buffer, int to);
int makeServSock(int port);
void clearBuffer(int8_t* buffer);
void sortQueue(int8_t* buffer, struct arg_struct *args);
void sendQueueToSocket(vector<const char*>* pMusicNames, vector<int>* pClientSockets, int sendTo);
void* thread_Listen(void* arguments);
void *thread_ReceiveConnections(void* arguments);
void *thread_MusicToBytes(void* args);
void *thread_sendQueue(void* arguments);
void *thread_receiveFile(void* args);

const uint16_t MUSIC_SIZE = 312;
const uint16_t PACKET_SIZE = MUSIC_SIZE + 1;

/*Commands:
****From client to server:
****10  - Go to next music in queue
****20  - Go to previous music in queue
****30  - Go to music at index
****40  - Remove music from queue at index
****50  - Receive numbers with music positions at queue
****100 - Want to send music, server answers 105 and starts to listen
****110 - client sends music name to server
****115 - packet with music data
****119 - finished sending music

****From server to client:
****-128 - music bytes
****105 - listening for music bytes
****124 - clear queue at client
****125 - add music name to client queue
****126 - finished updating queue at client
*/

int main(int argc, char ** argv){

    if(argc!=2) {
        error(1,0,"Usage: %s <port>", argv[0]);
	}

	//Thread n1
	pthread_t receiver;
	
	//Thread n2
	pthread_t MusicSendingThread;
	
	//Thread n3
	pthread_t ListenAtClients;
	
	//Thread for queue update signal
	//pthread_t queueUpdateThread;
	
	int servSock = makeServSock(atoi(argv[1])); //Server socket
	int epollfd = epoll_create1(0);
	epoll_event ee {};
	ee.events = EPOLLIN;
	
	vector<const char*> musicNames;
	musicNames.push_back("Strefa komfortu.wav");
	musicNames.push_back("Russia.wav"); 
	musicNames.push_back("Good Life.wav"); 
	musicNames.push_back("Must Have Been.wav"); 
	
	unsigned int currentMusic = 0;
	vector<int> clientSockets;
	
	struct arg_struct arguments; //Struct for pthreads
	arguments.servSock = servSock;
	arguments.pCurrentMusic = &currentMusic;
	arguments.pClientSockets = &clientSockets;
	arguments.pMusicNames = &musicNames;
	arguments.ee = &ee;
	arguments.epollfd = epollfd;
	arguments.currentClientSocket = 1200;

	//Receive connections
	pthread_create (&receiver, NULL, &thread_ReceiveConnections, (void*)&arguments);
	//Music sending
	pthread_create (&MusicSendingThread, NULL, &thread_MusicToBytes, (void*)&arguments);
	//Listen at clients
	pthread_create (&ListenAtClients, NULL, &thread_Listen, (void*)&arguments);
	
	//pthread_create (&queueUpdateThread, NULL, &thread_sendQueue, (void*)&arguments);

	while(true){
		pthread_join(MusicSendingThread, NULL);
		arguments.isMusicThreadRunning = true;
		pthread_create(&MusicSendingThread, NULL, &thread_MusicToBytes, (void*)&arguments);
	}
}


//Return name of music by index in currentMusic
const char* getMusic(struct arg_struct* arguments){
	struct arg_struct *args = (struct arg_struct*)arguments;
	unsigned int* pCurrentMusic = args -> pCurrentMusic;
	vector<const char*>* pMusicNames = args -> pMusicNames;
	if(*pCurrentMusic >= pMusicNames -> size()){
		*pCurrentMusic = 0;
	}
	const char* name = (*pMusicNames)[*pCurrentMusic];
	return name;
}


//Find file descriptor in vector and delete it
void deleteFromVector(vector<int>* pClientSockets, int fd){
	int i = -1;
	while((*pClientSockets)[++i] != fd);
	(*pClientSockets).erase((*pClientSockets).begin() + i);
}

	
//Retrieve musicData from buffer with commandByte
int8_t* subarray(int8_t* buffer){
	int8_t* data = new int8_t[MUSIC_SIZE];
	for(int i = 1; i < PACKET_SIZE; i++){
		*(data+i-1) = *(buffer+i);
	}
	return data;
}

int8_t* getMusicName(int8_t* buffer, int to){
	int8_t* data = new int8_t[MUSIC_SIZE];
	int i = 1;
	for(i = 1; i < to; i++){
		*(data+i-1) = *(buffer+i);
	}
	//const char* wavEnding = ".wav";
	*(data-1+i++) = 46;
	*(data-1+i++) = 119;
	*(data-1+i++) = 97;
	*(data-1+i++) = 118;
	return data;
}


//Initialize servSock
int makeServSock(int port){
	sockaddr_in localAddress{
        .sin_family = AF_INET,
        .sin_port   = htons(port),
        .sin_addr   = {htonl(INADDR_ANY)}
    };
    
    int servSock = socket(PF_INET, SOCK_STREAM, 0);
    setsockopt(servSock, SOL_SOCKET, SO_REUSEADDR, &one, sizeof(one));
    
    if(bind(servSock, (sockaddr*) &localAddress, sizeof(localAddress))) {
        error(1,errno,"Bind failed!");
    }
    return servSock;
}

void clearBuffer(int8_t* buffer){
    for(int i = 0; i < PACKET_SIZE; i++){
        *(buffer + i) = 0;
    }
}

void sortQueue(int8_t* buffer, struct arg_struct *args){
	vector<const char*>* pMusicNames = args -> pMusicNames;
	vector<const char*> tempVector;
	for(int i = 1; i < PACKET_SIZE; i++){
		if(*(buffer + i) == 0){
			break;
		}
		tempVector.push_back((*pMusicNames)[(int)*(buffer + i) - 1]);
	}
	(*pMusicNames) = tempVector;
	for(unsigned int i = 0; i < (*pMusicNames).size(); i++){
		cout << (*pMusicNames)[i] << endl;
	}	
}

/************
// THREADS //
************/

//Thread listening at clientSockets
void *thread_Listen(void* arguments){
	struct arg_struct *args = (struct arg_struct*)arguments;
	int epollfd = args -> epollfd;
	unsigned int* pCurrentMusic = args -> pCurrentMusic;
	vector<int>* pClientSockets = args -> pClientSockets;
	vector<const char*>* pMusicNames = args -> pMusicNames;
	epoll_event *ee = args -> ee;
	
	int8_t* buffer = new int8_t[PACKET_SIZE];
	int command = 0;
	int receivedBytes = 0;
	pthread_t receiveFileThread;
	pthread_t SendUpdatedQueue;

	while(epoll_wait(epollfd, ee, 1, -1)){
		if((receivedBytes = read(ee->data.fd, buffer, PACKET_SIZE)) < 0){
		//Handle error
			perror("thread_Listen read error");	
    			if(errno == ECONNRESET){
            		cout << "Closing socket: " << errno << endl;
					close(ee->data.fd);
					deleteFromVector(pClientSockets, ee->data.fd);
		        }
		} else {
			command = (int)*(buffer);
			cout << "Received signal from: " << ee->data.fd << " command: " << command << endl;
			if(command == 100) {
				//Client wants to send file
				args->currentClientSocket = ee->data.fd;
				epoll_ctl(epollfd, EPOLL_CTL_DEL, ee->data.fd, ee);  
				pthread_create(&receiveFileThread, NULL, thread_receiveFile, (void*)args);
			}
			if(command == 50) {
				//Received vector with new queue positions
				sortQueue(buffer, args);				
				args -> currentClientSocket = -1;
 			    pthread_create (&SendUpdatedQueue, NULL, &thread_sendQueue, (void*)args);
			}
			if(command == 10) {
				//Next music
				*pCurrentMusic = *pCurrentMusic + 1;
				args -> isMusicThreadRunning = false;
 			    //pthread_create (&MusicSendingThread, NULL, &thread_MusicToBytes, (void*)args);
			}
			if(command == 20){
				//Prev music
				*pCurrentMusic = *pCurrentMusic - 1;
				args -> isMusicThreadRunning = false;
			}
			if(command == 30){
				*pCurrentMusic = (int)*(buffer + 1) - 1;
				args -> isMusicThreadRunning = false;
				//TODO: Go to music number in packet; Implement in client
			}
			if(command == 40){
				//TODO: Implement removing in client				
				if((*pMusicNames).size() >= (unsigned int)*(buffer + 1)){
					(*pMusicNames).erase((*pMusicNames).begin() + (int)*(buffer + 1) - 1);
					args -> currentClientSocket = -1;
	 			    pthread_create (&SendUpdatedQueue, NULL, &thread_sendQueue, (void*)args);
 			    } else {
 			    	cout << "Delete error: index out of bound" << endl;
 			    }
			}
		}
	}
	delete [] buffer;
	buffer = nullptr;
	return NULL;
}


//Listen at servSock, receive connections, put client sockets at vector and add them to epoll
void *thread_ReceiveConnections(void* arguments){
    struct arg_struct *args = (struct arg_struct *)arguments;
    int servSock = args -> servSock;
    int epollfd = args -> epollfd;
    vector<int>* pClientSockets = args -> pClientSockets;
    epoll_event *event = args -> ee;
    int sock = 0;
    pthread_t queueUpdateThread;

	listen(servSock, 1);
	cout << "Server started" << endl;

	while(true){
        sock = accept(servSock, nullptr, nullptr);
        if(sock == -1){
            perror("Accept error");
        } else {
            (*pClientSockets).push_back(sock);
            event->data.fd = sock;
            epoll_ctl(epollfd, EPOLL_CTL_ADD, sock, event);  
            args -> currentClientSocket = sock;
            pthread_create (&queueUpdateThread, NULL, &thread_sendQueue, (void*)args);
            cout << "Accepted connection, socket: " << sock << endl;
        }
    }

    pthread_exit(NULL);
    return NULL;
}


//Convert music to byte array and send it to all client sockets
void *thread_MusicToBytes(void* arguments){
	struct arg_struct *args = (struct arg_struct *)arguments;
    vector<int>* pClientSockets = args -> pClientSockets;
    bool* pSwitcher = &(args -> isMusicThreadRunning);
	unsigned int* pCurrentMusic = args -> pCurrentMusic;
    
	int bytesWrote;
    wav_hdr wavHeader;
    int headerSize = sizeof(wav_hdr);
    const char* filePath = getMusic(args);  
    FILE* wavFile = fopen(filePath, "r");
    if (wavFile == nullptr)
    {
        fprintf(stderr, "Unable to open wave file: %s\n", filePath);
        return 0;
    }
    //Read the header
    size_t bytesRead = fread(&wavHeader, 1, headerSize, wavFile);
    //cout << "Header Read " << bytesRead << " bytes." << endl;
    if (bytesRead > 0)
    {
        int8_t* buffer = new int8_t[PACKET_SIZE];
        //While data in wav File
        while ((bytesRead = fread(buffer + 1, sizeof buffer[0],
        	    MUSIC_SIZE / (sizeof buffer[0]), wavFile)) > 0)
        {
        	buffer[0] = -128;
            for(unsigned int i = 0; i < pClientSockets->size(); i++){
            	if(*pSwitcher){
		            if((bytesWrote = write((*pClientSockets)[i], buffer, bytesRead + 1)) < 0){
		            	//Handle error
		            	perror("Write error");
		            	//if(errno == ECONNRESET)
		            	{
		            		cout << "Deleting socket, errno: " << errno << endl;
		            		(*pClientSockets).erase((*pClientSockets).begin() + i);
		            	}
		            }
	           } else {
            		cout << "Thread aborted" << endl;
	           		delete [] buffer;
        			buffer = nullptr;
	           		fclose(wavFile);
					pthread_exit(NULL);
					return NULL;
	           }
	           		
            }
			usleep(700);
        }
        delete [] buffer;
        buffer = nullptr;
    }
    //After sending music, go up in queue
    *pCurrentMusic = *pCurrentMusic + 1;
    fclose(wavFile);
    pthread_exit(NULL);
    return NULL;
}



void *thread_sendQueue(void* arguments){
	struct arg_struct *args = (struct arg_struct *)arguments;
	int currentClientSocket = args -> currentClientSocket;
    vector<int>* pClientSockets = args -> pClientSockets;
	vector<const char*>* pMusicNames = args -> pMusicNames;
    
    if(currentClientSocket == -1){
        cout << "Sending updated queue" << endl;
		//For every socket
		for(unsigned int n = 0; n < pClientSockets->size(); n++){
			sendQueueToSocket(pMusicNames, pClientSockets, (*pClientSockets)[n]);
		}
    } else if(currentClientSocket > 2) {
        cout << "Sending queue to new user: " << currentClientSocket << endl;
    	sendQueueToSocket(pMusicNames, pClientSockets, currentClientSocket);
    }
	//cout << "Sent queue signal" << endl;
    pthread_exit(NULL);
    return NULL;
}

void sendQueueToSocket(vector<const char*>* pMusicNames, vector<int>* pClientSockets, int sendTo){
    int8_t* buffer = new int8_t[PACKET_SIZE];   
    try {
	    //Clear queue at clients
		buffer[0] = 124;
		write(sendTo, buffer, PACKET_SIZE);
		clearBuffer(buffer);
			 
		//For every music title
		for(unsigned int i = 0; i < pMusicNames->size(); i++){
			for(unsigned int m = 0; m < strlen((*pMusicNames)[i]); m++){
				//Convert name to buffer
				buffer[m+1] = (*pMusicNames)[i][m];
			}
			buffer[0] = 125;
			write(sendTo, buffer, PACKET_SIZE);
			clearBuffer(buffer);
		}	
		
		//Send queue finish
		buffer[0] = 126;
		write(sendTo, buffer, PACKET_SIZE);
	} catch (...) {
			//Handle error
			perror("sendQueueToSocket write error");
			cout << "Deleting socket, errno: " << errno << endl;
			deleteFromVector(pClientSockets, sendTo);
	}
	delete [] buffer;
	buffer = nullptr;	
}


void *thread_receiveFile(void* arguments){
	struct arg_struct *args = (struct arg_struct *)arguments;
    int clientSocket = args -> currentClientSocket;
	vector<const char*>* pMusicNames = args -> pMusicNames;
    epoll_event *ee = args -> ee;
    
    int8_t* buffer = new int8_t[PACKET_SIZE];
    int bytesRead = 0;
    int receiving = 1;
    int command = 0;
    int bytesReceived = 0;
    int totalBytesReceived = 0;
    int receivedPackets = 0;
    const char* filePath = "ReceivedMusic.wav";
    FILE* wavFile = nullptr;
    const char* receivedName = nullptr;
    
    pthread_t SendUpdatedQueue;
    
	buffer[0] = 105;
	if(write(clientSocket, buffer, PACKET_SIZE) < 0){
		perror("Write command 105 error");
		pthread_exit(NULL);
		return NULL;
	}
	
	cout << "Started receiving file from " << clientSocket << endl;
		
    while(receiving) {
		if((bytesRead = read(clientSocket, buffer, PACKET_SIZE)) > 0){ //TODO: Add timeout
			command = (int)*(buffer);
			int8_t* tempBuffer = subarray(buffer);
			switch (command){
			
					case 110: //Read 110
						for(int i = 1; i < MUSIC_SIZE; i++){
							if((int)*(buffer + i) == 0){
								receivedName = (const char*)getMusicName(buffer, i);
								cout << "Received music name: "
								 << receivedName << endl;
								wavFile = fopen(receivedName, "w");
								if (wavFile == nullptr){
									fprintf(stderr, "Unable to open wave file: %s\n", filePath);
									return 0;
								}
								break;
							}
						}
					break;
					
					case 115:
						if((bytesReceived = fwrite(tempBuffer, sizeof *(buffer),
						    bytesRead - 1, wavFile)) < 0){
							perror("Write to file error");
							pthread_exit(NULL);
							return NULL;
						}
						receivedPackets++;
						totalBytesReceived += bytesRead - 1;
					break;
					
			 		case 119:
			 			(*pMusicNames).push_back(receivedName);
			 			args -> currentClientSocket = -1;
 			            pthread_create (&SendUpdatedQueue, NULL, &thread_sendQueue, (void*)args);
						cout << "Finished receiving data from " << clientSocket
						<< ", received bytes: " << totalBytesReceived
						<< ", received packets: " << receivedPackets << endl;
						receiving = 0;
					break;
			}
			
			delete [] tempBuffer;
			tempBuffer = nullptr;
			
		} else { //Error handle
			perror("Receiving music error, closing connection");
			delete [] buffer;
			buffer = nullptr;
			close(clientSocket);
			pthread_exit(NULL);
    		return NULL;
		}
    }    
    epoll_ctl(args -> epollfd, EPOLL_CTL_ADD, clientSocket, ee);  
    pthread_exit(NULL);
    return NULL;
}