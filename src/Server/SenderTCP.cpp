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
	pthread_t* MusicSendingThread;
	unsigned int* currentMusic;
	vector<int>* clientSockets;
	vector<const char*>* musicNames;
    epoll_event *ee;
    int epollfd;
};

struct recvFile_struct {
	int clientSocket;
	int epollfd;
	epoll_event *ee;
};

const char* getMusic(struct arg_struct* arguments);
void deleteFromVector(vector<int>* clientSockets, int fd);
int8_t* subarray(int8_t* buffer);
int makeServSock(int port);
void MusicToBytes(int sock);
void* thread_Listen(void* arguments);
void *thread_ReceiveConnections(void* arguments);
void *thread_MusicToBytes(void* args);
void *thread_receiveFile(void* args);


const uint16_t BUFFER_SIZE = 312;

/*Commands:
****From client to server:
****100 - Want to send music, server answers 105 and starts to listen
****110 - client sends music name to server
****115 - packet with music data
****119 - finished sending music
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
	
	int servSock = makeServSock(atoi(argv[1])); //Server socket
	int epollfd = epoll_create1(0);
	cout << "epollfd: " << epollfd << endl;
	epoll_event ee {};
	ee.events = EPOLLIN;
	
	vector<const char*> musicNames;
	musicNames.push_back("2.wav");
	musicNames.push_back("1.wav"); 	
	
	unsigned int currentMusic = 0;
	vector<int> clientSockets;
	
	struct arg_struct arguments; //Struct for pthreads
	arguments.servSock = servSock;
	arguments.MusicSendingThread = &MusicSendingThread;
	arguments.currentMusic = &currentMusic;
	arguments.clientSockets = &clientSockets;
	arguments.musicNames = &musicNames;
	arguments.ee = &ee;
	arguments.epollfd = epollfd;

	//Receive connections
	pthread_create (&receiver, NULL, &thread_ReceiveConnections, (void*)&arguments);
	//Music sending
	pthread_create (&MusicSendingThread, NULL, &thread_MusicToBytes, (void*)&arguments);
	//Listen at clients
	pthread_create (&ListenAtClients, NULL, &thread_Listen, (void*)&arguments);

	while(true){
		pthread_join(MusicSendingThread, NULL);
		currentMusic++;
		pthread_create(&MusicSendingThread, NULL, &thread_MusicToBytes, (void*)&arguments);
	}
}


//Return name of music by index in currentMusic
const char* getMusic(struct arg_struct* arguments){
	struct arg_struct *args = (struct arg_struct *)arguments;
	unsigned int* currentMusic = args -> currentMusic;
	vector<const char*>* musicNames = args -> musicNames;
	if(*currentMusic >= musicNames -> size()){
		*currentMusic = 0;
	}
	const char* name = (*musicNames)[*currentMusic];
	return name;
}


//Find file descriptor in vector and delete it
void deleteFromVector(vector<int>* clientSockets, int fd){
	int i = -1;
	while((*clientSockets)[++i] != fd);
	(*clientSockets).erase((*clientSockets).begin() + i);
}

	
//Retrieve musicData from buffer with commandByte
int8_t* subarray(int8_t* buffer){
	int8_t* data = new int8_t[BUFFER_SIZE];
	for(int i = 1; i < BUFFER_SIZE; i++){
		*(data+i-1) = *(buffer+i);
	}
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


/************
// THREADS //
************/

//Thread listening at clientSockets
void* thread_Listen(void* arguments){

	struct arg_struct *args = (struct arg_struct *)arguments;
	struct recvFile_struct recvFileArgs;
	vector<int>* clientSockets = args -> clientSockets;
	int epollfd = args -> epollfd;
	epoll_event *ee = args -> ee;
	int8_t* buffer = new int8_t[BUFFER_SIZE + 1];
	int command = 0;
	int receivedBytes = 0;
	pthread_t receiveFileThread;

	while(epoll_wait(epollfd, ee, 1, -1)){
		if((receivedBytes = read(ee->data.fd, buffer, BUFFER_SIZE + 1)) < 0){
		//Handle error
			perror("thread_Listen read error");	
    			if(errno == ECONNRESET){
            		cout << "Closing socket: " << errno << endl;
					close(ee->data.fd);
					deleteFromVector(clientSockets, ee->data.fd);
		        }
		} else {
			command = (int)*(buffer);
			cout << "Received signal from: " << ee->data.fd << " command: " << command << endl;
			cout << "Received: " << receivedBytes << " bytes." << endl;
			/*if(command == 100) {
				recvFileArgs.clientSocket = ee->data.fd;
				recvFileArgs.ee = ee;
				recvFileArgs.epollfd = epollfd;
				epoll_ctl(epollfd, EPOLL_CTL_DEL, ee->data.fd, ee);  
				pthread_create(&receiveFileThread, NULL, thread_receiveFile, (void*)&recvFileArgs);
			}*/
			//TODO: Handle commands
		}
	}
	return NULL;
}


//Listen at servSock, receive connections, put client sockets at vector and add them to epoll
void *thread_ReceiveConnections(void* arguments){
    struct arg_struct *args = (struct arg_struct *)arguments;
    vector<int>* clientSockets = args -> clientSockets;
    int servSock = args -> servSock;
    int epollfd = args -> epollfd;
    epoll_event *event = args -> ee;
    int sock = 0;

	listen(servSock, 1);
	cout << "Server started" << endl;

	while(true){
        sock = accept(servSock, nullptr, nullptr);
        if(sock == -1){
            perror("Accept error");
        } else {
            (*clientSockets).push_back(sock);
            event->data.fd = sock;
            epoll_ctl(epollfd, EPOLL_CTL_ADD, sock, event);  
            cout << "Accepted connection, socket: " << sock << endl;
        }
    }

    pthread_exit(NULL);
    return NULL;
}


//Convert music to byte array and send it to all client sockets
void *thread_MusicToBytes(void* arguments){

	struct arg_struct *args = (struct arg_struct *)arguments;
    vector<int>* clientSockets = args -> clientSockets;
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
    cout << "Header Read " << bytesRead << " bytes." << endl;
    if (bytesRead > 0)
    {
        int8_t* buffer = new int8_t[BUFFER_SIZE + 1];
        //While data in wav File
        while ((bytesRead = fread(buffer + 1, sizeof buffer[0],
        	    BUFFER_SIZE / (sizeof buffer[0]), wavFile)) > 0)
        {
        	buffer[0] = -128;
            for(unsigned int i = 0; i < clientSockets->size(); i++){
		            if((bytesWrote = write((*clientSockets)[i], buffer, bytesRead + 1)) < 0){
		            	//Handle error
		            	perror("Write error");
		            	if(errno == ECONNRESET){
		            		cout << "Deleting socket" << endl;
		            		(*clientSockets).erase((*clientSockets).begin() + i);
		            	}
		            }
            }
			usleep(1635);
        }
        delete [] buffer;
        buffer = nullptr;
    }
    fclose(wavFile);
    pthread_exit(NULL);
    return NULL;
}




/*void *thread_receiveFile(void* recvFile_args){
	struct recvFile_struct *args = (struct recvFile_struct*)recvFile_args;
    int clientSocket = args -> clientSocket;
    epoll_event *ee = args -> ee;
    int8_t* buffer = new int8_t[BUFFER_SIZE + 1];
    int bytesRead = 0;
    int receiving = 1;
    int command = 0;
    unsigned int fileSize = 0;
    //int8_t* musicBuffer = NULL;
    int offset = 0;
    const char* filePath = "ReceivedMusic.wav";
    FILE* wavFile = fopen(filePath, "w");
    if (wavFile == nullptr)
    {
        fprintf(stderr, "Unable to open wave file: %s\n", filePath);
        return 0;
    }

	cout << "Started receiving file from " << clientSocket << endl;
	
	buffer[0] = 105;
	if(write(clientSocket, buffer, BUFFER_SIZE + 1) < 0){
		perror("Write command 105 error");
		pthread_exit(NULL);
		return NULL;
	}
	
    while(receiving) {
		if((bytesRead = read(clientSocket, buffer, BUFFER_SIZE + 1)) > 0){ //TODO: Add timeout
			command = (int)*(buffer);
			switch (command){
					case 110: //Read 110
						cout << "Received music name: ";
						for(int i = 0; i < BUFFER_SIZE; i++){
							cout << (char)*(buffer + i);
						}
						cout << endl;
					break;
					
					case 111: //Read 111
						fileSize = (fileSize << 8) + (unsigned char)*(buffer);
						fileSize = (fileSize << 8) + (unsigned char)*(buffer + 1);
						fileSize = (fileSize << 8) + (unsigned char)*(buffer + 2);
						fileSize = (fileSize << 8) + (unsigned char)*(buffer + 3);
						cout << "Received music size " << fileSize << endl;
						//musicBuffer = new int8_t[fileSize];
					break;
					case 115:
						if(fwrite(subarray(buffer), sizeof *(buffer), bytesRead, wavFile) < 0){
							perror("Write to file error");
							pthread_exit(NULL);
							return NULL;
						}
					break;
			 		case 119:
						cout << "Finished receiving data from " << clientSocket
						<< ", received bytes: " << offset - 1 << endl;
						receiving = 0;
					break;
			}
		}
    }    
    epoll_ctl(args -> epollfd, EPOLL_CTL_ADD, clientSocket, ee);  
    pthread_exit(NULL);
    return NULL;
}*/

