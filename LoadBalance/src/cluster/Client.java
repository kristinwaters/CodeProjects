package cluster;

/******************************************************************************
 * Filename: Client.java
 * Description: This file contains the necessary functions to run the
 *              client component of the load-balanced compute cluster.
 *
 * Author: Kristin Dahl
 * Date: 11-20-2015
 *****************************************************************************/

import static cluster.Util.BAD_COMMAND;
import static cluster.Util.BAD_FILENAME;
import static cluster.Util.CLIENT_PORT;
import static cluster.Util.END;
import static cluster.Util.END_NUM;
import static cluster.Util.OK;
import static cluster.Util.SERVER_HOST;
import static cluster.Util.SERVER_PORT;
import static cluster.Util.STATS;
import static cluster.Util.SUBMIT;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.StringTokenizer;

public class Client {

	// Global data declarations
	private static ArrayList<String> jobFiles;
	private static ArrayList<Double> jobRunTimes;
	private static ArrayList<StatData> statData;
	private static String command;
	private static double totalRunTime;

	
	/*
	 * Function name: execute()
	 * 
	 * Description: This function is called to start the client program. It
	 * prints a welcome message and also initializes the global ArrayLists.
	 */
	public static void execute() 
	{
		try {
			jobFiles = new ArrayList<String>();
			jobRunTimes = new ArrayList<Double>();
			statData = new ArrayList<StatData>();

		} catch (Exception e) {
			System.out.println("Exception caught: " + e);
			e.printStackTrace();
			System.exit(1);
		}

		System.out.println("Welcome to the load-balanced compute cluster.\n");
		printMenu();
	}

	
	/*
	 * Function name: printMenu()
	 * 
	 * Description: This function presents the user with a menu that is used to
	 * interact with cluster system.
	 */
	public static void printMenu() 
	{
		try {
			while (true) {

				// Clear out previous data
				jobFiles.clear();
				statData.clear();
				jobRunTimes.clear();

				// Present menu
				System.out.println("********* Menu *********");
				System.out.println("Submit Job ----  Press 1");
				System.out.println("View Stats ----  Press 2");
				System.out.println("Exit  ---------  Press 3");
				System.out.println("************************");
				System.out.print("\nEnter a command: ");

				// Retrieve user input
				int command = 0;
				try {
					Scanner in = new Scanner(System.in);
					command = in.nextInt();
				} catch (Exception e) {}

				switch (command) {
				case 1:
					submitJob();
					receiveJobResult();
					printJobResult();
					break;
				case 2:
					requestStats();
					printStatData();
					break;
				case 3:
					System.out.println("Now exiting.  Bye!\n");
					System.exit(1);
				default:
					System.out.println("\nPlease enter a valid option.\n\n");
					break;
				}
			}

		} catch (Exception e) {
			System.out.println("Exception caught: " + e);
			e.printStackTrace();
			System.exit(1);
		}
	}
	

	/*
	 * Function name: submitJob()
	 * 
	 * Description: This function gathers user input and determines whether the
	 * user entered a properly-formatted job command.
	 * 
	 * If the command is properly formatted, submitToServer() is called.
	 * Otherwise a message is printed to the console and printMenu() is called.
	 */
	public static void submitJob() 
	{
		System.out.println("\n\n------- Submit Job -------");
		System.out.print("Enter a job command: ");

		// Get job command from user.
		Scanner in = new Scanner(System.in);
		String jobCommand = in.nextLine();
		boolean validInput = parseUserInput(jobCommand);

		if (!validInput) {
			System.out.println("\nInvalid command. Format: job-cmd file1 file2 ... fileN\n");
			printMenu();
		}

		submitToServer();
	}

	
	/*
	 * Function name: submitToServer()
	 * 
	 * Description: This function submits a job command and filenames to the
	 * server. If the submission is successful, the server returns 'OK' and a
	 * status message is printed at the console.
	 * 
	 * If job submission fails, the printMenu() function is called.
	 */
	public static void submitToServer() 
	{
		Socket connected  = null;
		String fromServer = null;
		
		ObjectInputStream objectIn   = null;
		ObjectOutputStream objectOut = null;

		try {
			connected = new Socket(SERVER_HOST, SERVER_PORT);
			objectIn  = new ObjectInputStream(connected.getInputStream());
			objectOut = new ObjectOutputStream(connected.getOutputStream());

			// Send SUBMIT Request
			objectOut.writeObject(SUBMIT);
			objectOut.flush();

			// Send Command
			objectOut.writeObject(command);
			objectOut.flush();

			// Send Files
			for (int i = 0; i < jobFiles.size(); i++) {
				objectOut.writeObject(jobFiles.get(i));
			}
			objectOut.writeObject(END);
			objectOut.flush();

			// Get response
			fromServer = (String) objectIn.readObject();

			// Clean up
			connected.close();
			objectOut.close();
			objectIn.close();

			// If 'OK' was received, continue processing. Otherwise, go back to
			// the menu.
			if (fromServer.equals(OK)) {
				System.out.println("\nJob submitted successfully.\n");
				return;
			} else {
				System.out.println("\nUnable to send job command to server.\n");
				printMenu();
			}

		} catch (Exception e) {
			System.out.println("Exception caught: " + e);
			e.printStackTrace();
			System.exit(1);
		}
	}
	

	/*
	 * Function name: requestStats()
	 * 
	 * Description: This function sends a STATS message to the server. After the
	 * message is sent, the client waits for the server to return status
	 * information for each node.
	 * 
	 * The status data for each node is added to the global statData list.
	 */
	public static void requestStats() 
	{
		Socket connected  = null;
		String fromServer = null;

		ObjectInputStream objectIn   = null;
		ObjectOutputStream objectOut = null;

		try {
			connected = new Socket(SERVER_HOST, SERVER_PORT);
			objectIn  = new ObjectInputStream(connected.getInputStream());
			objectOut = new ObjectOutputStream(connected.getOutputStream());

			// Send STATS Request
			objectOut.writeObject(STATS);
			objectOut.flush();

			// Retrieve StatData objects
			fromServer = (String) objectIn.readObject();

			while (!fromServer.equals(END)) {
				StatData data = (StatData) objectIn.readObject();
				statData.add(data);
				fromServer = (String) objectIn.readObject();
			}

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
	

	/*
	 * Function name: receiveJobResult()
	 * 
	 * Description: This function listens for a response to a previously-sent
	 * job submission. Upon connection, it receives the total run-time for the
	 * job and the run-time for each individual sub-job.
	 * 
	 * If the totalRunTime variable is a negative value (i.e. BAD_COMMAND or
	 * BAD_FILENAME), a message is printed to the user and printMenu() is
	 * called.
	 */
	public static void receiveJobResult() 
	{
		Socket connected = null;
		ServerSocket serverSocket  = null;
		ObjectInputStream objectIn = null;

		try {
			serverSocket = new ServerSocket(CLIENT_PORT);
			connected    = serverSocket.accept();
			objectIn     = new ObjectInputStream(connected.getInputStream());

			// Receive total time
			totalRunTime = objectIn.readDouble();

			// Receive individual node times
			double runtime = objectIn.readDouble();

			while (runtime != END_NUM) {
				jobRunTimes.add(runtime);
				runtime = objectIn.readDouble();
			}

			// Clean up
			objectIn.close();
			connected.close();
			serverSocket.close();

		} catch (Exception e) {
			System.out.println("Exception caught: " + e);
			e.printStackTrace();
			System.exit(1);
		}
	}
	

	/*
	 * Function name: printJobResult()
	 * 
	 * Description: This function presents the results of a remote job execution
	 * to the user. It displays the total run-time for the job and the run-time
	 * for each individual sub-job.
	 */
	public static void printJobResult() 
	{
		System.out.println("\n------- Job Results -------");
		System.out.println("Total time: " + totalRunTime + " seconds");
		System.out.println("\nIndividual Job Times:");

		for (int i = 0; i < jobRunTimes.size(); i++) {
			double runTime = jobRunTimes.get(i);
			
			if (runTime == BAD_COMMAND) {
				System.out.println("Job " + (i + 1) + ": Bad command");
				
			} else if (runTime == BAD_FILENAME) {
				System.out.println("Job " + (i + 1) + ": Bad filename");
				
			} else {
				System.out.println("Job " + (i + 1) + ": " + jobRunTimes.get(i) + " seconds");
			}
		}

		System.out.println("---------------------------\n\n");
	}

	
	/*
	 * Function name: printStatData()
	 * 
	 * Description: This function presents the results of a STATS request. For
	 * each node, the following data is displayed to the user:
	 *   - Hostname 
	 *   - Total jobs received
	 *   - Total jobs migrated 
	 *   - Current load 
	 *   - Average load
	 */
	public static void printStatData() 
	{
		System.out.println("\n------- Stat Data -------\n");

		for (int i = 0; i < statData.size(); i++) {
			StatData data = statData.get(i);

			System.out.println("Node " + (i + 1) + ": " + data.hostname);
			System.out.println("Total jobs received: " + data.jobsReceived);
			System.out.println("Total jobs migrated: " + data.jobsMigrated);
			System.out.println("Current load: " + data.currentLoad);
			System.out.println("Average load: " + data.averageLoad + "\n");
		}

		System.out.print("---------------------------\n\n");
	}

	
	/*
	 * Function name: parseUserInput()
	 * 
	 * Description: This function determines whether the user-entered job
	 * request is formatted properly. A proper job request has the following
	 * format: job-cmd file1 file2 ... fileN
	 * 
	 * If the command has more than one string, it is assumed that the first
	 * word is the command. Each additional string is added to the jobFiles
	 * list.
	 */
	public static boolean parseUserInput(String input) 
	{
		StringTokenizer st = new StringTokenizer(input);
		int stSize = st.countTokens();

		// If the user entered only the job command, return an error.
		if (stSize == 1) {
			return false;
		} else {
			command = st.nextToken();

			// Get files
			while (st.hasMoreTokens()) {
				String fileName = st.nextToken();
				jobFiles.add(fileName);
			}
			return true;
		}
	}
}
