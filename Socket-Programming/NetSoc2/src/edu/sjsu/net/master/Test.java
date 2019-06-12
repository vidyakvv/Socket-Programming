package edu.sjsu.net.master;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;


public class Test {

	public static void main(String[] args) throws IOException {
		ServerSocket serverSocket = new ServerSocket(8080);
		Socket s = serverSocket.accept();
		

	}

}
