import java.io.*;
import java.net.*;
class tcpserver {
	public static void main(String[] args) throws Exception {
		//String host = args[0];
		int port = Integer.parseInt(args[0]);
		try{			
		ServerSocket ss = new ServerSocket(port);
		Socket s = ss.accept();
		while (true) {
			BufferedReader fromclient = new BufferedReader(new InputStreamReader(s.getInputStream()));
			String msgfromclient = fromclient.readLine();
			System.out.println("Client says : "+msgfromclient);
			InputStreamReader isr = new InputStreamReader(System.in);
			BufferedReader br = new BufferedReader(isr);
			System.out.println("Enter Message : ");
			String msgtoclient = br.readLine();
			PrintStream bw = new PrintStream(s.getOutputStream());
			bw.println(msgtoclient);			
		}
	}
	catch (Exception e) {
		System.out.println("Error");
	}
	}
}