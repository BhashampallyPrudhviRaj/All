import java.io.*;
import java.net.*;
class tcpclient {
	public static void main(String[] args) throws Exception {
		
		String host = args[0];
		int port = Integer.parseInt(args[1]);
		try{
		Socket s = new Socket(host,port);
		while (true) {
			InputStreamReader isr = new InputStreamReader(System.in);
			BufferedReader br = new BufferedReader(isr);
			System.out.println("Enter Message : ");
			String msgtoserver = br.readLine();
			PrintStream bw = new PrintStream(s.getOutputStream());
			bw.println(msgtoserver);
			BufferedReader fromserver = new BufferedReader(new InputStreamReader(s.getInputStream()));
			String msgfromserver = fromserver.readLine();
			System.out.println("Server says : "+msgfromserver);
		}
	}
	catch (Exception e) {
		System.out.println("Error");
	}
	}
}