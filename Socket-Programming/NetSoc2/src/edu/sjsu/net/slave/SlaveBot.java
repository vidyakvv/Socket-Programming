package edu.sjsu.net.slave;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SlaveBot {

	public static void main(String[] args) {

		SlaveBot slv = new SlaveBot();
		// Validate inputs & create connection to server
		slv.processCliRequest(args);

	}

	private static int HOST_OPT_POS = 0;
	private static int IP_HOST_POS = 1;
	private static int PORT_OPT_POS = 2;
	private static int PORT_POS = 3;

	/*
	 * private static final String IPADDRESS_PATTERN =
	 * "^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
	 * "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
	 * "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
	 * "([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";
	 */

	/**
	 * Print usage
	 */
	private void printUsage() {
		System.err.println("Invalid argument!");
		System.err.println("-h serverhost|serverip -p serverport");
	}

	/**
	 * 
	 * @param args
	 */
	private void processCliRequest(String[] args) {

		// -h localhost -p 6088
		if (isValid(args)) {

			String serverIpOrHost = args[IP_HOST_POS];
			int serverPort = Integer.parseInt(args[PORT_POS]);

			makeConnection(serverIpOrHost, serverPort);

		} else {
			printUsage();
		}
	}

	/**
	 * Creates connection with server and returns the connected socket
	 * 
	 * @param serverIpOrHost
	 * @param serverPort
	 * @param keepAlive
	 * @return clientSocket it can be null if in case connection is not
	 *         successful
	 */
	public Socket makeConnection(String serverIpOrHost, int serverPort) {
		Socket clientSoc = null;
		try {
			clientSoc = new Socket(serverIpOrHost, serverPort);

			// Check if the connection is still alive
			// https://stackoverflow.com/questions/10240694/java-socket-api-how-to-tell-if-a-connection-has-been-closed/10241044#10241044
			/*InputStream in = clientSoc.getInputStream();
			while (in.read() != -1) {
				System.out.println("Still connected..");
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			System.out.println("Connnection disconnected");
			*/

		} catch (IOException e) {
			System.err.println("Error connecting to server : " + e);
		}
		return clientSoc;
	}

	
	/**
	 * Creates connection with server and returns the connected socket
	 * 
	 * @param serverIpOrHost
	 * @param serverPort
	 * @param keepAlive
	 * @return clientSocket it can be null if in case connection is not
	 *         successful
	 */
	public Socket makeConnection(String serverIpOrHost, int serverPort,
			boolean keepAlive) {
		Socket clientSoc = null;
		try {
			clientSoc = new Socket(serverIpOrHost, serverPort);

			// set keep alive only if it is true
			if (keepAlive) {
				clientSoc.setKeepAlive(keepAlive);
			}

			// Check if the connection is still alive
			// https://stackoverflow.com/questions/10240694/java-socket-api-how-to-tell-if-a-connection-has-been-closed/10241044#10241044
			/*InputStream in = clientSoc.getInputStream();
			while (in.read() != -1) {
				System.out.println("Still connected..");
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			System.out.println("Connnection disconnected");
			 */

		} catch (IOException e) {
			System.err.println("Error connecting to server : " + e);
		}
		return clientSoc;
	}

	
	/**
	 * Creates multiple connection with server and returns the connected socket
	 * 
	 * @param serverIpOrHost
	 * @param serverPort
	 * @param slaveIpOrHost
	 * @param noOfConnections
	 * @param keepAlive
	 * @return
	 */
	public List<Socket> makeConnection(String serverIpOrHost, int serverPort,
			String slaveIpOrHost, int noOfConnections) {

		List<Socket> socList = new ArrayList<Socket>();
		for (int i = 0; i < noOfConnections; i++) {
			Socket soc = makeConnection(serverIpOrHost, serverPort);
			socList.add(soc);
		}

		return socList;

	}
	
	/**
	 * Creates multiple connection with server and returns the connected socket
	 * 
	 * @param serverIpOrHost
	 * @param serverPort
	 * @param slaveIpOrHost
	 * @param noOfConnections
	 * @param keepAlive
	 * @return
	 */
	public List<Socket> makeConnection(String serverIpOrHost, int serverPort,
			String slaveIpOrHost, int noOfConnections, boolean keepAlive) {

		List<Socket> socList = new ArrayList<Socket>();
		for (int i = 0; i < noOfConnections; i++) {
			Socket soc = makeConnection(serverIpOrHost, serverPort, keepAlive);
			socList.add(soc);
		}

		return socList;

	}

	public List<Socket> makeConnection(String serverIpOrHost, int serverPort,
			String slaveIpOrHost, int noOfConnections, String serverQuery) {

		String urlQuery = getQueryString(serverQuery);

		List<Socket> socList = new ArrayList<Socket>();
		for (int i = 0; i < noOfConnections; i++) {

			Socket soc = makeConnection(serverIpOrHost, serverPort, urlQuery);
			socList.add(soc);
		}

		return socList;

	}

	private String getQueryString(String serverQuery) {
		String urlQuery = "";
		Pattern pattern = Pattern.compile("url=(.*)");
		Matcher matcher = pattern.matcher(serverQuery);
		if (matcher.matches()) {
			urlQuery = matcher.group(1);
		}
		return urlQuery;
	}

	/**
	 * Pattern matches to check if the string is an ip or not
	 * 
	 * @param slaveIpOrHost
	 * @return
	 */
	/*
	 * private boolean isIP(String slaveIpOrHost) { return
	 * slaveIpOrHost.matches(IPADDRESS_PATTERN); }
	 */

	private Socket makeConnection(String serverIpOrHost, int serverPort,
			String serverQuery) {
		Socket clientSoc = null;
		try {
			clientSoc = new Socket(serverIpOrHost, serverPort);

			// Connect to server and post query to server
			queryServer(clientSoc, serverQuery);

			// Check if the connection is still alive
			// https://stackoverflow.com/questions/10240694/java-socket-api-how-to-tell-if-a-connection-has-been-closed/10241044#10241044
			/*InputStream in = clientSoc.getInputStream();
			while (in.read() != -1) {
				System.out.println("Still connected..");
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			System.out.println("Connnection disconnected");
			*/

		} catch (IOException e) {
			System.err.println("Error in I/O to server : " + e);
		}
		return clientSoc;

	}

	private void queryServer(Socket clientSoc, String serverQuery)
			throws IOException {

		PrintWriter out = null;
		BufferedReader in = null;
		clientSoc.setSoTimeout(1000);

		// writer for socket
		out = new PrintWriter(clientSoc.getOutputStream(), true);
		// reader for socket
		in = new BufferedReader(new InputStreamReader(
				clientSoc.getInputStream()));

		// Send message to server
		String message = "GET /" + serverQuery + " HTTP/1.1\r\n\r\n";
		out.println(message);

		//System.out.println("Message sent");

		// Get response from server if needed!
		try {
			String response;
			response = in.readLine();
			//If you need to read response
			/*
			while (response != null && response != "") {
				System.out.println(response);
				if(response.trim().equalsIgnoreCase("")){
					break;
				}
				response = in.readLine();
			}*/
			System.out.println("Response =" + response);
		} catch (SocketTimeoutException e) {
			//System.err.println("Sorry no more data from server to read");
		}
		
	}

	/**
	 * Validates command line parameters to be in the format -h localhost -p
	 * 6088
	 * 
	 * @param args
	 * @return
	 */
	private boolean isValid(String[] args) {
		// Assumption.. Slave will take argument like below
		// -h "ipAddress|hostName" -p "port"
		// -h localhost -p 6088
		if (args.length == 4) {
			if ("-h".equalsIgnoreCase(args[HOST_OPT_POS])
					&& "-p".equals(args[PORT_OPT_POS])) {
				// Make sure the port number is actually a number
				try {
					Integer.parseInt(args[PORT_POS]);
				} catch (Exception e) {
					return false;
				}

				return true;
			}
		}
		return false;
	}

}
