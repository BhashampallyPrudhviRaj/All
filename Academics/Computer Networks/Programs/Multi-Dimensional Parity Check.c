#include<stdio.h>
int main(){
        int m,d,i,j,k,l,c=0,b[10][10],a[10][10],n,r[10][10];
        printf("Enter no. of rows : ");
        scanf("%d",&m);
        printf("\nEnter no. of columns : ");
        scanf("%d",&n);
        printf("\nEnter data in %d rows and %d coulmns wise to avoid confuse : ",m,n);
        for(i=0; i<m; i++){
                for(j=0; j<n; j++)
                        scanf("%d",&a[i][j]);
        }
        for(i=0; i<m; i++){
                for(j=0; j<(n-1); j++){
                        a[i][j+n]=a[i][j%n] ^ a[i][(j+1)%n] ^ a[i][(j+2)%n];
                        a[m][j] =a[i%m][j] ^ a[(i+1)%m][j] ^ a[(i+2)%m][j];
                        a[m][n-1] = a[i%m][n-1] ^ a[(i+1)%m][n-1] ^ a[(i+2)%m][n-1];
                }
        }
        for(i=0; i<m; i++){
                for(j=0; j<(n-2); j++)
                        a[m][j+n]=a[i%m][n+j] ^ a[(i+1)%m][j+n] ^ a[(i+2)%m][j+n];
        }
        a[m][n+n-2] = a[m][0];
        for(j=1; j<(n+n-2); j++)
                a[m][n+n-2] = a[m][n+n-2] ^ a[m][j%(n+n-2)];
        printf("transmission data is : \n");
        for(i=0; i<=m; i++){
                for(j=0; j<(n+n-1); j++){
                        b[i][j]=a[i][j];
                        printf("%d\t",a[i][j]);
                }
                printf("\n");
        }
        printf("\n do you want to change bits , if yes press 1 else 2 : ");
        scanf("%d",&d);
        switch(d){
                case 1: printf("enter position of row and column : ");
                        scanf("%d %d",&k,&l);
                        if(a[k][l]==0)
                                a[k][l]=1;
                        else
                                a[k][l]=0;
                        printf("\n--------------there is error------------\n");
                        for(i=0; i<m; i++){
                                for(j=0; j<(n-1); j++){
                                        a[i][j+n]=a[i][j%n] ^ a[i][(j+1)%n] ^ a[i][(j+2)%n];
                                        a[m][j] =a[i%m][j] ^ a[(i+1)%m][j] ^ a[(i+2)%m][j];
                                        a[m][n-1] = a[i%m][n-1] ^ a[(i+1)%m][n-1] ^ a[(i+2)%m][n-1];
                                }
                        }
                        for(i=0; i<m; i++){
                                for(j=0; j<(n-2); j++)
                                        a[m][j+n]=a[i%m][n+j] ^ a[(i+1)%m][j+n] ^ a[(i+2)%m][j+n];
                        }
                        a[m][n+n-2] = a[m][0];
                        for(j=1; j<(n+n-2); j++)
                                a[m][n+n-2] = a[m][n+n-2] ^ a[m][j%(n+n-2)];                        
                        break;

                case 2: printf("\n--------------no error-------------\n");
                        break;
        }
        printf("received data is : \n");
        for(i=0; i<=m; i++){
                for(j=0; j<(n+n-1); j++){
                        r[i][j]=a[i][j];
                        printf("%d\t",r[i][j]);
                }
                printf("\n");
        }
        for(i=0; i<=m; i++){
                for(j=0; j<(n+n-1); j++){
                        if(b[i][j]!=r[i][j]){
                                printf("row is = %d and column is = %d\n",i,j);
                                c++;
                        }
                }
        }
        printf("changed positions are : %d\n",c);
}