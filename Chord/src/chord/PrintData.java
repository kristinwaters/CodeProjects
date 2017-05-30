package chord;

/******************************************************************************
 * Filename: PrintData.java
 * 
 * Description: The file defines the data needed to display the DHT structure.
 * 
 * Author: Kristin Dahl
 * Date: 9-6-2016
 *****************************************************************************/

import java.util.ArrayList;

public class PrintData {
	int nodeID;
	int numEntries;
	int succID;
	int predID;

	ArrayList<Finger> fingers;

	public PrintData() {
		fingers = new ArrayList<Finger>();
	}
}
