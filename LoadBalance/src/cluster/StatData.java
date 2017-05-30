package cluster;

/******************************************************************************
 * Filename: StatData.java
 * Description:  This class defines the status data that is returned to the
 *               client upon a status request (option 2 from the client menu).
 *               A StatData object is created for each node in the system.
 *
 * Author: Kristin Dahl
 * Date: 11-20-2015
 *****************************************************************************/

import java.io.Serializable;

public class StatData implements Serializable {
	private static final long serialVersionUID = -8101231720383517072L;

	public int jobsReceived;
	public int jobsMigrated;

	public double currentLoad;
	public double averageLoad;

	public String hostname;

	// Default StatData constructor
	public StatData() {}
}
