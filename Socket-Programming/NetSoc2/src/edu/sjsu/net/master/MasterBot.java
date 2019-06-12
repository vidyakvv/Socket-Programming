package edu.sjsu.net.master;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;
import java.util.StringTokenizer;

import edu.sjsu.net.slave.SlaveBot;

public class MasterBot {

	// private static String LIST_DELIM = "\t";
	private static String LIST_DELIM = " ";

	class ConnectionDetails {
		private SimpleDateFormat dateFormatter = new SimpleDateFormat(
				"YYYY-MM-dd");
		private Socket socket;
		private Date registrationDate;
		// Change it in main as well

		boolean markForRemoval = false;
		InetSocketAddress slaveAddress;
		InetAddress masterAddress;
		int masterPort;

		// Place holder
		// private Socket targetSocket;

		public ConnectionDetails(Socket socket) {
			super();

			// Use the setter so that the registration time is recorded
			setSocket(socket);

			// Init the address
			slaveAddress = (InetSocketAddress) socket.getRemoteSocketAddress();
			masterAddress = socket.getLocalAddress();
			masterPort = socket.getPort();
		}

		public Socket getSocket() {
			return socket;
		}

		public void setSocket(Socket socket) {
			setRegistrationDate(new Date());
			this.socket = socket;
		}

		private Date getRegistrationDate() {
			return registrationDate;
		}

		private void setRegistrationDate(Date registrationDate) {
			this.registrationDate = registrationDate;
		}

		public String getSlaveIp() {

			return slaveAddress.getAddress().getHostAddress();
		}

		public String getSlaveHostName() {

			return slaveAddress.getHostName();
		}

		public int getSlavePortNumber() {

			return slaveAddress.getPort();
		}

		public String getMasterHostName() {
			return masterAddress.getHostName();
		}

		public String getMasterIp() {
			return masterAddress.getHostAddress();
		}

		public int getMasterPort() {
			return masterPort;
		}

		public boolean isMarkForRemoval() {
			return markForRemoval;
		}

		public void setMarkForRemoval(boolean markForRemoval) {
			this.markForRemoval = markForRemoval;
		}

		@Override
		public String toString() {
			InetSocketAddress slaveAddress = (InetSocketAddress) socket
					.getRemoteSocketAddress();

			String slaveHostName = slaveAddress.getHostName();
			String ipAddress = slaveAddress.getAddress().getHostAddress();
			int sourcePortNumber = slaveAddress.getPort();
			String regDateFormatted = dateFormatter
					.format(getRegistrationDate());
			/*
			 * String serverHost = socket.getLocalAddress().getHostAddress();
			 * int serverPort = socket.getLocalPort();
			 */

			return slaveHostName + LIST_DELIM + ipAddress + LIST_DELIM
					+ LIST_DELIM + sourcePortNumber + LIST_DELIM + LIST_DELIM
					+ regDateFormatted;

		}

	}

	class HttpRequestHandler extends Thread {
		private Socket socket; // The accepted socket from the Webserver

		// Start the thread in the constructor
		public HttpRequestHandler(Socket s, String hrefURL) {
			socket = s;
			start();
		}

		// Read the HTTP request, respond, and close the connection
		public void run() {
			try {

				// Open connections to the socket
				BufferedReader in = new BufferedReader(new InputStreamReader(
						socket.getInputStream()));
				PrintStream out = new PrintStream(new BufferedOutputStream(
						socket.getOutputStream()));

				// Read filename from first input line "GET /filename.html ..."
				// or if not in this format, treat as a file not found.
				String s = in.readLine();
				// System.out.println(s); // Log the request

				// Attempt to serve the file. Catch FileNotFoundException and
				// return an HTTP error "404 Not Found". Treat invalid requests
				// the same way.
				String filename = "";
				StringTokenizer st = new StringTokenizer(s);

				try {

					// Parse the filename from the GET command
					if (st.hasMoreElements()
							&& st.nextToken().equalsIgnoreCase("GET")
							&& st.hasMoreElements())
						filename = st.nextToken();
					else
						throw new FileNotFoundException(); // Bad request

					// Append trailing "/" with "index.html"
					if (filename.endsWith("/"))
						filename += "index.html";

					// Remove leading / from filename
					while (filename.indexOf("/") == 0)
						filename = filename.substring(1);

					// Replace "/" with "\" in path for PC-based servers
					filename = filename.replace('/', File.separator.charAt(0));

					// Check for illegal characters to prevent access to
					// superdirectories
					if (filename.indexOf("..") >= 0
							|| filename.indexOf(':') >= 0
							|| filename.indexOf('|') >= 0)
						throw new FileNotFoundException();

					// If a directory is requested and the trailing / is
					// missing,
					// send the client an HTTP request to append it. (This is
					// necessary for relative links to work correctly in the
					// client).
					if (new File(filename).isDirectory()) {
						filename = filename.replace('\\', '/');
						out.print("HTTP/1.0 301 Moved Permanently\r\n"
								+ "Location: /" + filename + "/\r\n\r\n");
						out.close();
						return;
					}

					// Open the file (may throw FileNotFoundException)
					InputStream f = new FileInputStream(filename);

					// Determine the MIME type and print HTTP header
					String mimeType = "text/plain";
					if (filename.endsWith(".html") || filename.endsWith(".htm"))
						mimeType = "text/html";
					else if (filename.endsWith(".jpg")
							|| filename.endsWith(".jpeg"))
						mimeType = "image/jpeg";
					else if (filename.endsWith(".gif"))
						mimeType = "image/gif";
					else if (filename.endsWith(".class"))
						mimeType = "application/octet-stream";
					out.print("HTTP/1.0 200 OK\r\n" + "Content-type: "
							+ mimeType + "\r\n\r\n");

					// Send file contents to client, then close the connection
					byte[] a = new byte[4096];
					int n;
					while ((n = f.read(a)) > 0)
						out.write(a, 0, n);
					out.close();
				} catch (FileNotFoundException x) {
					out.println("HTTP/1.0 404 Not Found\r\n"
							+ "Content-type: text/html\r\n\r\n"
							+ "<html><head></head><body>" + filename
							+ " not found</body></html>\n");
					out.close();
				}
			} catch (IOException x) {
				System.out.println(x);
			}
		}
	}

	// Main
	public static void main(String[] args) {
		// Validate inputs & Start Server
		MasterBot master = new MasterBot(args);

		// try {
		// Start Java Shell
		master.commandLine();
		// } finally {
		// master.close();
		// }

	}

	private ServerSocket serverSocket;
	private ServerSocket httpServerSocket;
	private Scanner scanner;
	private List<String> listOfExitCommands;
	List<String> listOfValidCommands;
	private static String ALL = "all";
	private static String CMD_DELIM = " ";
	// Change it in ConnectionDetails as well

	// All connections
	List<ConnectionDetails> allConnections;

	private MasterBot() {
		// System.out.println("Default Constructor");
		listOfExitCommands = Arrays.asList("exit", "quit");
		listOfValidCommands = Arrays.asList("list", "connect", "disconnect",
				"rise-fake-url", "down-fake-url");
		allConnections = new ArrayList<ConnectionDetails>();

		// System.out.println("Connection established @ "+
		// dateFormatter.format(new Date()));

	}

	public MasterBot(String[] args) {

		// Calling the default constructor
		this();

		// System.out.println("Parameterized Constructor");
		// Check if the args are correct & connect!
		processRequest(args);

		// Start a thread to listen to connections
		listenToConnections();

	}

	/**
	 * Method to process user arugments from command line and create a
	 * ServerSocket
	 * 
	 * Application exits if the connection could not be established
	 * 
	 * @param args
	 */
	private void processRequest(String[] args) {

		if (args.length != 2) {
			// print usage
			printUsage();

			// Do not proceed since we do not have valid arguments
			System.exit(0);

		} else {

			// Validate commands if needed -- args[0] -- args[1]

			// Create a ServerSocket
			try {
				// TODO : Remove or comment
				System.out.println("Starting master on port : "
						+ Integer.parseInt(args[1]));

				serverSocket = new ServerSocket(Integer.parseInt(args[1]));

			} catch (NumberFormatException e) {
				System.err.println("Invalid PortNumber" + e);
				System.exit(1);
			} catch (IOException e) {
				System.err
						.println("Connection cannot be established to port : "
								+ args[1] + e);

				System.exit(1);
			}
		}

	}

	/**
	 * Print usage
	 */
	private void printUsage() {
		System.err.println("Invalid argument!");
		System.err.println("-p PortNumber");
	}

	/**
	 * Method to listen to new connections
	 * 
	 * @return
	 */
	private Thread listenToConnections() {
		// Create a thread to listen to new connections
		Thread connectionListener = new Thread() {
			public void run() {

				if (serverSocket != null) {
					while (true) {
						try {
							// Accept client connection
							Socket clientSoc = serverSocket.accept();
							// Register the connection
							allConnections
									.add(new ConnectionDetails(clientSoc));

						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

					}
				}
			}
		};

		// Master Server thread
		connectionListener.setName("MasterBot");

		// Start the thread
		connectionListener.start();

		return connectionListener;

	}

	/**
	 * Close out connections
	 */
	public void close() {
		try {
			System.out.println("Shutting down master at "
					+ serverSocket.getInetAddress().getHostAddress() + ":"
					+ serverSocket.getLocalPort());
			serverSocket.close();
		} catch (IOException e) {
			System.err.println("Server could not close connection gracefully!"
					+ e);

		}
	}

	/**
	 * Method to present a shell like interface to the user
	 */
	private void commandLine() {
		scanner = new Scanner(System.in);

		try {
			while (true) {

				// Waiting for a command to be entered
				System.out.print("> ");
				String command = scanner.nextLine();

				// Make sure the command is a valid one
				if (isValidCommand(command)) {
					// If the command entered is an exit command then exit for
					// the interface
					if (isExitCommand(command)) {
						// close connections & exit
						close();
						System.exit(0);
					} else {

						/********* Call appropriate method to do the thing ********/
						// TODO: Remove or comment this line -- only for debug
						// System.out.println("Valid command!");

						String[] commands = command.split(CMD_DELIM);

						switch (commands[0]) {
						case "list":
							list();
							break;
						case "connect":
							connect(command);
							break;
						case "disconnect":
							disconnect(command);
							break;
						case "rise-fake-url":
							startFakeWebsite(command);
							break;
						case "down-fake-url":
							stopFakeWebsite(command);
							break;
						}

					}
				} else {

					printShellUsage(command);
				}
			}

		} finally {
			scanner.close();
		}

	}

	private void stopFakeWebsite(String command) {

		String[] commands = command.split(CMD_DELIM);
		int userPortNumber = Integer.parseInt(commands[1]);

		if (httpServerSocket != null) {
			int localPort = httpServerSocket.getLocalPort();
			if (localPort == userPortNumber) {
				try {
					System.out.println("Shutting down http server at "
							+ httpServerSocket.getInetAddress()
									.getHostAddress() + ":"
							+ httpServerSocket.getLocalPort());

					httpServerSocket.close();
					httpServerSocket = null;
				} catch (IOException e) {
					System.err
							.println("HTTPServer could not close connection gracefully!"
									+ e);
				}
			}

		}

	}

	private void startFakeWebsite(String command) {
		// Get port number from command
		String[] commands = command.split(CMD_DELIM);

		if (isFakeWebCommandValid(commands)) {
			int port_number = Integer.parseInt(commands[1]);
			String hrefURL = commands[2];

			try {
				httpServerSocket = new ServerSocket(port_number);

				// Create a thread to listen to new connections
				Thread httpConnectionListener = new Thread() {
					public void run() {

						if (httpServerSocket != null) {
							while (true && httpServerSocket != null) {
								try {
									// Accept client connection
									Socket httpSoc = httpServerSocket.accept();
									new HttpRequestHandler(httpSoc, hrefURL);

								} catch (IOException e) {

								}

							}
						}
					}
				};

				// Master Server thread
				httpConnectionListener.setName("HttpListenerThread");

				// Start the thread
				httpConnectionListener.start();

			} catch (IOException e) {
				e.printStackTrace();
			}

		}

	}

	private boolean isFakeWebCommandValid(String[] commands) {
		// If the commands is null
		if (commands == null || commands.length != 3) {
			return false;
		} else {
			return isValidPort(commands[1]);
		}
	}

	/**
	 * Method simply list all the connections in a specific format
	 * 
	 * @param command
	 *            TODO : Replace /t by <spaces> if needed
	 */
	private void list() {

		// If there are no connections
		if (allConnections == null || allConnections.isEmpty()) {
			System.out.println("Nothing connection to list");
			return;
		}

		// If there are connections
		System.out.println("SlaveHostName" + LIST_DELIM + "IPAddress"
				+ LIST_DELIM + "SourcePortNumber" + LIST_DELIM
				+ "RegistrationDate");

		for (ConnectionDetails connectionDetails : allConnections) {

			// toString does the formmating for this output
			System.out.println(connectionDetails);

		}

	}

	/**
	 * Method to connect
	 * 
	 * @param command
	 */
	private void connect(String command) {

		String[] commands = command.split(CMD_DELIM);

		// connect slave_ip_h target_ip_h target_port <no_of_connections>
		if (isConnectValid(command)) {

			// Collect all the inputs
			String slaveIpH = commands[1];
			String targetIpH = commands[2];
			int targetPort = Integer.parseInt(commands[3]);
			int no_of_connections = 1;
			boolean keepAlive = false;
			String url = "";
			SlaveBot slv = new SlaveBot();
			try {

				int noOfArgs = commands.length;
				if (noOfArgs == 6) {
					// no_of_connections given + keepalive/url
					no_of_connections = Integer.parseInt(commands[4]);
					if (commands[5].equalsIgnoreCase("keepalive")) {
						// keepalive
						keepAlive = true;
						slv.makeConnection(targetIpH, targetPort, slaveIpH,
								no_of_connections, keepAlive);
					} else if (commands[5].matches(".*url.*")) {
						// url=path-to-be-provided-to-web-server
						url = commands[5];
						slv.makeConnection(targetIpH, targetPort, slaveIpH,
								no_of_connections, url);
					}

				} else if (noOfArgs == 5
						&& (commands[4].equalsIgnoreCase("keepalive") || commands[4]
								.matches(".*url.*"))) {
					// 5 args + keepalive or url

					if (commands[4].equalsIgnoreCase("keepalive")) {
						// keepalive
						keepAlive = true;
						slv.makeConnection(targetIpH, targetPort, slaveIpH,
								no_of_connections, keepAlive);
					} else if (commands[4].matches(".*url.*")) {
						// url=path-to-be-provided-to-web-server
						url = commands[4];
						slv.makeConnection(targetIpH, targetPort, slaveIpH,
								no_of_connections, url);
					}

				} else if (noOfArgs == 5
						&& !(commands[4].equalsIgnoreCase("keepalive") || commands[4]
								.matches(".*url.*"))) {
					// 5 args + no (keepalive or url)
					no_of_connections = Integer.parseInt(commands[4]);
					slv.makeConnection(targetIpH, targetPort, slaveIpH,
							no_of_connections);
				} else if (noOfArgs < 5) {
					// less than 5 args
					slv.makeConnection(targetIpH, targetPort, slaveIpH,
							no_of_connections);
				}

			} catch (Exception e) {
				System.err.println("Some exception occured " + e);
			}

		} else {
			// TODO: Remove or comment this line
			System.err.println("Invalid connect command");
			System.err
					.println("connect slave_ip_h target_ip_h target_port <no_of_connections>");
		}

	}

	/**
	 * Method to check if the connect command is correct
	 * 
	 * @param command
	 * @return
	 */
	private boolean isConnectValid(String command) {
		// connect slave_ip_h target_ip_h target_port <no_of_connections>
		String[] commands = command.split(CMD_DELIM);

		// If the commands is null
		if (commands == null) {
			return false;
		} else {

			if (commands.length == 4) {
				// Check port number
				return isValidPort(commands[3]);

			} else if (commands.length == 5 || commands.length == 6) {
				// no_of_connections AND or OR keepalive is provided
				return true;
			}
		}
		return false;
	}

	/**
	 * Method to disconnect
	 * 
	 * @param command
	 */
	private void disconnect(String command) {

		// disconnect slave_ip target_ip target_port
		String[] commands = command.split(CMD_DELIM);

		// Check if we have any registered connections to being with
		if (allConnections == null) {
			// TODO: remove or comment
			System.err.println("No connections to disconnect!");
			return;

		} else {

			// Check if the command entered is valid!
			if (isDisconnectValid(command)) {
				String slaveIpHostAllInp = commands[1];
				// Do we use the following
				String targetIpHostInp = commands[2];
				boolean usePortNumber = false;
				int targetPortNumberInp = 0;

				// port number provided
				if (commands.length == 4) {
					targetPortNumberInp = Integer.parseInt(commands[3]);
					usePortNumber = true;
				}

				/********* Iterate through the connection list *********/
				for (ConnectionDetails connectionDetails : allConnections) {

					// Disconnect the matching cases -- or alternatively create
					// a key using which you can disconnect directly

					if (slaveIpHostAllInp.equalsIgnoreCase(ALL)) {
						// If it is ALL disconnect all the connections
						disconnectSocket(connectionDetails);

					} else if (isDisconnectMatch(slaveIpHostAllInp,
							targetIpHostInp, usePortNumber,
							targetPortNumberInp, connectionDetails)) {

						// If the slave ip is a match disconnect the connections
						disconnectSocket(connectionDetails);
					}

				}

				// Remove all the marked sockets from the list
				removeMarkedElements();

			} else {
				// TODO: Remove or comment this line
				System.err.println("Invalid disconnect command");
				System.err
						.println("disconnect slave_ip target_ip <target_port>");
			}
		}

	}

	private boolean isDisconnectMatch(String slaveIpHostAllInp,
			String targetIpHostInp, boolean usePortNumber,
			int targetPortNumberInp, ConnectionDetails connectionDetails) {

		if ((slaveIpHostAllInp.equalsIgnoreCase(connectionDetails
				.getSlaveHostName()) || slaveIpHostAllInp
				.equalsIgnoreCase(connectionDetails.getSlaveIp()))
				&& (targetIpHostInp.equalsIgnoreCase(connectionDetails
						.getMasterHostName()) || targetIpHostInp
						.equalsIgnoreCase(connectionDetails.getMasterIp()))
				&& usePortNumber
				&& targetPortNumberInp == connectionDetails
						.getSlavePortNumber()) {

			// If the following Match
			// 1. Slave IP or Host
			// 2. Master IP or Host
			// 3. Slave Port

			return true;

		} else if ((slaveIpHostAllInp.equalsIgnoreCase(connectionDetails
				.getSlaveHostName()) || slaveIpHostAllInp
				.equalsIgnoreCase(connectionDetails.getSlaveIp()))
				&& (targetIpHostInp.equalsIgnoreCase(connectionDetails
						.getMasterHostName()) || targetIpHostInp
						.equalsIgnoreCase(connectionDetails.getMasterIp()))
				&& !usePortNumber) {

			// If the following Match
			// 1. Slave IP or Host
			// 2. Master IP or Host
			// Meaning delete all the port numbers matching the above master &
			// slave
			return true;
		}
		return false;
	}

	/**
	 * Iterate and remove the elements in the list
	 */
	private void removeMarkedElements() {

		for (Iterator<ConnectionDetails> conIterator = allConnections
				.iterator(); conIterator.hasNext();) {
			if (conIterator.next().markForRemoval) {
				conIterator.remove();
			}
		}

	}

	/**
	 * Method to check if the command entered is valid for disconnect
	 * 
	 * @param command
	 * @return
	 */
	private boolean isDisconnectValid(String command) {
		String[] commands = command.split(CMD_DELIM);

		// If the commands is null
		if (commands == null) {
			return false;
		} else {
			if (commands.length == 3) {
				// slave target
				return true;
			} else if (commands.length == 4) {
				// slave target target_port
				return isValidPort(commands[3]);
			} else {
				return false;
			}
		}

	}

	private boolean isValidPort(String portNumberStr) {
		try {
			Integer.parseInt(portNumberStr);
			return true;
		} catch (NumberFormatException e) {
			// TODO: Remove or comment this line
			System.err.println("Invalid port number : " + portNumberStr);
			return false;
		}
	}

	/**
	 * Method to try and disconnect the socket
	 * 
	 * @param connectionDetails
	 */
	private void disconnectSocket(ConnectionDetails connectionDetails) {
		try {
			connectionDetails.getSocket().close();
			connectionDetails.setMarkForRemoval(true);
		} catch (IOException e) {
			System.err.println("Connection termination failed : " + e);
		}
	}

	/**
	 * Method to print usage of shell
	 * 
	 * TODO : Make sure everything as per specification
	 */
	private void printShellUsage(String command) {
		System.err.println("Invalid command : " + command);
		System.err.println("Following are the supported commands");

		// Print the commands supported
		System.err.println("> list");
		System.err
				.println("> connect [SlaveIPAddress|Hostname] [TargetIPAddress|Hostname] [TargetPortNumber] [NoOfConnections|DFLT=1]");
		System.err
				.println("> connect [SlaveIPAddress|Hostname] [TargetIPAddress|Hostname] [TargetPortNumber] [NoOfConnections|DFLT=1]");
		System.err
				.println("> connect [SlaveIPAddress|Hostname] [TargetIPAddress|Hostname] [TargetPortNumber] [NoOfConnections|DFLT=1] [keepalive]");
		System.err
				.println("> connect [SlaveIPAddress|Hostname] [TargetIPAddress|Hostname] [TargetPortNumber] [NoOfConnections|DFLT=1] [url=path-to-be-provided-to-web-server]");
		System.err
				.println("> disconnect [SlaveIPAddress|Hostname|all] [TargetIPAddress|Hostname] [TargetPortNumber|DFLT=all]");
		System.err.println("> rise-fake-url [PortNumber] [URL]");
		System.err.println("> down-fake-url [PortNumber] [URL]");
		System.err.println("> exit | quit");
	}

	/**
	 * Method to identify an exit command
	 * 
	 * @param command
	 * @return
	 */
	private boolean isExitCommand(String command) {

		if (listOfExitCommands.contains(command)) {
			return true;
		}
		return false;
	}

	/**
	 * Method to identify a valid command
	 * 
	 * @param command
	 * @return
	 */
	private boolean isValidCommand(String command) {

		// Tokenize the string and check if the root is correct
		if (command == null) {
			return false;
		}
		String commands[] = command.split(CMD_DELIM);

		String rootCommand = commands[0];

		if (listOfValidCommands.contains(rootCommand)
				|| isExitCommand(rootCommand)) {
			return true;
		}

		return false;
	}

}
