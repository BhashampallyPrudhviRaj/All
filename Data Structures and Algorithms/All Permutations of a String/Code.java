import java.io.*;
public class Main
{
	public static void main(String[] args) throws IOException{
	    BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
	    String input = reader.readLine();
	    int size = input.length();
	    for(int index=size-1; index>0; index--)
	        size*=index;
	    permute(input,0,input.length()-1);
	}
	
	public static void permute(String input, int left, int right){
	    if(left==right){
	        System.out.print(input+" ");
	    }
	    else{
	        for (int index=left; index<=right; index++) {
	            input = swap(input, left, index);
	            permute(input, left+1, right);
	            input = swap(input,left,index);
	        }
	    }
	}
	
	public static String swap(String input, int index1, int index2){
	    char temp;
	    char[] array = input.toCharArray();
	    temp=array[index1];
	    array[index1] = array[index2];
	    array[index2] = temp;
	    return String.valueOf(array);
	}
}