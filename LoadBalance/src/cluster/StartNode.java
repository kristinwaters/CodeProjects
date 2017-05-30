package cluster;

/******************************************************************************
 * Filename: StartNode.java
 * Description: This class is used to create a new ComputeNodeThread.
 *
 * Author: Kristin Dahl
 * Date: 11-20-2015
 *****************************************************************************/

import cluster.ComputeNodeThread;

public class StartNode {
	
	// Start up each a ComputeNodeThread
	public static void main(String[] args) {
		System.out.println("Starting ComputeNode...");
		
		Runnable n = new ComputeNodeThread();
		Thread t = new Thread(n);
		t.start();
	}
}
