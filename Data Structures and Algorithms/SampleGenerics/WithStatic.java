package com.prudhvi;

/**
 * Author: Prudhvi
 * Created on: 08-03-2022
 *
 * Generics are used to allow any type (Integer, String, Double,... and user-defined types) to be passed as parameters to methods, classes and Interfaces
 */

public class WithStatic {

    public static <T> void print(T argument){
        System.out.println("Argument passed is: "+argument+" of type: "+argument.getClass().getSimpleName());
    }

    public static void main(String[] args) {
        print("Hello World!");
        print(10);
        print(30.5);
        print('A');
    }
}
