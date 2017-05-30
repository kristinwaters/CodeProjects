package cluster;

/******************************************************************************
 * Filename: ProcessRunner.java
 * Description:  This class is used by the ComputeNodeThread to execute a 
 *               sub-job.  Upon job completion, this class will forward the
 *               result to the server
 *
 * Author: Kristin Dahl
 * Date: 11-20-2015
 *****************************************************************************/

import static cluster.Util.BAD_COMMAND;
import static cluster.Util.BAD_FILENAME;
import static cluster.Util.ERR;
import static cluster.Util.RESULT;
import static cluster.Util.SERVER_HOST;
import static cluster.Util.SERVER_PORT;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ProcessRunner implements Runnable {

	// Global data declarations
	private String jobCommand;
	private String fileName;

	
	/*
	 * Function name: ProcessRunner(String jobCommand, String fileName)
	 * 
	 * Description: This is the constructor for the ProcessRunner class.
	 */
	public ProcessRunner(String jobCommand, String fileName) 
	{
		this.jobCommand = jobCommand;
		this.fileName = fileName;
	}

	
	/*
	 * Function name: run()
	 * 
	 * Description: This function is called to execute a job. First, it
	 * determines the current directory. It is assumed that the job command and
	 * input file are located in the current working directory.
	 * 
	 * Prior to starting the process, the current system time is saved. Then,
	 * when the process completes, the current system time is saved. The elapsed
	 * time is then forwarded to the server.
	 * 
	 * If an error occurs, an error code is forwarded to the server.
	 */
	public void run() 
	{
		Runtime runTime = Runtime.getRuntime();
		Process process = null;

		String currentDir = System.getProperty("user.dir");
		String cmd[] = { currentDir + '/' + jobCommand, currentDir + '/' + fileName };

		try {
			long startTime, stopTime;
			double elapsed;

			// Execute process
			startTime = System.currentTimeMillis();
			process = runTime.exec(cmd);
			process.waitFor();
			stopTime = System.currentTimeMillis();

			int retVal = process.exitValue();
			process.destroy();

			if (retVal == 0) {
				submitErrorToServer(BAD_FILENAME);
				return;
			}

			// Calculate the elapsed time
			elapsed = (stopTime - startTime) / 1000.0;

			// Send result
			submitResultToServer(elapsed);

		} catch (Exception e) {
			submitErrorToServer(BAD_COMMAND);
		}
	}

	
	/*
	 * Function name: submitResultToServer(double result)
	 * 
	 * Description: This function submits the result parameter to the server. It
	 * is called after completion of a sub-job.
	 */
	public static void submitResultToServer(double result) 
	{
		Socket connected = null;
		ObjectInputStream  objectIn  = null;
		ObjectOutputStream objectOut = null;

		try {
			connected = new Socket(SERVER_HOST, SERVER_PORT);
			objectIn  = new ObjectInputStream(connected.getInputStream());
			objectOut = new ObjectOutputStream(connected.getOutputStream());

			// Send SUBMIT Request
			objectOut.writeObject(RESULT);
			objectOut.flush();
			
			// Send Result
			objectOut.writeDouble(result);
			objectOut.flush();

			// Receive OK
			objectIn.readObject();

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
	 * Function name: submitErrorToServer(int error)
	 * 
	 * Description: This function submits the error parameter to the server. It
	 * is called in the event that a sub-job encounters an error.
	 */
	public static void submitErrorToServer(int error) 
	{
		Socket connected = null;
		ObjectInputStream  objectIn  = null;
		ObjectOutputStream objectOut = null;

		try {
			connected = new Socket(SERVER_HOST, SERVER_PORT);
			objectIn  = new ObjectInputStream(connected.getInputStream());
			objectOut = new ObjectOutputStream(connected.getOutputStream());

			// Send SUBMIT Request
			objectOut.writeObject(ERR);
			objectOut.flush();
			
			// Send Result
			objectOut.writeInt(error);
			objectOut.flush();

			// Receive OK
			objectIn.readObject();
			
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
