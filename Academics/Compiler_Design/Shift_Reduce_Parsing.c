#include<stdio.h>
#include<stdlib.h>
#include<string.h>
char stack[15],input_symbol[15],action[15],temp[2],temp2[2];
int input_ptr=0,string_ptr=0,len,i;
void check();
void main(){
	printf("\t Shift Reduce Parser \n Grammar \n E->E+E \n E->E/E \n E->E*E \n E->a/b \n Enter the input symbol : ");	
	gets(input_symbol);
	printf("\n\t Stack Implementation Table \n stack \t\t input_symbol \t\t action \n ______\t\t ______________ \t _______ ");
	printf("\n $ \t\t %s $ \t\t --",input_symbol);
	strcpy(action,"Shift ");
	temp[0]=input_symbol[input_ptr];
	temp[1]='\0';
	strcat(action,temp);
	len = strlen(input_symbol);
	for (i = 0; i < len	; i++)
	{
		stack[string_ptr]=input_symbol[input_ptr];
		stack[string_ptr+1]='\0';
		input_symbol[input_ptr]=' ';
		input_ptr++;
		printf("\n $ %s \t\t %s $ \t\t %s",stack,input_symbol,action);
		strcpy(action,"Shift ");
		temp[0]=input_symbol[input_ptr];
		temp[1]='\0';
		strcat(action,temp);
		check();
		string_ptr++;
	}
	string_ptr++;
	check();
}

void check(){
	int flag=0;
	temp2[0] = stack[string_ptr];
	temp2[1] = '\0';
	if ((!strcmp(temp2,"a")) || (!strcmp(temp2,"b")))
	{
		stack[string_ptr] = 'E';
		if (!strcmp(temp2,"a"))
			printf("\n $ %s \t\t %s $ \t\t E -> a",stack,input_symbol);
		else
			printf("\n $ %s \t\t %s $ \t\t E -> b",stack,input_symbol);
		flag = 1;
	}
	if ((!strcmp(temp2,"+")) || (!strcmp(temp2,"/")) || (!strcmp(temp2,"*")))
		flag = 1;
	if ((!strcmp(stack,"E+E")) || (!strcmp(stack,"E/E")) || (!strcmp(stack,"E*E"))){
		strcpy(stack,"E");
		string_ptr = 0;
		if (!strcmp(stack,"E+E"))
			printf("\n $ %s \t\t %s $ \t\t E->E+E",stack,input_symbol);		
		else
			if (!strcmp(stack,"E/E"))
				printf("\n $ %s \t\t %s $ \t\t E->E/E",stack,input_symbol );
			else
				printf("\n $ %s \t\t %s $ \t\t E->E*E",stack,input_symbol);
		flag = 1;
	}
	if (!strcmp(stack,"E") && input_ptr == len)
	{
		printf("\n $ %s \t\t %s $ \t\t Accept",stack,input_symbol);
		exit(0);
	}
	if (flag == 0)
	{
		printf("\n $ %s \t\t %s $ \t\t Reject",stack,input_symbol );
		exit(0);
	}
	return;
}