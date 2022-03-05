package String_StringBuffer_StringBuilder;

/**
 * Author: Prudhvi
 * Created on: 05-03-2022 
 */

import java.util.Scanner;

public class RunLengthEncoding {

    public String encoding(String string){
        StringBuffer encodedString = new StringBuffer();
        int count=1;
        for(int index=1; index<=string.length(); index++){
            if(index==string.length() || string.charAt(index)!=string.charAt(index-1)){
                encodedString.append(count).append(string.charAt(index-1));
                count=1;
            }
            else
                count++;
        }

        return encodedString.toString();
    }

    public static void main(String[] args) {

        Scanner scanner = new Scanner(System.in);
        String string = scanner.nextLine();
        RunLengthEncoding runLengthEncoding = new RunLengthEncoding();
        System.out.println(runLengthEncoding.encoding(string));
    }
}
