package chord;

/******************************************************************************
 * Filename: Finger
 * 
 * Description:  This file defines the Finger data class.  A Finger object
 * is used within a node's finger table.
 *
 * Author: Kristin Dahl
 * Date: 9-6-2016
 *****************************************************************************/

import java.io.Serializable;

public class Finger implements Serializable {
	private static final long serialVersionUID = 7733281083158892217L;

	public int start;
	public Node node;

	public Finger(Node n) {
		this.node = n;
	}
}
