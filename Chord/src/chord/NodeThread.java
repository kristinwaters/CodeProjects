package chord;

/******************************************************************************
 * Filename: NodeThread.java
 * 
 * Description: The file defines all of the node operations needed to execute 
 * the Chord protocol.  Each NodeThread has its own Node object.
 * 
 * Each NodeThread listens on a port for incoming connections from other nodes.
 * When a connection is made, it determines what the request is and then 
 * executes the request.  When the request is complete, the thread will then 
 * continue to listen on its port.
 * 
 * Author: Kristin Dahl
 * Date: 9-6-2016
 *****************************************************************************/

import static chord.ChordUtil.CLOSEST_PRE_FINGER;
import static chord.ChordUtil.END;
import static chord.ChordUtil.ERR;
import static chord.ChordUtil.FIND_PRED;
import static chord.ChordUtil.FIND_SUCC;
import static chord.ChordUtil.GET_NODE;
import static chord.ChordUtil.HOST;
import static chord.ChordUtil.JOIN;
import static chord.ChordUtil.M;
import static chord.ChordUtil.MAX_NODES;
import static chord.ChordUtil.OK;
import static chord.ChordUtil.PRINT_DATA;
import static chord.ChordUtil.PUBLISH;
import static chord.ChordUtil.SEND_HOST;
import static chord.ChordUtil.SEND_PORT;
import static chord.ChordUtil.SEND_TAG;
import static chord.ChordUtil.SEND_TRACE;
import static chord.ChordUtil.SEND_URL;
import static chord.ChordUtil.SET_PRED;
import static chord.ChordUtil.SUBSCRIBE;
import static chord.ChordUtil.SUPERNODE_ID;
import static chord.ChordUtil.SUPERNODE_PORT_NUM;
import static chord.ChordUtil.UPDATE_FINGER_TABLE;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class NodeThread implements Serializable, Runnable {
	private static final long serialVersionUID = 5182277720568162566L;

	public String clientHost;
	public int    clientPort;
	public Node   myNode;
	
	public boolean trace;			          // Determine whether a node trace must be sent
	public ArrayList<Integer> nodeTrace;      // Used to keep track of which nodes are contacted

	
	// NodeThread constructor
	public NodeThread(int port) {
		trace     = false;
		myNode    = new Node(port);
		nodeTrace = new ArrayList<Integer>(MAX_NODES);
	}
	
	// Default NodeThread constructor
	public NodeThread() {}

	// When run is executed, each node will listen on the specified port.
	public void run() {
		listen(myNode.port);
	}

	// Listen for incoming connections
	public void listen(int port) {
		try {
			ServerSocket serverSocket = new ServerSocket(port);
			Socket connected = null;
			
			ObjectOutputStream outputObject = null;
			ObjectInputStream  inputObject  = null;

			while (true) {
				// Clear out previous node trace data
				nodeTrace.clear();
				trace = false;

				connected    = serverSocket.accept();
				outputObject = new ObjectOutputStream(connected.getOutputStream());
				inputObject  = new ObjectInputStream(connected.getInputStream());

				// Get the command
				String fromNode = (String) inputObject.readObject();

				if (fromNode.equals(JOIN)) {
					int id = inputObject.readInt();
					myNode.setID(id);
					System.out.println("\nNode " + myNode.id + " received join request.");

					// Call join and respond to supernode with OK
					join();
					outputObject.writeObject(OK);
					outputObject.flush();

				} else if (fromNode.equals(PUBLISH)) {
					handlePublish(outputObject, inputObject);

				} else if (fromNode.equals(SUBSCRIBE)) {
					handleSubscribe(outputObject, inputObject);

				} else if (fromNode.equals(GET_NODE)) {
					outputObject.writeObject(myNode);
					outputObject.flush();

				} else if (fromNode.equals(FIND_PRED)) {
					int id = (Integer) inputObject.readInt();
					outputObject.writeObject(findPredecessor(id));
					outputObject.flush();

				} else if (fromNode.equals(FIND_SUCC)) {
					int id = (Integer) inputObject.readInt();
					trace  = (boolean) inputObject.readBoolean();

					outputObject.writeObject(findSuccessor(id));
					outputObject.flush();

				} else if (fromNode.equals(SET_PRED)) {
					Node s = (Node) inputObject.readObject();
					myNode.predecessor = s;

				} else if (fromNode.equals(UPDATE_FINGER_TABLE)) {
					Node s = (Node)    inputObject.readObject();
					int  i = (Integer) inputObject.readObject();
					updateFingerTable(s, i);

					outputObject.writeObject(OK);
					outputObject.flush();
					
				} else if (fromNode.equals(CLOSEST_PRE_FINGER)) {
					int id = (Integer) inputObject.readInt();
					outputObject.writeObject(closestPrecedingFinger(id));
					outputObject.flush();

				} else if (fromNode.equals(PRINT_DATA)) {
					clientPort = (Integer) inputObject.readInt();
					clientHost = (String)  inputObject.readObject();
					
					outputObject.writeObject(OK);
		            sendClientNodeData();

		            if (myNode.successor().id != SUPERNODE_ID) {
		            	forwardPrintMessage(myNode.successor().port);
		            }

				} else {
					System.err.println("Node " + myNode.id + ": RECEIVED UNKNOWN COMMAND " + fromNode);
					outputObject.writeObject(ERR);
					outputObject.flush();
				}

				// If the calling node needs the node trace data, send it.
				if (trace) {
					for (int i = 0; i < nodeTrace.size(); i++) {
						outputObject.writeInt(nodeTrace.get(i));
						outputObject.flush();
					}

					outputObject.writeInt(-1);
					outputObject.flush();
				}

				// Clean up
				connected.close();
				inputObject.close();
				outputObject.close();
			}
		} catch (Exception e) {
			System.out.println("Exception caught: " + e);
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	
	/*
	 * Function name: join()
	 * 
	 * Description: This function is called remotely by the SuperNode upon
	 * startup. It initializes the finger table of a node and updates the finger
	 * tables of nodes that have already joined the network.
	 */
	public void join( ) {
		initFingerTable();
		updateOthers();
	}
	
	/*
	 * Function name: initFingerTable()
	 * 
	 * Description: This function initializes the finger table of a node. When
	 * joining, each node contacts the SuperNode, because the SuperNode is
	 * assumed to be a node that is already on the network.
	 */
	public void initFingerTable() {
		int start = (myNode.id + 1) % (int) Math.pow(2, M);

		try {
			Node newNode = (Node) invokeRemote(SUPERNODE_PORT_NUM, null, FIND_SUCC, start, false);
			myNode.fingerTable[1] = new Finger(newNode);
			myNode.fingerTable[1].start = start;

			myNode.predecessor = myNode.successor().predecessor;
			myNode.successor().predecessor = myNode;
			invokeRemote(myNode.successor().port, myNode, SET_PRED, 0, false);
						
			for (int i = 1; i < M; i++) {
				start = (myNode.id + (int) Math.pow(2, i)) % (int) Math.pow(2, M);

				if (onRange(start, myNode.id, myNode.fingerTable[i].node.id - 1)) {
					myNode.fingerTable[i + 1] = new Finger(myNode.fingerTable[i].node);

				} else {
					newNode = (Node) invokeRemote(SUPERNODE_PORT_NUM, null, FIND_SUCC, start, false);
					myNode.fingerTable[i + 1] = new Finger(newNode);

					if (!onRange(myNode.fingerTable[i + 1].node.id, start,myNode.id)) {
						myNode.fingerTable[i + 1] = new Finger(myNode);
					}
				}
				myNode.fingerTable[i + 1].start = start;
			}

		} catch (Exception e) {
			System.out.println("Exception caught: " + e);
			e.printStackTrace();
		}
	}

	/*
	 * Function name: updateOthers()
	 * 
	 * Description: This function is used to update the finger tables of other
	 * existing nodes on the network. It is called after a new node's finger
	 * table has been initialized.
	 */
	public void updateOthers() {
		for (int i = 1; i <= M; i++) {
			int pKey = myNode.id - (int) Math.pow(2, i - 1) + 1;
			Node p = findPredecessor(pKey);

			if (myNode.id != p.id){
				invokeRemote(p.port, myNode, UPDATE_FINGER_TABLE, i, false);
			}
		}
	}
	
	/*
	 * Function name: updateFingerTable(Node s, int i)
	 * 
	 * Description: This function is used to update the finger tables of other
	 * existing nodes on the network. It is called after a new node's finger
	 * table has been initialized.
	 */
	public void updateFingerTable(Node s, int i) {
		if (onRange(s.id, myNode.id, myNode.fingerTable[i].node.id - 1)) {
			myNode.fingerTable[i].node = s;

			if (myNode.predecessor.id != s.id) {
				invokeRemote(myNode.predecessor.port, s, UPDATE_FINGER_TABLE, i, false);
			}
		}
	}
	
	/*
	 * Function name: findSuccessor(int keyID)
	 * 
	 * Description: This function is used to find the successor of a node. This
	 * is done by finding the immediate predecessor of node ID. The successor of
	 * that node must be the successor of the keyID.
	 */
	public Node findSuccessor(int keyID) {
		nodeTrace.add(myNode.id);
		return findPredecessor(keyID).successor();
	}

	/*
	 * Function name: findPredecessor(int keyID)
	 * 
	 * Description: This function contacts a series of nodes moving forward
	 * around the Chord circle toward keyID. If a node contacts a node nPrime
	 * such that keyID falls between nPrime and the successor of nPrime, then
	 * findPredecessor is done and returns nPrime. Otherwise, the node asks
	 * nPrime for the node that mostly closely precedes keyID.
	 */
	public Node findPredecessor(int keyID) {
		Node nPrime = myNode;

		while (!onRange(keyID, nPrime.id + 1, nPrime.successor().id)) {
			if (nPrime.id == myNode.id) {
			    nPrime = closestPrecedingFinger(keyID);
				
			} else {
				nPrime = (Node) invokeRemote(nPrime.port, null, CLOSEST_PRE_FINGER, keyID, false);
			}

			if (nPrime.id != myNode.id) {
				nodeTrace.add(nPrime.id);
			}
		}

		return nPrime;
	}

	/*
	 * Function name: closestPrecedingFinger(int keyID)
	 * 
	 * Description: This function checks each element of a node's finger table
	 * and looks for a node whose ID falls between the current node's ID and
	 * keyID.
	 */
	public Node closestPrecedingFinger(int keyID) {
		Node returnNode = myNode;
		
		for (int i = M; i > 0; i--) {
			if (onRange(myNode.fingerTable[i].node.id, myNode.id + 1, keyID - 1)) {
				
				if (myNode.id != myNode.fingerTable[i].node.id) {
					returnNode = (Node) invokeRemote(myNode.fingerTable[i].node.port, null, GET_NODE, 0, false);
					return returnNode; 
					
				} else {
					returnNode = myNode.fingerTable[i].node;
					return returnNode;
				}
			}
		}
		return returnNode;
	}



	/*
	 * Function name: handlePublish(ObjectOutputStream outToClient, ObjectInputStream inFromClient)
	 * 
	 * Description: This function is used to handle a publish request. Publish
	 * request data is received via the ObjectOutputStream and ObjectInputStream
	 * parameters.
	 * 
	 * Once all of the publish data has been receieved, the new entry is added
	 * to the node's 'entries' map. Then, a call is made to the client to send
	 * an 'OK' response and the corresponding node trace data.
	 */
	public void handlePublish(ObjectOutputStream outToClient, ObjectInputStream inFromClient) {
		String fromClient;
		int hashTag;
		int nodeID;
		ArrayList<String> urlList = new ArrayList<String>();

		try {
			// Request tag
			outToClient.writeObject(SEND_TAG);
			outToClient.flush();
			hashTag = inFromClient.readInt();

			// Request URLs
			outToClient.writeObject(SEND_URL);
			outToClient.flush();
			fromClient = (String) inFromClient.readObject();

			while (!fromClient.equals(END)) {
				urlList.add(fromClient);
				fromClient = (String) inFromClient.readObject();
			}

			// Get client's hostname
			outToClient.writeObject(SEND_HOST);
			outToClient.flush();
			clientHost = (String) inFromClient.readObject();

			// Get client's port
			outToClient.writeObject(SEND_PORT);
			outToClient.flush();
			clientPort = inFromClient.readInt();

			// Get node trace
			outToClient.writeObject(SEND_TRACE);
			outToClient.flush();
			nodeID = inFromClient.readInt();

			while (nodeID != -1) {
				nodeTrace.add(nodeID);
				nodeID = inFromClient.readInt();
			}

			// Add entry
			myNode.entries.put(hashTag, urlList);

			// Send response to client
			sendClientOK();

		} catch (Exception e) {
			System.out.println("Exception caught: " + e);
			e.printStackTrace();
			System.exit(1);
		}
	}

	/*
	 * Function name: handleSubscribe(ObjectOutputStream outToClient, ObjectInputStream inFromClient)
	 * 
	 * Description: This function is used to handle a subscribe request.
	 * Subscribe request tags are received via the ObjectInputStream parameter.
	 * 
	 * Once all of the subscribe data has been receieved, the corresponding
	 * entries are retrieved from the 'entries' map. Then, a call is made to the
	 * client to send the entries and the corresponding node trace data.
	 */
	public void handleSubscribe(ObjectOutputStream outToClient, ObjectInputStream inFromClient) {
		try {
			// Request Tag
			outToClient.writeObject(SEND_TAG);
			outToClient.flush();

			int hashTag = inFromClient.readInt();

			// Get client's hostname
			outToClient.writeObject(SEND_HOST);
			outToClient.flush();
			clientHost = (String) inFromClient.readObject();

			// Get client's port
			outToClient.writeObject(SEND_PORT);
			outToClient.flush();
			clientPort = inFromClient.readInt();

			// Get node trace
			outToClient.writeObject(SEND_TRACE);
			outToClient.flush();
			int nodeID = inFromClient.readInt();

			while (nodeID != -1) {
				nodeTrace.add(nodeID);
				nodeID = inFromClient.readInt();
			}

			// Send entries to client
			ArrayList<String> urlList = new ArrayList<String>();

			urlList = myNode.entries.get(hashTag);
			sendClientURLs(hashTag, urlList);

		} catch (Exception e) {
			System.out.println("Exception caught: " + e);
			e.printStackTrace();
			System.exit(1);
		}
	}


    /*
     * Function name: forwardPrintMessage(int port)
     * 
     * Description: This function is used to forward a PRINT DATA request to
     * the node on the specified port.  The client port and hostname are passed
     * along to the node.
     */
    public void forwardPrintMessage(int port) {
    	Socket connected  = null;
    	ObjectOutputStream objectOut = null;
    	ObjectInputStream  objectIn  = null;

    	try {
    		connected = new Socket(HOST, port);
    		objectIn  = new ObjectInputStream(connected.getInputStream());
    		objectOut = new ObjectOutputStream(connected.getOutputStream());

    		// Send PRINT command to node
    		objectOut.writeObject(PRINT_DATA);
    		objectOut.flush();

    		// Send client port
    		objectOut.writeInt(clientPort);
    		objectOut.flush();

    		// Send client host
    		objectOut.writeObject(clientHost);
    		objectOut.flush();

    		
			// Wait for OK
			String fromclient = (String) objectIn.readObject();
			
			// Clean up
			if (fromclient.equals(OK)) {
	    		connected.close();
	    		objectIn.close();
	    		objectOut.close();
			}

    	} catch (Exception e) {
    		System.out.println("ERROR.");
    		e.printStackTrace();
    		System.exit(1);
    	}
    }


	/*
	 * Function name: sendClientOK()
	 * 
	 * Description: This function is called after a publish request has been
	 * received. It sends an 'OK' response to the client. In addition, it also
	 * sends the node trace data.
	 */
	public void sendClientOK() {
		Socket connected = null;
		ObjectOutputStream outToClient = null;

		try {
			connected   = new Socket(clientHost, clientPort);
			outToClient = new ObjectOutputStream(connected.getOutputStream());

			// Send nodeTrace
			for (int i = 0; i < nodeTrace.size(); i++) {
				outToClient.writeInt(nodeTrace.get(i));
				outToClient.flush();
			}

			// -1 Indicates that this is the end of the message
			outToClient.writeInt(-1);
			outToClient.flush();

			// Close connections
			connected.close();
			outToClient.close();

		} catch (IOException e) {
			System.out.println("Exception caught: " + e);
			e.printStackTrace();
			System.exit(1);
		}
	}

	
	/*
	 * Function name: sendClientURLs(int hashTag, ArrayList<String> urlList)
	 * 
	 * Description: This function is called after a subscribe request has been
	 * received. It sends the URL list to the client. In addition, it also sends
	 * the node trace data.
	 */
	public void sendClientURLs(int hashTag, ArrayList<String> urlList) {
		Socket connected = null;
		ObjectOutputStream outToClient = null;

		try {
			connected   = new Socket(clientHost, clientPort);
			outToClient = new ObjectOutputStream(connected.getOutputStream());

			// Send URLs
			if (urlList != null) {
				for (int i = 0; i < urlList.size(); i++) {
					outToClient.writeObject(urlList.get(i));
					outToClient.flush();
				}
			}

			outToClient.writeObject(END);
			outToClient.flush();

			// Send nodeTrace
			for (int i = 0; i < nodeTrace.size(); i++) {
				outToClient.writeInt(nodeTrace.get(i));
				outToClient.flush();
			}

			// -1 Indicates that this is the end of the message
			outToClient.writeInt(-1);
			outToClient.flush();

			// Close connections
			connected.close();
			outToClient.close();

		} catch (IOException e) {
			System.out.println("Exception caught: " + e);
			e.printStackTrace();
			System.exit(1);
		}
	}


	/*
	 * Function name: sendClientNodeData()
	 * 
	 * Description: This function is used to send node data to the client. It is
	 * called when a user selects "View DHT" from the client menu.
	 */
	public void sendClientNodeData() {
		Socket connected  = null;
		String fromClient = null;

		ObjectOutputStream outToClient = null;
		ObjectInputStream inFromClient = null;

		try {
			connected    = new Socket(clientHost, clientPort);
			outToClient  = new ObjectOutputStream(connected.getOutputStream());
			inFromClient = new ObjectInputStream(connected.getInputStream());

			// Node ID
			outToClient.writeInt(myNode.id);
			outToClient.flush();

			// Number of entries
			outToClient.writeInt(myNode.entries.size());
			outToClient.flush();

			// Successor ID
			outToClient.writeInt(myNode.successor().id);
			outToClient.flush();

			// Predecessor ID
			outToClient.writeInt(myNode.predecessor.id);
			outToClient.flush();

			// Finger table data
			for (int i = 1; i <= M; i++) {
				outToClient.writeObject(myNode.fingerTable[i]);
				outToClient.flush();
			}

			fromClient = (String) inFromClient.readObject();

			// End of data
			if (fromClient.equals(OK)) {
				outToClient.writeObject(END);
				outToClient.flush();
			}

			// Close connections
			connected.close();
			outToClient.close();
			inFromClient.close();

		} catch (Exception e) {
			System.out.println("Exception caught: " + e);
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	
	/*
	 * onRange(int keyID, int fromKey, int toKey)
	 * 
	 * Description: This function determines if keyID falls between fromKey and
	 * toKey.
	 */
	public boolean onRange(int keyID, int fromKey, int toKey) {
		boolean result;

		if (fromKey <= toKey) {
			result = (keyID >= fromKey) && (keyID <= toKey);
		} else {
			result = (keyID >= fromKey) || (keyID <= toKey);
		}
		return result;
	}
	
	/*
	 * Function name: invokeRemote(int remotePort, String remoteCommand, int index, boolean getTrace)
	 * 
	 * Description: This function is used to execute a command on a remote node.
	 * The possible commands are findSuccessor, findPredecessor, closestPreceedingFinger, and 
	 * updateFingerTable.
	 * 
	 * The remotePort parameter specfies the port to connect to, and the remoteCommand parameter 
	 * specifies which command to execute. The getTrace boolean value is used to specify whether 
	 * a node trace should be returned.
	 */
	public Object invokeRemote(int remotePort, Node nodeParam, String remoteCommand, int index, boolean getTrace) {
		ObjectInputStream  myInputObject  = null;
		ObjectOutputStream myOutputObject = null;
		
		Socket remoteNodeSocket = null;
		Object result = null;

		try {
			remoteNodeSocket = new Socket(HOST, remotePort);
			myOutputObject   = new ObjectOutputStream(remoteNodeSocket.getOutputStream());
			myInputObject    = new ObjectInputStream(remoteNodeSocket.getInputStream());

			if (remoteCommand.equals(FIND_SUCC)) {

				myOutputObject.writeObject(FIND_SUCC);
				myOutputObject.flush();
				myOutputObject.writeInt(index);
				myOutputObject.flush();
				myOutputObject.writeBoolean(getTrace);
				myOutputObject.flush();

				result = (Node) myInputObject.readObject();

			} else if (remoteCommand.equals(FIND_PRED)) {

				myOutputObject.writeObject(FIND_PRED);
				myOutputObject.flush();
				myOutputObject.writeInt(index);
				myOutputObject.flush();

				result = (Node) myInputObject.readObject();

			} else if (remoteCommand.equals(CLOSEST_PRE_FINGER)) {

				myOutputObject.writeObject(CLOSEST_PRE_FINGER);
				myOutputObject.flush();
				myOutputObject.writeInt(index);
				myOutputObject.flush();

				result = (Node) myInputObject.readObject();

			} else if (remoteCommand.equals(SET_PRED)) {

				myOutputObject.writeObject(SET_PRED);
				myOutputObject.flush();
				myOutputObject.writeObject(nodeParam);
				myOutputObject.flush();
				
			} else if (remoteCommand.equals(UPDATE_FINGER_TABLE)) {

				myOutputObject.writeObject(UPDATE_FINGER_TABLE);
				myOutputObject.flush();
				myOutputObject.writeObject(nodeParam);
				myOutputObject.flush();
				myOutputObject.writeObject(index);
				myOutputObject.flush();

				result = (String) myInputObject.readObject();

			} else if (remoteCommand.equals(GET_NODE)) {

				myOutputObject.writeObject(GET_NODE);
				result = (Node) myInputObject.readObject();

			} else {
				System.err.println("Attempting to execute invalid remote command ["+ remoteCommand + "]");
			}

			// If trace data was requested, read it from the input stream.
			if (getTrace) {
				int fromclient = myInputObject.readInt();

				while (fromclient != -1) {
					nodeTrace.add(fromclient);
					fromclient = myInputObject.readInt();
				}
			}

			// Clean up
			remoteNodeSocket.close();
			myOutputObject.close();
			myInputObject.close();

		} catch (Exception e) {
			System.out.println("Exception caught: " + e);
			e.printStackTrace();
			System.exit(1);
		}

		return result;
	}
}
