import java.io.*;
public class Main
{
    public static void compute_lps(char[] pattern, int length, int[] lps){
        int first_index=0, second_index=1;
        lps[first_index]=0;
        while(second_index<length){
            if(pattern[first_index]==pattern[second_index]){
                first_index++;
                lps[second_index]=first_index;
                second_index++;
            }
            else{
                if(first_index>0)
                first_index=lps[first_index-1];
                else{
                    lps[second_index]=0;
                    second_index++;
                }
            }
        }
    }
    
	public static void main(String[] args) throws IOException{
	    BufferedReader b = new BufferedReader(new InputStreamReader(System.in));
	    char[] t = b.readLine().toCharArray();
	    char[] p = b.readLine().toCharArray();
	    int i=0, j=0, n=t.length, m=p.length;
	    int[] lps = new int[m];
	    compute_lps(p,m,lps);
	    while(i<n){
	        if(t[i]==p[j]){
	            i++;
	            j++;
	            if(j==m){
	                System.out.println(i-j);
	                j=lps[j-1];
	            }
	        }
	        else{
	            if(j==0){
	                i++;   
	            }
	            else{
	                j=lps[j-1];
	            }
	        }
	    }
	}
}