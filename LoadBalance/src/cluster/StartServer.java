package cluster;

/******************************************************************************
 * Filename: StartServer.java
 * Description: This class is used to create a new ServerThread.
 *
 * Author: Kristin Dahl
 * Date: 11-20-2015
 *****************************************************************************/

import cluster.ServerThread;

public class StartServer {

	// Start up a ServerThread
	public static void main(String[] args) {
		System.out.println("Starting Server...");

		Runnable s = new ServerThread();
		Thread t = new Thread(s);
		t.start();
	}
}
