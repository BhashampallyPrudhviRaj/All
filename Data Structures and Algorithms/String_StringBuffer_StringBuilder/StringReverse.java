package String_StringBuffer_StringBuilder;

public class StringReverse {
    public static void main(String[] args) {

        //code using String
        String string = "Hello";
        char[] a = new char[string.length()];
        int i,j;
        for(i=0, j=string.length()-1; i<j; i++,j--){
            a[j] = string.charAt(i);
            a[i] = string.charAt(j);
        }
        a[j] = string.charAt(i);
        System.out.print("Code using String: ");
        for (char p: a) {
            System.out.print(p);
        }
        System.out.println();


        //code using StringBuffer
        StringBuffer stringBuffer = new StringBuffer(string);
        System.out.println("Code using StringBuffer: "+stringBuffer.reverse());


        //code using StringBuilder
        StringBuilder stringBuilder = new StringBuilder(string);
        System.out.println("Code using StringBuilder: "+stringBuilder.reverse());
    }
}
