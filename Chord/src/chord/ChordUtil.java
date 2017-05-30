package chord;

/******************************************************************************
 * Filename: ChordUtil.java
 * 
 * Description: The file defines global data that is used throughout the Chord
 * system.
 * 
 * Author: Kristin Dahl
 * Date: 9-6-2016
 *****************************************************************************/

public class ChordUtil {
	public static final int M = 6;
	public static final int MAX_NODES = 10;
	public static final int SUPERNODE_ID = 1;
	public static final String HOST = "localhost";

	// PORT_LIST is the list of ports that the nodes will be running on.
	public static final int SUPERNODE_PORT_NUM = 5370;
	public static final int PORT_LIST[] = { SUPERNODE_PORT_NUM, 55356, 55350, 5358, 5359, 5360, 5361, 5362, 5363, 5364 };

	public static final String OK   = "OK";
	public static final String ERR  = "ERR";
	public static final String END  = "END";
	public static final String JOIN = "JOIN";

	public static final String PUBLISH    = "PUBLISH";
	public static final String SUBSCRIBE  = "SUBSCRIBE";
	public static final String PRINT_DATA = "PRINT DATA";

	public static final String GET_NODE  = "GET NODE";
	public static final String GET_SUCC  = "GET SUCC";
	public static final String FIND_SUCC = "FIND SUCC";
	public static final String FIND_PRED = "FIND PRED";
	public static final String SET_PRED  = "SET PRED";

	public static final String UPDATE_FINGER_TABLE = "UPDATE FINGER TABLE";
	public static final String CLOSEST_PRE_FINGER  = "CLOSEST PRE FINGER";

	public static final String SEND_TAG   = "SEND TAG";
	public static final String SEND_URL   = "SEND URL";
	public static final String SEND_TRACE = "SEND TRACE";

	public static final String SEND_HOST = "SEND HOST";
	public static final String SEND_PORT = "SEND PORT";
}
