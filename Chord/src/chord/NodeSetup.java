package chord;

/****************************************************************************** 
 * Filename: NodeSetup.java
 * 
 * Description: The is a file that is used to start up each "regular" node.  
 * Each of these nodes should be running before the SuperNode is started.
 * 
 * Author: Kristin Dahl
 * Date: 9-6-2016
 *****************************************************************************/

import static chord.ChordUtil.PORT_LIST;
import static chord.ChordUtil.SUPERNODE_ID;
import static chord.ChordUtil.SUPERNODE_PORT_NUM;

public class NodeSetup {
	public static NodeThread nodeList[] = new NodeThread[10 + 1];

	// Start up each node thread
	public static void main(String[] Args) {
		
		Runnable n1 = new NodeThread(PORT_LIST[1]);
		Thread t1 = new Thread(n1);
		t1.start();
		System.out.println("Node started on port " + PORT_LIST[1]);
		nodeList[1] = (NodeThread) n1;

		Runnable n2 = new NodeThread(PORT_LIST[2]);
		Thread t2 = new Thread(n2);
		t2.start();
		System.out.println("Node started on port " + PORT_LIST[2]);
		nodeList[2] = (NodeThread) n2;

		Runnable n3 = new NodeThread(PORT_LIST[3]);
		Thread t3 = new Thread(n3);
		t3.start();
		System.out.println("Node started on port " + PORT_LIST[3]);
		nodeList[3] = (NodeThread) n3;

		Runnable n4 = new NodeThread(PORT_LIST[4]);
		Thread t4 = new Thread(n4);
		t4.start();
		System.out.println("Node started on port " + PORT_LIST[4]);
		nodeList[4] = (NodeThread) n4;

		Runnable n5 = new NodeThread(PORT_LIST[5]);
		Thread t5 = new Thread(n5);
		t5.start();
		System.out.println("Node started on port " + PORT_LIST[5]);
		nodeList[5] = (NodeThread) n5;

		Runnable n6 = new NodeThread(PORT_LIST[6]);
		Thread t6 = new Thread(n6);
		t6.start();
		System.out.println("Node started on port " + PORT_LIST[6]);
		nodeList[6] = (NodeThread) n6;

		Runnable n7 = new NodeThread(PORT_LIST[7]);
		Thread t7 = new Thread(n7);
		t7.start();
		System.out.println("Node started on port " + PORT_LIST[7]);
		nodeList[7] = (NodeThread) n7;

		Runnable n8 = new NodeThread(PORT_LIST[8]);
		Thread t8 = new Thread(n8);
		t8.start();
		System.out.println("Node started on port " + PORT_LIST[8]);
		nodeList[8] = (NodeThread) n8;

		Runnable n9 = new NodeThread(PORT_LIST[9]);
		Thread t9 = new Thread(n9);
		t9.start();
		System.out.println("Node started on port " + PORT_LIST[9]);
		nodeList[9] = (NodeThread) n9;
		
		System.out.println("Starting SuperNode...\n");
		SuperNode sNode = new SuperNode(SUPERNODE_ID);
		
		// Create a thread to listen for incoming connections
		Runnable sn = new SuperNodeThread(SUPERNODE_PORT_NUM, sNode.id, sNode);
		Thread snt = new Thread(sn);
		snt.start();

		// Call join for each node
		SuperNode.createNodeIDs();
		SuperNode.contactNodes();
		System.out.println("\nAll nodes have joined.");
		
		Client.execute();
	}
}
