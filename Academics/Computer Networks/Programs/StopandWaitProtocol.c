#include <stdio.h>
#include <stdlib.h>
int main(){
	int send[10],rec[10],n,i,j,ack=0,frame=1,index=0;
	printf("How many frames you want to send : ");
	scanf("%d",&n);
	while(n>0){
		if (frame > n)
			break;
		send[index] = frame;
		rec[index] = send[index];
		frame++;
		index++;
		printf("\nSending frame %d",send[index]);
		printf("\nReceived frame %d",rec[index]);
		printf("\ndo you want to send Acknowledgement, if yes press 1 else 2 : ");
		scanf("%d",&ack);
		if (ack == 1)
		{
			continue;
			n--;
		}
		else{
			while(ack == 2){
				
				printf("\ndo you want to send Acknowledgement, if yes press 1 else 2 :");
				scanf("%d",&ack);
				if (ack == 1)
					break;
			}
		}
	}
	printf("\nAll Frames are sent\n");
}