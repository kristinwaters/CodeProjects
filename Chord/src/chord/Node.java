package chord;

/******************************************************************************
 * Filename: Node.java
 * 
 * Description:  This file defines the Node data class.  Each node has its own
 * ID, finger table, port, and entries map.
 *
 * Author: Kristin Dahl
 * Date: 9-6-2016
 *****************************************************************************/

import static chord.ChordUtil.M;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Node implements Serializable {
	private static final long serialVersionUID = -7357466511459361679L;

	public int id;
	public int port;
	public Finger[] fingerTable;
	public Node predecessor;
	public Map<Integer, ArrayList<String>> entries;

	
	public Node(int port) {
		this.port = port;
		this.entries = new HashMap<Integer, ArrayList<String>>();
		this.fingerTable = new Finger[M + 1];
	}
	
	public void setID(int id) {
		this.id = id;
	}

	// Default constructor
	public Node() {}
	
	
	/*
	 * Function name: successor()
	 * 
	 * Description: This function simply returns the first index of the node's
	 * finger table.
	 */
	public Node successor() {
		return fingerTable[1].node;
	}
}
