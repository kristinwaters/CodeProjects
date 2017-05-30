package chord;

/******************************************************************************
 * Filename: SuperNode.java
 * 
 * Description:  This file defines the SuperNode data class.  Each node has 
 * its own ID, finger table, port, and entries map.
 * 
 * Author: Kristin Dahl
 * Date: 9-6-2016
 *****************************************************************************/

import static chord.ChordUtil.HOST;
import static chord.ChordUtil.JOIN;
import static chord.ChordUtil.M;
import static chord.ChordUtil.MAX_NODES;
import static chord.ChordUtil.OK;
import static chord.ChordUtil.PORT_LIST;
import static chord.ChordUtil.SUPERNODE_ID;
import static chord.ChordUtil.SUPERNODE_PORT_NUM;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;

public class SuperNode extends Node {
	private static final long serialVersionUID = -8101231720383517072L;
	private static ArrayList<Integer> nodeIDs;

	// SuperNode constructor
	public SuperNode(int id) {
		this.entries = new HashMap<Integer, ArrayList<String>>();
		this.id = id;
		this.port = SUPERNODE_PORT_NUM;
		nodeIDs = new ArrayList<Integer>(MAX_NODES);
		
		// Initiate supernode finger tables
		initSuperNode();		
	}

	
	/*
	 * Function name: initSuperNode()
	 * 
	 * Initiate the finger table for the supernode and set the predecessor and
	 * successor values.
	 */
	public void initSuperNode() {
		fingerTable = new Finger[M + 1];
		for (int k = 1; k <= M; k++) {
			fingerTable[k] = new Finger(this);
			fingerTable[k].start = (id + (int) Math.pow(2, k - 1)) % (int) Math.pow(2, M);
		}
		predecessor = this;
	}

	
	/*
	 * Function name: hashNodeID(String id)
	 * 
	 * Used to generate node IDs.
	 */
	public static int hashNodeID(String id) {
		MessageDigest md5 = null;
		byte[] thedigest = null;
		try {
			byte[] bytesOfTag = id.getBytes("UTF-8");
			md5 = MessageDigest.getInstance("MD5");
			thedigest = md5.digest(bytesOfTag);

		} catch (Exception e) {
			e.printStackTrace();
		}
		return (thedigest.hashCode() % (int) Math.pow(2, M));
	}
	
	
	public static void createNodeIDs() {
		nodeIDs.add(SUPERNODE_ID);
		
		for (int i = 1; i < PORT_LIST.length; i++) {
			String nodePort = Integer.toString(PORT_LIST[i]);
			String nodeString = HOST.concat(nodePort);
			int newNodeID = hashNodeID(nodeString);
			
			while (nodeIDs.contains(newNodeID)) {
				newNodeID = hashNodeID(nodeString);
			}
			
			nodeIDs.add(newNodeID);
		}	
	}

	
	/*
	 * Function name: contactNodes()
	 * 
	 * This is called upon startup to send a JOIN request to each "regular"
	 * node.
	 */
	public static void contactNodes() {
		for (int i = 1; i < MAX_NODES; i++) { // Index 0 refers to the supernode. Skip.
			boolean join = false;
			String fromclient = null;

			ObjectOutputStream objectOut = null;
			ObjectInputStream objectIn = null;
			Socket connected = null;

			try {
				while (!join) {
					connected = new Socket(HOST, PORT_LIST[i]);
					objectIn  = new ObjectInputStream(connected.getInputStream());
					objectOut = new ObjectOutputStream(connected.getOutputStream());

					// Send JOIN command to node
					System.out.println("Sending JOIN request to node on port " + PORT_LIST[i]);
					objectOut.writeObject(JOIN);
					objectOut.flush();
					
					// Send nodeID to node
					objectOut.writeInt(nodeIDs.get(i));
					objectOut.flush();
					fromclient = (String) objectIn.readObject();
					
					if (fromclient.equals(OK)) {
						join = true;
					}
				}

				// Clean up
				connected.close();
				objectIn.close();
				objectOut.close();

			} catch (Exception e) {
				System.err.println("Exception caught: " + e);
				e.printStackTrace();
				System.exit(1);
			}
		}
	}

	// Main function to start SuperNode
	public static void main(String[] args) {
		System.out.println("Starting SuperNode...\n");
		SuperNode sNode = new SuperNode(SUPERNODE_ID);
		
		// Create a thread to listen for incoming connections
		Runnable n1 = new SuperNodeThread(SUPERNODE_PORT_NUM, sNode.id, sNode);
		Thread t1 = new Thread(n1);
		t1.start();

		// Call join for each node
		createNodeIDs();
		contactNodes();
		System.out.println("\nAll nodes have joined.");
	}
}
