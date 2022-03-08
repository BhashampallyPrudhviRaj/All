package com.prudhvi;

/**
 * Author: Prudhvi
 * Created on: 08-03-2022
 *
 * Generics are used to allow any type (Integer, String, Double,... and user-defined types) to be passed as parameters to methods, classes and Interfaces
 */

public class WithoutStatic {

    public <T> void print(T argument){
        System.out.println("Argument passed is: "+argument+" of type: "+argument.getClass().getName());
    }

    public static void main(String[] args) {
        WithoutStatic object = new WithoutStatic();
        object.print("Hi");
        object.print(50);
        object.print(45.879);
        object.print('b');
    }
}
