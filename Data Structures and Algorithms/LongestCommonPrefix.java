package com.prudhvi;

import java.util.Scanner;

/**
 * Author: Prudhvi
 * Created on: 03-03-2022
 *
 * This program finds the longest common prefix among the given strings
 */
public class LongestCommonPrefix {

    public String longestCommonPrefix(String[] strings){
        if(0 == strings.length)
            return "";
        else{
            int n = strings[0].length();
            for(int i=0; i<n; i++){
                for(int j=1; j<strings.length; j++)
                if(strings[j].length()<=i || strings[j].charAt(i)!=strings[0].charAt(i))
                    return strings[0].substring(0,i);
            }
        }
        return strings[0];
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        int testcases = scanner.nextInt();
        if(testcases>0) {
            scanner.nextLine();
            LongestCommonPrefix longestCommonPrefix = new LongestCommonPrefix();
            while ((testcases--)!=0) {
                String[] strings = scanner.nextLine().split(" ");
                System.out.println(longestCommonPrefix.longestCommonPrefix(strings));
            }
        }
    }
}
