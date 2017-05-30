package chord;

/******************************************************************************
 * Filename: SuperNodeThread.java
 * 
 * Description: The SuperNodeThread class listens on a well-known port for
 * incoming connections from the client or other nodes. When a publish or
 * subscribe request is made, this thread will determine which node to forward
 * the request to (and then forward it).
 * 
 * Author: Kristin Dahl
 * Date: 9-6-2016
 *****************************************************************************/

import static chord.ChordUtil.END;
import static chord.ChordUtil.FIND_SUCC;
import static chord.ChordUtil.HOST;
import static chord.ChordUtil.MAX_NODES;
import static chord.ChordUtil.PUBLISH;
import static chord.ChordUtil.SEND_HOST;
import static chord.ChordUtil.SEND_PORT;
import static chord.ChordUtil.SEND_TAG;
import static chord.ChordUtil.SEND_TRACE;
import static chord.ChordUtil.SEND_URL;
import static chord.ChordUtil.SUBSCRIBE;
import static chord.ChordUtil.SUPERNODE_PORT_NUM;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;
import java.util.ArrayList;

public class SuperNodeThread extends NodeThread implements Serializable, Runnable {
	private static final long serialVersionUID = -8101231720383517072L;

	// SuperNodeThread constructor
	public SuperNodeThread(int port, int id, SuperNode sNode) {
		nodeTrace = new ArrayList<Integer>(MAX_NODES);
		myNode = sNode;
	}

	// Listen for incoming node connections on given port.
	public void run() {
		listen(SUPERNODE_PORT_NUM);
	}

	/*
	 * Function name: handlePublish(ObjectOutputStream outToClient, ObjectInputStream inFromClient)
	 * 
	 * Description: This function is initiated by a call from the client. The
	 * SuperNode gathers information from the client about the publish request.
	 * Then, it determines which node in the Chord system should handle the
	 * actual publish request. Then, the request is forwarded on to that node.
	 */
	public void handlePublish(ObjectOutputStream outToClient, ObjectInputStream inFromClient) {
		String fromClient = null;
		int hashTag = -1;
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
			clientHost = (String) inFromClient.readObject();

			// Get client's port
			clientPort = inFromClient.readInt();

			nodeTrace.add(myNode.id);
			if (onRange(hashTag, myNode.id, myNode.successor().id)) {
				myNode.entries.put(hashTag, urlList);
				sendClientOK();
			} else {
				Node newNode = findNode(hashTag, myNode.successor());

				if (newNode.id == myNode.id) {
					myNode.entries.put(hashTag, urlList);
					sendClientOK();
				} else {
					forwardPublishRequest(hashTag, urlList, newNode);
				}
			}

		} catch (Exception e) {
			System.out.println("Exception caught: " + e);
			e.printStackTrace();
			System.exit(1);
		}
	}

	
	/*
	 * Function name: handleSubscribe(ObjectOutputStream outToClient, ObjectInputStream inFromClient)
	 * 
	 * Description: This function is initiated by a call from the client. The
	 * SuperNode gathers information from the client about the subscribe
	 * request. Then, it determines which node in the Chord system should handle
	 * the actual subscribe request. Then, the request is forwarded on to that
	 * node.
	 */
	public void handleSubscribe(ObjectOutputStream outToClient, ObjectInputStream inFromClient) {
		int hashTag = -1;
		ArrayList<Integer> validTagList = new ArrayList<Integer>();

		try {
			// Request Tags
			outToClient.writeObject(SEND_TAG);
			outToClient.flush();
			hashTag = inFromClient.readInt();

			while (hashTag != -1) {
				validTagList.add(hashTag);
				hashTag = inFromClient.readInt();
			}

			// Get client port
			clientPort = inFromClient.readInt();
			nodeTrace.add(myNode.id);

			for (int i = 0; i < validTagList.size(); i++) {
				if (onRange(validTagList.get(i), myNode.id, myNode.successor().id)) {
					
					// Add to entries
					ArrayList<String> urlList = myNode.entries.get(validTagList.get(i));
					sendClientURLs(validTagList.get(i), urlList);
					
				} else {
					Node newNode = findNode(validTagList.get(i), myNode.successor());

					if (newNode.id == myNode.id) {
						ArrayList<String> urlList = myNode.entries.get(validTagList.get(i));
						sendClientURLs(validTagList.get(i), urlList);

					} else {
						forwardSubscribeRequest(validTagList.get(i), newNode);
					}
				}
			}

		} catch (Exception e) {
			System.out.println("Exception caught: " + e);
			e.printStackTrace();
			System.exit(1);
		}
	}

	
	/*
	 * Function name: forwardPublishRequest(int hashTag, ArrayList<String>urlList, Node n)
	 * 
	 * Description: This function forwards a publish request to node n.
	 */
	public void forwardPublishRequest(int hashTag, ArrayList<String> urlList, Node n) {
		String fromNode = null;
		Socket connected = null;
		ObjectOutputStream outToNode = null;
		ObjectInputStream inFromNode = null;

		try {
			connected = new Socket(HOST, n.port);
			inFromNode = new ObjectInputStream(connected.getInputStream());
			outToNode = new ObjectOutputStream(connected.getOutputStream());

			// Send PUBLISH command to node
			outToNode.writeObject(PUBLISH);
			outToNode.flush();

			fromNode = (String) inFromNode.readObject();
			if (fromNode.equals(SEND_TAG)) {
				outToNode.writeInt(hashTag);
				outToNode.flush();
				fromNode = (String) inFromNode.readObject();
			}

			if (fromNode.equals(SEND_URL)) {
				for (int i = 0; i < urlList.size(); i++) {
					outToNode.writeObject(urlList.get(i));
					outToNode.flush();
				}
				outToNode.writeObject(END);
				outToNode.flush();
				fromNode = (String) inFromNode.readObject();
			}

			if (fromNode.equals(SEND_HOST)) {
				outToNode.writeObject(this.clientHost);
				outToNode.flush();
				fromNode = (String) inFromNode.readObject();
			}

			if (fromNode.equals(SEND_PORT)) {
				outToNode.writeInt(clientPort);
				outToNode.flush();
				fromNode = (String) inFromNode.readObject();
			}

			if (fromNode.equals(SEND_TRACE)) {
				for (int i = 0; i < nodeTrace.size(); i++) {
					outToNode.writeInt(nodeTrace.get(i));
					outToNode.flush();
				}
				outToNode.writeInt(-1);
				outToNode.flush();
			}

			connected.close();
			outToNode.close();
			inFromNode.close();

		} catch (Exception e) {
			System.out.println("Exception caught: " + e);
			e.printStackTrace();
			System.exit(1);
		}
	}

	/*
	 * Function name: forwardSubscribeRequest(int hashTag, Node n)
	 * 
	 * Description: This function forwards a subscribe request to node n.
	 */
	public void forwardSubscribeRequest(int hashTag, Node n) {
		String fromNode = null;
		Socket connected = null;
		ObjectOutputStream outToNode = null;
		ObjectInputStream inFromNode = null;

		try {
			connected = new Socket(HOST, n.port);
			inFromNode = new ObjectInputStream(connected.getInputStream());
			outToNode = new ObjectOutputStream(connected.getOutputStream());

			// Send SUBSCRIBE command to node
			outToNode.writeObject(SUBSCRIBE);
			outToNode.flush();

			fromNode = (String) inFromNode.readObject();

			if (fromNode.equals(SEND_TAG)) {
				outToNode.writeInt(hashTag);
				outToNode.flush();
				fromNode = (String) inFromNode.readObject();
			}

			if (fromNode.equals(SEND_HOST)) {
				outToNode.writeObject(clientHost);
				outToNode.flush();
				fromNode = (String) inFromNode.readObject();
			}

			if (fromNode.equals(SEND_PORT)) {
				outToNode.writeInt(clientPort);
				outToNode.flush();
				fromNode = (String) inFromNode.readObject();
			}

			if (fromNode.equals(SEND_TRACE)) {
				for (int i = 0; i < nodeTrace.size(); i++) {
					outToNode.writeInt(nodeTrace.get(i));
					outToNode.flush();
				}
				outToNode.writeInt(-1);
				outToNode.flush();
			}

			// Close connections
			connected.close();
			inFromNode.close();
			outToNode.close();

		} catch (Exception e) {
			System.out.println("Exception caught.");
			e.printStackTrace();
			System.exit(1);
		}
	}

	/*
	 * Function name: findNode(int hashTag, Node n)
	 * 
	 * Description: This function returns the node that should be responsible
	 * for the given hashTag.
	 */
	public Node findNode(int hashTag, Node n) {
		Node succNode;

		if (n.id == myNode.id) {
			succNode = findSuccessor(hashTag);
		} else {
			succNode = (Node) invokeRemote(n.port, null, FIND_SUCC, hashTag, true);
		}

		if (succNode.id != myNode.id) {
			nodeTrace.add(succNode.id);
		}
		return succNode;
	}
}
