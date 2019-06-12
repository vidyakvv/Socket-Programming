package edu.sjsu.net.slave;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;

public class GoogleConnect {

	public static void main(String[] args) throws Exception {
		Socket s = new Socket();
		String host = "www.google.com";
		PrintWriter s_out = null;
		BufferedReader s_in = null;

		try {
			s.connect(new InetSocketAddress(host, 80));
			System.out.println("Connected");

			// writer for socket
			s_out = new PrintWriter(s.getOutputStream(), true);
			// reader for socket
			s_in = new BufferedReader(new InputStreamReader(s.getInputStream()));
		} catch (UnknownHostException e) {
			// Host not found
			System.err.println("Don't know about host : " + host);
			System.exit(1);
		}

		// Send message to server
		String message = "GET /#q=hello HTTP/1.1\r\n\r\n";
		s_out.println(message);

		System.out.println("Message send");

		// Get response from server
		// String response;
		// while ((response = s_in.readLine()) != null) {
		// System.out.println(response);
		// }
		String response;
		response = s_in.readLine();
		while (response != null && !response.trim().equalsIgnoreCase("") ) {
			System.out.println(response);
			System.out.println("================");
			if(response.trim().equalsIgnoreCase("")){
				break;
			}
			response = s_in.readLine();
		}

	}

}
