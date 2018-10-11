#include<stdio.h>
int main(){
    int n,i,j,k,d[10][10],src,des,m=0;
    printf("Enter no. of nodes : ");
    scanf("%d",&n);
    for(i=0; i<n; i++){
        for(j=0; j<n; j++){
                printf("\nDistance between %c and %c : ",(65+i),(65+j));
                scanf("%d",&d[i][j]);
        }
    }
    printf("\nEnter Source : ");
    scanf("%d",&src);
    printf("\nEnter Destination : ");
    scanf("%d",&des);
    for(i=0; i<n; i++){
        for(j=0; j<n; j++){
            for(k=0; k<n; k++){
                if (d[i][k]!=0 && d[k][j]!=0){
                    m = d[i][k]+d[k][j];
                    if (m < d[i][j]){
                        d[i][j] = m;
                        if (i==src && j==des)
                            printf("Distance between %c to %c via %c : %d\n",(65+i),(65+j),(65+k),d[i][j] );
                    }
                    else{
                        if (i==src && j==des)
                            printf("Distance between %c and %c : %d\n",(65+i),(65+j),d[i][j] );
                        break;
                    }
                }
            }
        }
    }
}