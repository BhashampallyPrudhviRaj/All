package String_StringBuffer_StringBuilder;

/**
 * Author: Prudhvi
 * Created on: 05-03-2022
 *
 *
 *
 * String is immutable and whenever we do any operations on strings then new objects are created and old objects are collected by Garbage Collector.
 *
 * StringBuffer and StringBuilder works on the same object initially created without creating new objects.
 *
 * StringBuffer is thread-safe whereas StringBuilder is not thread-safe.
 *
 *
 *
 * This program calculates the speeds of String, StringBuffer and StringBuilder
 */

public class SpeedTest {
    public static void main(String[] args) {

        //code using String
        String string = new String("Hello ");    //can directly use: String string = "Hello ";
        long previousTime = System.currentTimeMillis();
        for(int i=0; i<10000; i++)
            string = string.concat("Prudhvi ");
        System.out.println("String speed: "+(System.currentTimeMillis()-previousTime)+" ms");

        //code using StringBuffer
        StringBuffer stringBuffer = new StringBuffer("Hello ");
        previousTime = System.currentTimeMillis();
        for(int i=0; i<10000; i++)
            stringBuffer = stringBuffer.append("Prudhvi ");
        System.out.println("StringBuffer speed: "+(System.currentTimeMillis()-previousTime)+" ms");

        //code using StringBuilder
        StringBuilder stringBuilder = new StringBuilder("Hello ");
        previousTime = System.currentTimeMillis();
        for(int i=0; i<10000; i++)
            stringBuilder = stringBuilder.append("Prudhvi ");
        System.out.println("StringBuilder speed: "+(System.currentTimeMillis()-previousTime)+" ms");
    }
}




/* Output:
String speed: 589 ms
StringBuffer speed: 4 ms
StringBuilder speed: 0 ms
*/