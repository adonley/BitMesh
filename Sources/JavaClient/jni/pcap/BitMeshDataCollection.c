#include <stdio.h>
#include <pcap.h>
#include <arpa/inet.h>
#include <string.h>
#include <ifaddrs.h>
#include <jni.h>
#include <pthread.h>
#include "bitmesh_bitmeshmicropayments_jni_pcap_BitMeshDataCollection.h"
//#include "BitMeshDataCollection.h"

/* ethernet headers are always exactly 14 bytes [1] */
#define SIZE_ETHERNET 14
#define SIZE_WIFI 30
/* Ethernet addresses are 6 bytes */
#define ETHER_ADDR_LEN	6

#define MAX_CONNECTIONS 10

#define HOST_OR_LENGTH sizeof("host or ")
#define MAX_ADDR_LENGTH 64
#define MAX_FILTER_LENGTH (MAX_ADDR_LENGTH + HOST_OR_LENGTH)
#define PCAP_BUFFER_SIZE 2097152


struct client_data_count
{
	char client[MAX_ADDR_LENGTH];
	unsigned long up;
	unsigned long down;
	unsigned long delta;
} client_data_count;

/* Ethernet header */
struct sniff_ethernet {
    u_char  ether_dhost[ETHER_ADDR_LEN];    /* destination host address */
    u_char  ether_shost[ETHER_ADDR_LEN];    /* source host address */
	u_short ether_type;                     /* IP? ARP? RARP? et */
};

/* IP header */
struct sniff_ip {
        u_char  ip_vhl;                 /* version << 4 | header length >> 2 */
        u_char  ip_tos;                 /* type of service */
        u_short ip_len;                 /* total length */
        u_short ip_id;                  /* identification */
        u_short ip_off;                 /* fragment offset field */
        #define IP_RF 0x8000            /* reserved fragment flag */
        #define IP_DF 0x4000            /* dont fragment flag */
        #define IP_MF 0x2000            /* more fragments flag */
        #define IP_OFFMASK 0x1fff       /* mask for fragmenting bits */
        u_char  ip_ttl;                 /* time to live */
        u_char  ip_p;                   /* protocol */
        u_short ip_sum;                 /* checksum */
        struct  in_addr ip_src;
        struct  in_addr ip_dst;  /* source and dest address */
};
#define IP_HL(ip)               (((ip)->ip_vhl) & 0x0f)
#define IP_V(ip)                (((ip)->ip_vhl) >> 4)

/* TCP header */
typedef u_int tcp_seq;

struct sniff_tcp {
        u_short th_sport;               /* source port */
        u_short th_dport;               /* destination port */
        tcp_seq th_seq;                 /* sequence number */
        tcp_seq th_ack;                 /* acknowledgement number */
        u_char  th_offx2;               /* data offset, rsvd */
#define TH_OFF(th)      (((th)->th_offx2 & 0xf0) >> 4)
        u_char  th_flags;
        #define TH_FIN  0x01
        #define TH_SYN  0x02
        #define TH_RST  0x04
        #define TH_PUSH 0x08
        #define TH_ACK  0x10
        #define TH_URG  0x20
        #define TH_ECE  0x40
        #define TH_CWR  0x80
        #define TH_FLAGS        (TH_FIN|TH_SYN|TH_RST|TH_ACK|TH_URG|TH_ECE|TH_CWR)
        u_short th_win;                 /* window */
        u_short th_sum;                 /* checksum */
        u_short th_urp;                 /* urgent pointer */
};


struct client_data_count client_data[MAX_CONNECTIONS];
char packet_filter_expression[MAX_CONNECTIONS * MAX_FILTER_LENGTH];
struct bpf_program filter;
int num_clients = 0;
char localhost[MAX_ADDR_LENGTH];
pcap_t *handle = NULL;
pthread_t *looper;


void get_my_ip();
void print_client_data();
struct client_data_count* get_data_for_client(const char* client);
void handle_data_count(u_char *args, const struct pcap_pkthdr *header, const u_char *packet);
int add_client(const char* client);
int forget_client(const char* client);
int init_pcap();
int stop_pcap();
int create_bpf();
void* loop(void* args);
int test_add_forget_clients();
int ip_matches_string(struct in_addr addr1, char* string);


int test_add_forget_clients()
{
	char *dev, errbuf[PCAP_ERRBUF_SIZE];
	int success = 0;
	dev = pcap_lookupdev(errbuf);
	if (dev == NULL)
	{
		fprintf(stderr, "Couldn't find default device: %s\n", errbuf);
		return 2;
	}
	printf ("Device: %s\n", dev);
	get_my_ip(dev);
	handle = pcap_create(dev, errbuf);
	pcap_set_buffer_size(handle, PCAP_BUFFER_SIZE);
	if (pcap_activate(handle))
	{
		fprintf(stderr, "Couldn't activate sniffer: %s\n", pcap_geterr(handle));
		return 3;
	}

	char client[] = "de3.mullvad.net";
	add_client(client);
	success = num_clients == 1 && strncmp(client_data[0].client, client, MAX_ADDR_LENGTH) == 0;
	add_client("192.168.1.1");
	success &= num_clients == 2;	
	forget_client(client);
	success &= num_clients == 1 && strncmp(client_data[0].client, "192.168.1.1", MAX_ADDR_LENGTH) == 0;
	pcap_freecode(&filter);
	pcap_close(handle);
	return success;
}

int init_pcap()
{
	char *dev, errbuf[PCAP_ERRBUF_SIZE];
	dev = pcap_lookupdev(errbuf);
	if (dev == NULL)
	{
		fprintf(stderr, "Couldn't find default device: %s\n", errbuf);
		return 2;
	}
	printf ("Device: %s\n", dev);
	get_my_ip(dev);
	handle = pcap_create(dev, errbuf);
	if (pcap_set_buffer_size(handle, PCAP_BUFFER_SIZE))
	{
		fprintf(stderr, "Couldn't set buffer size: handle was already activated\n");
		return 3;
	}
	/*
	if (pcap_setnonblock(handle, 0, errbuf) == -1)
	{
		fprintf(stderr, "Couldn't set non-blocking: %s\n", errbuf);
		return 4;
	}
	printf("pcap_getnonblock: %d %s\n", pcap_getnonblock(handle, errbuf), errbuf);
	*/
	if (pcap_activate(handle))
	{
		fprintf(stderr, "Couldn't activate sniffer: %s\n", pcap_geterr(handle));
		return 5;
	}
	//printf("Localhost: %s\n", localhost);
	create_bpf();
	return 0;
//	pcap_loop(handle, -1, got_packet, NULL);
//	pthread_create(looper, NULL, loop, NULL);
//	printf("\n\n\n\n\n\n\n\n\n\n\n");
}

void* loop(void* args)
{
	pcap_loop(handle, -1, handle_data_count, NULL);
	//pcap_loop(handle, -1, got_packet, NULL);
	return NULL;
}


int create_bpf()
{
	int i;
	int index = 0;
	if (num_clients > 0)
	{
		//printf("Constructing bpf\n");
		index += snprintf(packet_filter_expression + index, MAX_FILTER_LENGTH, "host %s ", client_data[0].client);
		//printf("Packet filter expression: %s\n", packet_filter_expression);
		for (i = 1; i < num_clients; i++)
		{
			index += snprintf(packet_filter_expression + index, MAX_FILTER_LENGTH, "or host %s ", client_data[i].client);
			//printf("Packet filter expression: %s\n", packet_filter_expression);
		}		
	}
	packet_filter_expression[index] = '\0';

	if (index > MAX_FILTER_LENGTH * MAX_CONNECTIONS)
	{
		fprintf(stderr, "ERROR THIS SHOULD NEVER HAPPEN INDEX OVERFLOW\n");
		return 1;
	}
	if (handle == NULL)
	{
		fprintf(stderr, "ERROR HANDLE MUST BE INITIALIZED BEFORE CALLING THIS\n");
		return 2;
	}

	if (pcap_compile(handle, &filter, packet_filter_expression, 1, PCAP_NETMASK_UNKNOWN) == -1)
	{
		fprintf(stderr, "Couldn't parse filter %s: %s\n", packet_filter_expression, pcap_geterr(handle));
		return 3;
	}
	if (pcap_setfilter(handle, &filter) == -1)
	{
		fprintf(stderr, "Couldn't install filter %s: %s\n", packet_filter_expression, pcap_geterr(handle));
		return 4;
	}
	printf("Packet filter expression: %s\n", packet_filter_expression);
	return 0;
}

int main(int argc, char *argv[])
{
	return init_pcap();
}

// counts data, returns whether or the client was new
void handle_data_count(u_char *args, const struct pcap_pkthdr *header, const u_char *packet)
{
	//print_client_data();
	const struct sniff_ip *ip = (struct sniff_ip*)(packet + SIZE_ETHERNET);
	int size_ip = IP_HL(ip)*4;
	const struct sniff_tcp *tcp = (struct sniff_tcp*)(packet + SIZE_ETHERNET + size_ip);
	int size_tcp = TH_OFF(tcp)*4;

	int payload_size = ntohs(ip->ip_len) - (size_ip + size_tcp);
	int i;
	for (i = 0; i < num_clients; i++)
	{

		if (ip_matches_string(ip->ip_src, client_data[i].client) && 
			ip_matches_string(ip->ip_dst, localhost))
		{
//			printf("Src: %s\n", inet_ntoa(ip->ip_src));
//			printf("Dst: %s\n", inet_ntoa(ip->ip_dst));
//			printf("adding to down: %d\n", payload_size);
			client_data[i].down += payload_size;
			client_data[i].delta = payload_size;
			return;
		} 
		else if (ip_matches_string(ip->ip_dst, client_data[i].client) && 
				 ip_matches_string(ip->ip_src, localhost))
		{

//			printf("Src: %s\n", inet_ntoa(ip->ip_src));
//			printf("Dst: %s\n", inet_ntoa(ip->ip_dst));
//			printf("adding to up: %d\n", payload_size);
			client_data[i].up   += payload_size;
			client_data[i].delta = payload_size;
			return;
		}
	}
	fprintf(stderr, "Was expecting only packets to or from ourselves. From: %s To: %s\n", inet_ntoa(ip->ip_src), inet_ntoa(ip->ip_dst) );
}

void print_client_data()
{
	int i;
	struct pcap_stat stats = {0};
	pcap_stats(handle, &stats);
	printf("packets received: %d\npackets dropped by buffer overflow (read them faster if this is high):%d\npackets droppped by network interface or its driver:%d\n", 
			stats.ps_recv, stats.ps_drop, stats.ps_ifdrop);
	for (i = 0; i < num_clients; i++)
	{

		printf("client: %s\nup:  %ld\ndown:%ld\ndelta:%ld\n\n", client_data[i].client, client_data[i].up, client_data[i].down, client_data[i].delta);
		//printf("up: %ld\ndown:%ld\n\n", get_data_for_client(client_data[i].client, 1), get_data_for_client(client_data[i].client, 0));
		//printf("\n", inet_ntoa(client_data[i].client));
	}
}

void get_my_ip(char * dev)
{
	struct ifaddrs * ifAddrStruct=NULL;
    struct ifaddrs * ifa=NULL;
    void * tmpAddrPtr=NULL;

    getifaddrs(&ifAddrStruct);

    for (ifa = ifAddrStruct; ifa != NULL; ifa = ifa->ifa_next) 
    {
        if (!ifa->ifa_addr) 
        {
            continue;
        }
        // if it is IP4
        if (ifa->ifa_addr->sa_family == AF_INET) 
        { 
            // is a valid IP4 Address
            tmpAddrPtr=&((struct sockaddr_in *)ifa->ifa_addr)->sin_addr;
            char addressBuffer[INET_ADDRSTRLEN];
            inet_ntop(AF_INET, tmpAddrPtr, addressBuffer, INET_ADDRSTRLEN);
            if (!strncmp(ifa->ifa_name, dev, MAX_ADDR_LENGTH))
            {
            	strncpy(localhost,addressBuffer, MAX_ADDR_LENGTH);
	            printf("%s IP Address %s\n", ifa->ifa_name, addressBuffer); 
            }
        } 
        // ipv6
        /*
        else if (ifa->ifa_addr->sa_family == AF_INET6) { // check it is IP6
            // is a valid IP6 Address
            tmpAddrPtr=&((struct sockaddr_in6 *)ifa->ifa_addr)->sin6_addr;
            char addressBuffer[INET6_ADDRSTRLEN];
            inet_ntop(AF_INET6, tmpAddrPtr, addressBuffer, INET6_ADDRSTRLEN);
            printf("%s IP Address %s\n", ifa->ifa_name, addressBuffer); 
        }
        */ 
    }
    if (ifAddrStruct!=NULL) freeifaddrs(ifAddrStruct);
}

// start counting data for client
int add_client(const char* client)
{
	if (num_clients == MAX_CONNECTIONS)
	{
		fprintf(stderr, "ERROR TOO MANY CLIENTS\n");
		return -1;
	}

	int i; 
	for (i = 0; i < num_clients; i++)
	{
		if (!strncmp(client, client_data[i].client, MAX_ADDR_LENGTH))
		{
			// move everybody below up
			fprintf(stderr, "ERROR CANNOT ADD CLIENT MORE THAN ONCE %s\n", client);
			return -1;
		}
	}

	strncpy(client_data[num_clients].client, client, MAX_ADDR_LENGTH);
	client_data[num_clients].up    = 0;
	client_data[num_clients].down  = 0;
	client_data[num_clients].delta = 0;
	num_clients++;
	return create_bpf();
}

// stop counting data for a client
int forget_client(const char* client)
{
	int i; 
	for (i = 0; i < num_clients; i++)
	{
		printf("comparing %s to %s\n", client, client_data[i].client);
		if (!strncmp(client, client_data[i].client, MAX_ADDR_LENGTH))
		{
			// move everybody below up
			memmove(&client_data[i], &client_data[i+1], (num_clients - i - 1) * sizeof(client_data_count));
			num_clients--;
			return create_bpf();
		}
	}
	fprintf(stderr, "ERROR CANNOT FORGET UNKNOWN CLIENT %s\n", client);
	return -1;
}

struct client_data_count* get_data_for_client(const char* client)
{
	int i;
	for (i = 0; i < num_clients; i++)
	{
		if (!strncmp(client, client_data[i].client, MAX_ADDR_LENGTH))
		{
			return &client_data[i];
		}
	}
	return NULL;
}

int stop_pcap()
{
	if (handle != NULL)
	{
		pcap_breakloop(handle);
		pcap_freecode(&filter);
		pcap_close(handle);
		/*
		if (looper != NULL)
		{
			pthread_join(*looper, NULL);
		}
		*/
		return 0;
	}
	else
	{
		fprintf(stderr, "Cannot stop pcap, it is not running\n");
		return 1;
	}
}

int ip_matches_string(struct in_addr addr1, char* string)
{
	char str_addr1[MAX_ADDR_LENGTH];
	char *char_addr1 = str_addr1;
	char *char_addr2 = string;
	inet_ntop(AF_INET, &addr1, str_addr1, MAX_ADDR_LENGTH);

	int i;
	for (i = 0; i < MAX_ADDR_LENGTH; i++, char_addr1++, char_addr2++)
	{
		if (*char_addr1 == '\0' && *char_addr2 == '\0')
		{
			break;
		}
		if (*char_addr1 != *char_addr2)
		{
			return 0;
		}
	}
	return 1;	
}

int same_ip(struct in_addr addr1, struct in_addr addr2)
{
	char str_addr1[MAX_ADDR_LENGTH];
	char str_addr2[MAX_ADDR_LENGTH];
	char *char_addr1 = str_addr1;
	char *char_addr2 = str_addr2;
	inet_ntop(AF_INET, &addr1, str_addr1, MAX_ADDR_LENGTH);
	inet_ntop(AF_INET, &addr2, str_addr2, MAX_ADDR_LENGTH);

	int i;
	for (i = 0; i < MAX_ADDR_LENGTH; i++, char_addr1++, char_addr2++)
	{
		if (*char_addr1 == '\0' && *char_addr2 == '\0')
		{
			break;
		}
		if (*char_addr1 != *char_addr2)
		{
			return 0;
		}
	}
	return 1;
}

// ====================== START JNI FUNCTIONS ====================//

JNIEXPORT jint JNICALL Java_bitmesh_bitmeshmicropayments_jni_pcap_BitMeshDataCollection_initPcap
  (JNIEnv *env, jobject jobj)
{
  	return init_pcap();
}

JNIEXPORT void JNICALL Java_bitmesh_bitmeshmicropayments_jni_pcap_BitMeshDataCollection_pcapLoop
  (JNIEnv *env, jobject jobj)
{
	loop(NULL);
}


JNIEXPORT jint JNICALL Java_bitmesh_bitmeshmicropayments_jni_pcap_BitMeshDataCollection_stopPcap
  (JNIEnv *env, jobject jobj)
{
	return stop_pcap();
}


JNIEXPORT jint JNICALL Java_bitmesh_bitmeshmicropayments_jni_pcap_BitMeshDataCollection_addClient
  (JNIEnv *env, jobject jobj, jstring string) 
{
	const char *client = (*env)->GetStringUTFChars(env, string, 0);
	return add_client(client);
}

JNIEXPORT jint JNICALL Java_bitmesh_bitmeshmicropayments_jni_pcap_BitMeshDataCollection_forgetClient
  (JNIEnv *env, jobject jobj, jstring string)
{
	const char *client = (*env)->GetStringUTFChars(env, string, 0);
	return forget_client(client);	
}


JNIEXPORT jlong JNICALL Java_bitmesh_bitmeshmicropayments_jni_pcap_BitMeshDataCollection_getDataUpForClient
  (JNIEnv *env, jobject jobj, jstring string)
{
	const char *client = (*env)->GetStringUTFChars(env, string, 0);
	struct client_data_count *dat = get_data_for_client(client);
	if (dat)
	{
		return (jlong)dat->up;
	}	
	return 0;
}

JNIEXPORT jlong JNICALL Java_bitmesh_bitmeshmicropayments_jni_pcap_BitMeshDataCollection_getDataDownForClient
  (JNIEnv *env, jobject jobj, jstring string)
{
	const char *client = (*env)->GetStringUTFChars(env, string, 0);
	struct client_data_count *dat = get_data_for_client(client);
	if (dat)
	{
		return (jlong)dat->down;
	}	
	return 0;	
}

