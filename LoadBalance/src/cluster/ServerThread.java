package cluster;

/******************************************************************************
 * Filename: ServerThread.java
 * Description:  This file contains the necessary functions to run the
 *               server component of the load-balanced compute cluster.
 *
 * Author: Kristin Dahl
 * Date: 11-20-2015
 *****************************************************************************/

import static cluster.Util.CLIENT_HOST;
import static cluster.Util.CLIENT_PORT;
import static cluster.Util.DO_SUB_JOB;
import static cluster.Util.END;
import static cluster.Util.END_NUM;
import static cluster.Util.ERR;
import static cluster.Util.HOST_LIST;
import static cluster.Util.NODE_PORT;
import static cluster.Util.OK;
import static cluster.Util.RESULT;
import static cluster.Util.SERVER_PORT;
import static cluster.Util.STATS;
import static cluster.Util.SUBMIT;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class ServerThread implements Runnable {

	// Global data declarations
	private static int totalJobs;
	private static int completedJobs;
	private static double totalRunTime;
	private static ArrayList<String> jobFiles;
	private static ArrayList<Double> jobRunTimes;
	private static ArrayList<StatData> statData;
	private static long startTime, stopTime;
	
	/*
	 * Function name: run()
	 * 
	 * Description: This function is called to start the server program.
	 */	
	public void run() 
	{
		jobFiles = new ArrayList<String>();
		statData = new ArrayList<StatData>();
		jobRunTimes = new ArrayList<Double>();

		listen(SERVER_PORT);
	}
	

	/*
	 * Function name: listen(int port)
	 * 
	 * Description: This function listens for incoming connections from both the
	 * client and other nodes.
	 */
	public void listen(int port) 
	{
		try {
			ServerSocket serverSocket = new ServerSocket(port);
			Socket connected = null;
			ObjectOutputStream objectOut = null;
			ObjectInputStream objectIn = null;

			while (true) {
				System.out.println("\nServer is listening on port " + port);

				connected = serverSocket.accept();
				objectOut = new ObjectOutputStream(connected.getOutputStream());
				objectIn  = new ObjectInputStream(connected.getInputStream());

				// Clear old status data
				statData.clear();

				// Get the command
				String fromClient = (String) objectIn.readObject();

				if (fromClient.equals(SUBMIT)) {
					System.out.println("\nServer received SUBMIT request.");

					// Reset job variables
					totalJobs = 0;
					startTime = 0;
					stopTime  = 0;
					totalRunTime  = 0;
					completedJobs = 0;

					jobFiles.clear();
					jobRunTimes.clear();

					// Receive command
					String command = (String) objectIn.readObject();

					// Receive files
					fromClient = (String) objectIn.readObject();

					while (!fromClient.equals(END)) {
						jobFiles.add(fromClient);
						fromClient = (String) objectIn.readObject();
					}

					// Send OK
					objectOut.writeObject(OK);
					objectOut.flush();

					// Handle Send command
					startTime = System.currentTimeMillis();
					handleSendCommand(command);

				} else if (fromClient.equals(STATS)) {
					System.out.println("\nServer received STATS request.");

					// Request stats data from each node
					int numNodes = HOST_LIST.length;

					for (int i = 0; i < numNodes; i++) {
						StatData data = requestStatData(HOST_LIST[i]);
						statData.add(data);
					}

					// Send results to client
					for (int i = 0; i < statData.size(); i++) {
						objectOut.writeObject(OK);
						objectOut.flush();

						objectOut.writeObject(statData.get(i));
						objectOut.flush();
					}

					objectOut.writeObject(END);
					objectOut.flush();

				} else if (fromClient.equals(RESULT)) {
					System.out.println("\nServer received RESULT request.");

					// Receive run time
					double runTime = objectIn.readDouble();
					totalRunTime += runTime;

					// Send OK
					objectOut.writeObject(OK);
					objectOut.flush();

					jobRunTimes.add(runTime);
					completedJobs++;

					if (completedJobs == totalJobs) {
						stopTime = System.currentTimeMillis();
						sendResultsToClient();
					}

				} else if (fromClient.equals(ERR)) {
					System.out.println("\nServer received ERR from node.");

					// Receive error code
					int error = objectIn.readInt();

					// Send OK
					objectOut.writeObject(OK);
					objectOut.flush();
					
					jobRunTimes.add((double) error);
					completedJobs++;

					if (completedJobs == totalJobs) {
						stopTime = System.currentTimeMillis();
						sendResultsToClient();
					}

				} else {
					System.err.println("Server received unknown command " + fromClient);
					objectOut.writeObject(ERR);
					objectOut.flush();
				}

				// Clean up
				objectIn.close();
				objectOut.close();
				connected.close();
			}

		} catch (Exception e) {
			System.out.println("Exception caught: " + e);
			e.printStackTrace();
			System.exit(1);
		}
	}

	
	/*
	 * Function name: sendResultsToClient()
	 * 
	 * Description: This function is called after all sub-job results have been
	 * returned to the server. It sends the total job run-time and the
	 * individual sub-job run-times to the client.
	 */
	public static void sendResultsToClient() 
	{
		Socket connected = null;
		ObjectOutputStream objectOut = null;

		try {
			connected = new Socket(CLIENT_HOST, CLIENT_PORT);
			objectOut = new ObjectOutputStream(connected.getOutputStream());

			// Send elapsed time
			totalRunTime = (stopTime - startTime) / 1000.0;
			objectOut.writeDouble(totalRunTime);
			objectOut.flush();

			// Send individual node times
			for (int i = 0; i < jobRunTimes.size(); i++) {
				objectOut.writeDouble(jobRunTimes.get(i));
				objectOut.flush();
			}
			objectOut.writeDouble(END_NUM);
			objectOut.flush();

			// Clean up
			connected.close();
			objectOut.close();

		} catch (Exception e) {
			System.out.println("Exception caught: " + e);
			e.printStackTrace();
			System.exit(1);
		}
	}
	

	/*
	 * Function name: handleSendCommand(String command)
	 * 
	 * Description: This function is used to determine which nodes to send a
	 * command and file to. Nodes are chosen randomly - a random index is
	 * generated, and this index is used to pick a hostname from the HOST_LIST
	 * array.
	 */
	public void handleSendCommand(String command) 
	{
		// Determine the number of files to send to each node
		int numFiles = jobFiles.size();
		int numNodes = HOST_LIST.length;

		totalJobs = numFiles;
		completedJobs = 0;

		for (int i = 0; i < numFiles; i++) {
			int randomIndex = (int) (numNodes * Math.random());
			String nodeHost = HOST_LIST[randomIndex];
			sendJob(command, nodeHost, jobFiles.get(i));
		}
	}
	

	/*
	 * Function name: requestStatData(String hostname)
	 * 
	 * Description: This function sends a STATS message to the node running on
	 * hostname. After the message is sent, the server waits for the node to
	 * return its status information. This status information is then returned
	 * to the calling function.
	 */
	public StatData requestStatData(String hostname) 
	{
		Socket connected = null;
		ObjectInputStream objectIn   = null;
		ObjectOutputStream objectOut = null;

		StatData data = new StatData();

		try {
			connected = new Socket(hostname, NODE_PORT);
			objectIn  = new ObjectInputStream(connected.getInputStream());
			objectOut = new ObjectOutputStream(connected.getOutputStream());

			// Send STATS Request
			objectOut.writeObject(STATS);
			objectOut.flush();

			// Retrieve jobsReceived
			data.jobsReceived = objectIn.readInt();

			// Retrieve jobsMigrated
			data.jobsMigrated = objectIn.readInt();

			// Retrieve currentLoad
			data.currentLoad = objectIn.readDouble();

			// Retrieve averageLoad
			data.averageLoad = objectIn.readDouble();

			// Retrieve hostname
			data.hostname = (String) objectIn.readObject();

			// Clean up
			connected.close();
			objectOut.close();
			objectIn.close();

		} catch (Exception e) {
			System.out.println("Exception caught: " + e);
			e.printStackTrace();
			System.exit(1);
		}

		return data;
	}

	
	/*
	 * Function name: sendJob(String command, String hostname, String fileName)
	 * 
	 * Description: This function sends a command and filename to the node
	 * running on hostname. It is called upon receipt of a SUBMIT message from
	 * the client.
	 */
	public void sendJob(String command, String hostname, String fileName) 
	{
		Socket connected = null;
		ObjectInputStream objectIn = null;
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
