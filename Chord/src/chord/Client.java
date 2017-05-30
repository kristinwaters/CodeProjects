package chord;

/******************************************************************************
 * Filename: Client.java
 * 
 * Description:  This file contains the necessary functions to run the
 *               Client component of a decentralized publish and
 *               subscribe infrastructure.
 *
 * Author: Kristin Dahl
 * Date: 9-6-2016
 *****************************************************************************/

import static chord.ChordUtil.END;
import static chord.ChordUtil.HOST;
import static chord.ChordUtil.M;
import static chord.ChordUtil.MAX_NODES;
import static chord.ChordUtil.OK;
import static chord.ChordUtil.PRINT_DATA;
import static chord.ChordUtil.PUBLISH;
import static chord.ChordUtil.SEND_TAG;
import static chord.ChordUtil.SEND_URL;
import static chord.ChordUtil.SUBSCRIBE;
import static chord.ChordUtil.SUPERNODE_PORT_NUM;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class Client {
	private static final int CLIENT_PORT = 55372;

	private static Map<String, Integer> hashedTags;
	private static ArrayList<Integer>   nodeTrace;
	private static ServerSocket         serverSocket;
	private static ArrayList<String>    urlList;

	private static ClientUtil myUtil;

	public static void execute() {
		System.out.println("Welcome to the Chord-based publish/subscribe system.\n");

		try {
			serverSocket = new ServerSocket(CLIENT_PORT);
		} catch (Exception e) {}

		myUtil = new ClientUtil();
		urlList = new ArrayList<String>();
		nodeTrace = new ArrayList<Integer>(MAX_NODES);
		hashedTags = new HashMap<String, Integer>(50);
		
		printMenu();
	}

	
	/*
	 * Function name: printMenu()
	 * 
	 * Description: This function simply presents the user with a menu that is
	 * used to interact with the Chord system.
	 */
	public static void printMenu() {
		nodeTrace.clear();
		urlList.clear();

		System.out.println("********* Menu *********");
		System.out.println("Publish  ------  Press 1");
		System.out.println("Subscribe  ----  Press 2");
		System.out.println("View DHT  -----  Press 3");
		System.out.println("Exit  ---------  Press 4");
		System.out.println("************************");
		
		System.out.print("\nEnter a command: ");

		int command = 0;
		try {
			Scanner in = new Scanner(System.in);
			command = in.nextInt();
		} catch (Exception e) {}

		switch (command) {
		case 1:
			publish();
			myUtil.printNodeTrace(nodeTrace);
			break;
		case 2:
			subscribe();
			myUtil.printNodeTrace(nodeTrace);
			break;
		case 3:
			requestDHTData();
			break;
		case 4:
			System.out.println("Now exiting.  Bye!\n");
			System.exit(1);
		default:
			System.out.println("\nPlease enter a valid option.\n\n");
			break;
		}

		printMenu();
	}

	
	/*
	 * Function name: publish()
	 * 
	 * Description: This function gathers the necessary user input for the
	 * publish functionality and then calls the appropriate methods to send the
	 * data to the SuperNode. Then, it displays a message that lets the user
	 * know whether the operation succeeded.
	 */
	public static void publish() {
		System.out.println("\n\n------- Publish -------");

		boolean more = true;
		String tag = null;
		String url = null;
		String choice = null;
		int hashTag;
		Scanner in = new Scanner(System.in);

		// Get tag from user. Client must hash tags.
		System.out.print("Please enter a tag: ");
		tag = in.nextLine();

		// Get URLs from user
		while (more) {
			System.out.print("Please enter URL: ");
			url = in.nextLine();
			urlList.add(url);

			System.out.print("Add another URL? y/n: ");
			choice = in.nextLine();

			if (!choice.equalsIgnoreCase("y")) {
				more = false;
			}
		}

		// Determine if tag already exists. If not, it must be hashed and
		// added to the hashedTags table.
		if (hashedTags.containsKey(tag)) {
			hashTag = hashedTags.get(tag);

		} else {
			hashTag = myUtil.hashTag(tag);
			while (hashedTags.containsValue(hashTag)) {
				hashTag = myUtil.hashTag(tag);
			}
			hashedTags.put(tag, hashTag);
		}

		// Contact SuperNode with request to publish.
		System.out.println("\nHashed tag is: " + hashTag);
		requestPublish(hashTag);

		// Now wait for a response from the node.
		if (receivePublishResponse()) {
			System.out.println("Publish completed successfully!");

		} else {
			System.out.println("Unable to complete publish request.");
		}
	}
	

	/*
	 * Function name: subscribe()
	 * 
	 * Description: This function gathers the necessary user input for the
	 * subscribe functionality and then checks whether the user-entered tag is
	 * valid. If it is, then it forwards the tag to the SuperNode and displays
	 * the resulsts. If the tag is not valid, then it displays this message to
	 * the user.
	 */
	public static void subscribe() {
		System.out.println("\n\n------- Subscribe -------");
		System.out.print("Please enter a query: ");

		ArrayList<Integer> validTagList = new ArrayList<Integer>();

		String retVal[] = null;
		String query = null;
		Scanner in = new Scanner(System.in);
		Map<Integer, ArrayList<String>> tagsToURLs = new HashMap<Integer, ArrayList<String>>();
		ArrayList<String> queryURLs;
		
		// Get query from user.
		try {
			query = in.nextLine();
			retVal = myUtil.parseSubscribeQuery(query, hashedTags, validTagList);
		} catch (Exception e) {}

		// Determine whether user entered valid data.
		if (retVal == null) {
			System.out.println("\nInvalid query.\n\n");
			return;
		} else {
			if (validTagList.isEmpty()) {
				System.out.println("\nNo matching URLs were found.\n\n");
				return;
			}
			for (int i = 0; i < validTagList.size(); i++) {
				System.out.println("Hashed tag is: " + validTagList.get(i));
			}
		}

		// Contact SuperNode with request to subscribe.
		requestSubscribe(validTagList);

		// Now wait for a response from the node(s).
		for (int j = 0; j < validTagList.size(); j++) {
			tagsToURLs.put(j, receiveSubscribeResponse());
		}

		queryURLs = myUtil.matchQueryURLs(tagsToURLs, query);
		
		// Display results to user.
		if (queryURLs.size() == 0) {
			System.out.println("\nNo matching URLs were found.");
		} else {
			System.out.println("\nURLs found:");
			for (int i = 0; i < queryURLs.size(); i++) {
				System.out.println("- " + queryURLs.get(i));
			}
		}
	}

	/*
	 * Function name: requestPublish(int hashedTag)
	 * 
	 * Description: This function contacts the SuperNode with a request to
	 * publish a set of URLs. The input is the generated hashTag. The URLs to
	 * add are located in the global urlList array.
	 */
	public static void requestPublish(int hashedTag) {
		Socket connected = null;
		String fromclient = null;

		ObjectInputStream objectIn = null;
		ObjectOutputStream objectOut = null;

		try {
			connected = new Socket(HOST, SUPERNODE_PORT_NUM);
			objectIn  = new ObjectInputStream(connected.getInputStream());
			objectOut = new ObjectOutputStream(connected.getOutputStream());

			// Send PUBLISH Request
			objectOut.writeObject(PUBLISH);
			objectOut.flush();
			fromclient = (String) objectIn.readObject();

			System.out.println("Sending publish data...");

			// Send tag
			if (fromclient.equals(SEND_TAG)) {
				objectOut.writeInt(hashedTag);
				objectOut.flush();
				fromclient = (String) objectIn.readObject();
			}

			// Send URLs
			if (fromclient.equals(SEND_URL)) {
				for (int i = 0; i < urlList.size(); i++) {
					objectOut.writeObject(urlList.get(i));
					objectOut.flush();
				}
			}

			objectOut.writeObject(END);
			objectOut.flush();
			objectOut.writeObject(HOST);
			objectOut.flush();
			objectOut.writeInt(CLIENT_PORT);
			objectOut.flush();

			connected.close();
			objectIn.close();
			objectOut.close();
		} catch (Exception e) {
			System.out.println("Exception caught: " + e);
			System.exit(1);
		}
	}

	
	/*
	 * Function name: requestSubscribe(ArrayList<Integer> validTagList)
	 * 
	 * Description: This function contacts the SuperNode with a request to
	 * subscribe to the given tag(s). The input is a list of hashed tags. After
	 * sending all of the tags to the SuperNode, the connection is closed and
	 * the function returns.
	 */
	public static void requestSubscribe(ArrayList<Integer> validTagList) {
		Socket connected  = null;
		String fromclient = null;

		ObjectInputStream objectIn = null;
		ObjectOutputStream objectOut = null;
		System.out.println("\nSending subscribe data...");
		
		try {
			connected = new Socket(HOST, SUPERNODE_PORT_NUM);
			objectIn  = new ObjectInputStream(connected.getInputStream());
			objectOut = new ObjectOutputStream(connected.getOutputStream());

			// Send SUBSCRIBE Request
			objectOut.writeObject(SUBSCRIBE);
			objectOut.flush();
			fromclient = (String) objectIn.readObject();

			// Send tags
			if (fromclient.equals(SEND_TAG)) {
				for (int i = 0; i < validTagList.size(); i++) {
					objectOut.writeInt(validTagList.get(i));
					objectOut.flush();
				}
			}

			// -1 Indicates that there are no more tags to send
			objectOut.writeInt(-1);
			objectOut.flush();

			// Write the client's port number
			objectOut.writeInt(CLIENT_PORT);
			objectOut.flush();

			// Clean up
			connected.close();
			objectIn.close();
			objectOut.close();
		} catch (Exception e) {
			System.out.println("Exception caught: " + e);
			e.printStackTrace();
			System.exit(1);
		}
	}
	

	/*
	 * Function name: requestDHTData()
	 * 
	 * Description: This function contacts the SuperNode with a request for node
	 * data so that the DHT structure can be printed out. The SuperNode responds
	 * immediately with its own data. Then, receivePrintData is called to
	 * receive all of the other nodes' data.
	 */
	public static void requestDHTData() {
		Socket connected  = null;

		ObjectOutputStream objectOut  = null;
		ObjectInputStream  objectIn   = null;
		ArrayList<PrintData> nodeData = new ArrayList<PrintData>(MAX_NODES);
		
		System.out.println("Requesting data...\n");
		try {
			connected = new Socket(HOST, SUPERNODE_PORT_NUM);
			objectOut = new ObjectOutputStream(connected.getOutputStream());

			// Send PRINT_DATA Request
			objectOut.writeObject(PRINT_DATA);
			objectOut.flush();
			
			// Send port
			objectOut.writeInt(CLIENT_PORT);
			objectOut.flush();

			// Send host
			objectOut.writeObject(HOST);
			objectOut.flush();
			
			// Wait for OK
			objectIn = new ObjectInputStream(connected.getInputStream());
			String fromclient = (String) objectIn.readObject();
			
			// Clean up
			if (fromclient.equals(OK)) {
				connected.close();
				objectOut.close();
			}

			for (int i = 0; i < MAX_NODES; i++) {
				receivePrintData(nodeData);
			}

			myUtil.displayPrintData(nodeData);

		} catch (Exception e) {
			System.out.println("Exception caught: " + e);
			e.printStackTrace();
			System.exit(1);
		}
	}

	
	/*
	 * Function name: receivePublishResponse()
	 * 
	 * Description: This function listens for a response to a previously-sent
	 * publish request. Upon connection, it receives the nodeTrace data to be
	 * printed out for the user.
	 */
	public static boolean receivePublishResponse() {
		try {
			Socket connected = serverSocket.accept();
			ObjectInputStream objectIn = new ObjectInputStream(connected.getInputStream());

			int fromNode = objectIn.readInt();

			while (fromNode != -1) {
				nodeTrace.add(fromNode);
				fromNode = objectIn.readInt();
			}

			// Clean up
			connected.close();
			objectIn.close();
			return true;

		} catch (Exception e) {
			System.out.println("Exception caught: " + e);
			e.printStackTrace();
			return false;
		}
	}

	
	/*
	 * Function name: receiveSubscribeResponse()
	 * 
	 * Description: This function listens for a response to a previously-sent
	 * subscribe request. Upon connection, it receives the URLs that were found.
	 * Also, it receives the nodeTrace data to be printed out for the user.
	 */
	public static ArrayList<String> receiveSubscribeResponse() {
		String fromNode = null;
		ArrayList<String> urls = new ArrayList<String>();
		
		try {
			Socket connected = serverSocket.accept();
			ObjectInputStream objectIn = new ObjectInputStream(connected.getInputStream());

			// Request URLs
			while (true) {
				fromNode = (String) objectIn.readObject();

				while (!fromNode.equals(END)) {
					urlList.add(fromNode);
					urls.add(fromNode);
					fromNode = (String) objectIn.readObject();
				}

				int nodeID = objectIn.readInt();
				
				while (nodeID != -1) {
					nodeTrace.add(nodeID);
					nodeID = objectIn.readInt();
				}
				break;
			}

			// Clean up
			objectIn.close();
			connected.close();
			

		} catch (Exception e) {
			System.out.println("Exception caught: " + e);
			e.printStackTrace();
			System.exit(1);
		}
		return urls;
	}
	

	/*
	 * Function name: receivePrintData(ArrayList<PrintData> nodeData)
	 * 
	 * Description: This function listens for a response to a previously-sent
	 * node data request. Upon connection, it receives information about the
	 * connected node. This information includes the node's ID, the number of
	 * entries it contains, its successor and predecessor nodes, and its finger
	 * table data.
	 */
	public static void receivePrintData(ArrayList<PrintData> nodeData) {
		Socket connected = null;

		ObjectInputStream objectIn = null;
		ObjectOutputStream objectOut = null;

		try {
			connected = serverSocket.accept();
			objectIn  = new ObjectInputStream(connected.getInputStream());
			objectOut = new ObjectOutputStream(connected.getOutputStream());

			// Receive Node's data
			PrintData nodeItem = new PrintData();
			nodeItem.fingers = new ArrayList<Finger>(M + 1);

			// Retrieve nodeID
			nodeItem.nodeID = objectIn.readInt();

			// Retrieve number of entries
			nodeItem.numEntries = objectIn.readInt();

			// Retrieve successor ID
			nodeItem.succID = objectIn.readInt();

			// Retrieve predecessor ID
			nodeItem.predID = objectIn.readInt();

			// Finger Table Data
			for (int i = 0; i < M; i++) {
				Finger nodeFinger = (Finger) objectIn.readObject();
				nodeItem.fingers.add(nodeFinger);
			}

			nodeData.add(nodeItem);
			objectOut.writeObject(OK);
			objectOut.flush();

			// Clean up
			if (((String) objectIn.readObject()).equals(END)) {
				connected.close();
				objectIn.close();
				objectOut.close();
			}

		} catch (Exception e) {
			System.out.println("Exception caught: " + e);
			e.printStackTrace();
			System.exit(1);
		}
	}
}
