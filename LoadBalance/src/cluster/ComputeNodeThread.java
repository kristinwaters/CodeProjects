package cluster;

/******************************************************************************
 * Filename: ComputeNodeThread.java
 * Description:  This file contains the necessary functions to run the
 *               ComputeNode component of the load-balanced compute cluster.
 *
 * Author: Kristin Dahl
 * Date: 11-20-2015
 *****************************************************************************/

import static cluster.Util.DO_SUB_JOB;
import static cluster.Util.ERR;
import static cluster.Util.GET_LOAD_VALUE;
import static cluster.Util.HOST_LIST;
import static cluster.Util.LOAD_PATH;
import static cluster.Util.LOAD_THRESHOLD;
import static cluster.Util.NODE_PORT;
import static cluster.Util.OK;
import static cluster.Util.STATS;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.StringTokenizer;

public class ComputeNodeThread implements Runnable {

	// Global data declarations
	public String nodeHost;
	private int jobsReceived;
	private int jobsMigrated;

	
	/*
	 * Function name: run()
	 * 
	 * Description: This function is called to start the node.
	 */
	public void run() 
	{
		try {
			// Get hostname
			InetAddress addr = InetAddress.getLocalHost();
			this.nodeHost = addr.getHostName();
		} catch (Exception e) {
			System.out.println("Exception caught: " + e);
			e.printStackTrace();
		}

		jobsReceived = 0;
		jobsMigrated = 0;
		listen(NODE_PORT);
	}
	

	/*
	 * Function name: listen(int port)
	 * 
	 * Description: This function listens for incoming connections from both the
	 * server and other nodes.
	 */
	public void listen(int port) 
	{
		try {
			Socket connected = null;
			ServerSocket serverSocket = new ServerSocket(port);
			ObjectOutputStream objectOut = null;
			ObjectInputStream objectIn = null;

			while (true) {
				System.out.println("\nComputeNode is listening on port " + port);

				connected = serverSocket.accept();
				objectOut = new ObjectOutputStream(connected.getOutputStream());
				objectIn  = new ObjectInputStream(connected.getInputStream());

				// Get the command
				String incoming = (String) objectIn.readObject();

				if (incoming.equals(DO_SUB_JOB)) {
					System.out.println("\nComputeNode received DO_SUB_JOB command.");

					// Increment jobsReceived
					jobsReceived++;

					// Receive command
					String jobCommand = (String) objectIn.readObject();

					// Receive file
					String fileName = (String) objectIn.readObject();

					// Handle job
					handleSubJob(jobCommand, fileName);

				} else if (incoming.equals(GET_LOAD_VALUE)) {
					System.out.println("\nComputeNode received GET_LOAD_VALUE command.");

					// Send current processor load value
					double currentLoad = getCurrentLoad();
					objectOut.writeDouble(currentLoad);
					objectOut.flush();

					// Send OK
					objectOut.writeObject(OK);
					objectOut.flush();

				} else if (incoming.equals(STATS)) {
					System.out.println("\nComputeNode received STATS request.");

					// Send jobsReceived
					objectOut.writeInt(jobsReceived);
					objectOut.flush();

					// Send jobsMigrated
					objectOut.writeInt(jobsMigrated);
					objectOut.flush();

					// Send current load
					double currentLoad = getCurrentLoad();
					objectOut.writeDouble(currentLoad);
					objectOut.flush();

					// Send average load
					double averageLoad = getAverageLoad();
					objectOut.writeDouble(averageLoad);
					objectOut.flush();

					// Send hostname
					objectOut.writeObject(nodeHost);
					objectOut.flush();

				} else {
					System.err.println("nComputeNode received unknown command " + incoming);
					objectOut.writeObject(ERR);
					objectOut.flush();
				}

				// Clean up
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

	
	/*
	 * Function name: handleSubJob(String jobCommand, String fileName)
	 * 
	 * Description: This function determines whether a sub-job should be
	 * forwarded to another node. First, it gets the node's current processing
	 * load. If the node's current load is greater than the load threshold, then
	 * it requests the processing load of each of the other nodes listed in
	 * HOST_LIST.
	 * 
	 * If a different node has a lower processing load than the node threshold,
	 * then the sub-job is forwarded to that node. If no other nodes are found,
	 * then this node will execute the sub-job (regardless of the current
	 * processing load).
	 */
	public void handleSubJob(String jobCommand, String fileName) 
	{
		double currentLoad = getCurrentLoad();
		double remoteNodeLoad;

		// If the currentLoad is greater than the load threshold, then request
		// the load values for the other nodes. Otherwise, execute the subjob.
		if (currentLoad > LOAD_THRESHOLD) {
			for (int i = 0; i < HOST_LIST.length; i++) {

				if (!HOST_LIST[i].equals(this.nodeHost)) {
					remoteNodeLoad = requestLoadValue(HOST_LIST[i]);

					if (remoteNodeLoad < currentLoad) {
						sendJob(jobCommand, HOST_LIST[i], fileName);

						// Increment jobsMigrated
						jobsMigrated++;
						return;
					}
				}
			}
		}

		// At this point, perform the sub-job regardless of load size
		Runnable processRunner = new ProcessRunner(jobCommand, fileName);
		Thread thread = new Thread(processRunner);
		thread.start();
	}

	
	/*
	 * Function name: getCurrentLoad()
	 * 
	 * Description: This function returns the node's current processing load.
	 * This value is obtained by reading the first value located in
	 * /proc/loadavg.
	 */
	public static double getCurrentLoad() 
	{
		FileInputStream fStream = null;
		DataInputStream dataIn  = null;
		BufferedReader  bReader = null;
		String strLine = null;

		// Read the first line from /proc/loadavg
		try {
			// Open the file and DataInputStream
			fStream = new FileInputStream(LOAD_PATH);
			dataIn  = new DataInputStream(fStream);
			bReader = new BufferedReader(new InputStreamReader(dataIn));

			// Read Line
			strLine = bReader.readLine();

			// Close the input streams
			fStream.close();
			dataIn.close();
			bReader.close();

		} catch (Exception e) {
			System.out.println("Exception caught: " + e);
			e.printStackTrace();
			System.exit(1);
		}

		// Parse the load value average over the last minute.
		StringTokenizer st = new StringTokenizer(strLine);
		String oneMinPeriod = st.nextToken();

		return (new Double(oneMinPeriod));
	}

	
	/*
	 * Function name: getAverageLoad()
	 * 
	 * Description: This function returns the node's average processing load.
	 * This value is obtained by reading the second value located in
	 * /proc/loadavg.
	 */
	public static double getAverageLoad() 
	{
		FileInputStream fStream = null;
		DataInputStream dataIn  = null;
		BufferedReader  bReader = null;
		String strLine = null;

		// Read the first line from /proc/loadavg
		try {
			// Open the file and DataInputStream
			fStream = new FileInputStream(LOAD_PATH);
			dataIn  = new DataInputStream(fStream);
			bReader = new BufferedReader(new InputStreamReader(dataIn));

			// Read Line
			strLine = bReader.readLine();

			// Close the input streams
			fStream.close();
			dataIn.close();
			bReader.close();

		} catch (Exception e) {
			System.out.println("Exception caught: " + e);
			e.printStackTrace();
			System.exit(1);
		}

		// Parse the load value average over the last five minutes.
		StringTokenizer st = new StringTokenizer(strLine);
		st.nextToken(); // Skip one-minute period

		String fiveMinPeriod = st.nextToken();

		return (new Double(fiveMinPeriod));
	}

	
	/*
	 * Function name: requestLoadValue(String hostname)
	 * 
	 * Description: This function sends a request to the node running on
	 * hostname for its current processing load.
	 */
	public static double requestLoadValue(String hostname) 
	{
		Socket connected = null;
		String fromNode  = null;
		
		ObjectInputStream  objectIn  = null;
		ObjectOutputStream objectOut = null;

		double remoteLoadValue = 0;

		try {
			connected = new Socket(hostname, NODE_PORT);
			objectIn  = new ObjectInputStream(connected.getInputStream());
			objectOut = new ObjectOutputStream(connected.getOutputStream());

			// Send SUBMIT Request
			objectOut.writeObject(GET_LOAD_VALUE);
			objectOut.flush();

			// Receive load value
			remoteLoadValue = objectIn.readDouble();

			// Receive OK
			fromNode = (String) objectIn.readObject();

			// Clean up
			if (fromNode.equals(OK)) {
				connected.close();
				objectOut.close();
				objectIn.close();
			}

		} catch (Exception e) {
			System.out.println("Exception caught: " + e);
			e.printStackTrace();
			System.exit(1);
		}

		return remoteLoadValue;
	}
	

	/*
	 * Function name: sendJob(String command, String hostname, String fileName)
	 * 
	 * Description: This function forwards a command and filename to the node
	 * running on hostname.
	 */
	public void sendJob(String command, String hostname, String fileName) 
	{
		Socket connected = null;
		ObjectInputStream  objectIn  = null;
		ObjectOutputStream objectOut = null;

		try {
			connected = new Socket(hostname, NODE_PORT);
			objectIn  = new ObjectInputStream(connected.getInputStream());
			objectOut = new ObjectOutputStream(connected.getOutputStream());

			// Send SUB_JOB Request
			objectOut.writeObject(DO_SUB_JOB);
			objectOut.flush();

			// Send Command
			objectOut.writeObject(command);
			objectOut.flush();

			// Send File
			objectOut.writeObject(fileName);
			objectOut.flush();

			// Clean up
			connected.close();
			objectOut.close();
			objectIn.close();

		} catch (Exception e) {
			System.out.println("Exception caught: " + e);
			e.printStackTrace();
			System.exit(1);
		}
	}
}
