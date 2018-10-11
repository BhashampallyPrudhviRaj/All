#include<stdio.h>
int main(){
	int a[10],b[10],n,s0,s1,s2,i,j,d,p;
	n=4;
	printf("enter 4 data bits :");	
	for(i=0;i<n;i++)				
        scanf("%d",&a[i]);          
	a[5]=a[2]^a[1]^a[0];
	a[6]=a[3]^a[2]^a[1];
	a[4]=a[3]^a[0]^a[2];	
	printf("Transmission data is : ");
	for(i=0;i<n+3;i++){
        printf("\t%d\t",a[i]);
        b[i]=a[i];
	}
	printf("\ndo you want to modify data press '1' if yes else '2' ");
	scanf("%d",&d);
	if(d==1){
		printf("enter position of data you want to modify : ");
		scanf("%d",&p);
		if(b[p]==1)
        	b[p]=0;
		else
        	b[p]=1;
	}  
	printf("data is sent to receiver\n");
	for(i=0;i<n+3;i++)
        printf("%d",b[i]);
    					
	s2=b[0]^b[3]^ b[2]^b[4];
	s1=b[0]^b[1]^b[2]^b[5];
	s0=b[1]^b[2]^b[3]^b[6];
	printf("\nsyndromes are %d%d%d \n",s0,s1,s2);
	if(s2==0&&s1==0&&s0==0)
        printf("\nno error");
	else if(s2==0&&s1==0&&s0==1)
        printf("data modified at q0 ");
	else if(s2==0&&s1==1&&s0==0)
        printf("data modified at q1 ");
	else if(s2==1&&s1==0&&s0==0)
        printf("data modified at q2 ");
	else if(s2==0&&s1==1&&s0==1)
        printf("data modified at b2 ");
	else if(s2==1&&s1==0&&s0==1)
        printf("data modified at b0 ");
	else if(s2==1&&s1==1&&s0==0)
        printf("data modified at b3");
	else if(s2==1&&s1==1&&s0==1)
        printf("data modified at b1");
	printf("\n");
}