package cluster;

/******************************************************************************
 * Filename: Util.java
 * Description:  This class defines global variables that are used by the
 * 				 load-balanced compute cluster system.
 *
 * Author: Kristin Dahl
 * Date: 11-20-2015
 *****************************************************************************/

public class Util
{
	// The following variables can be modified to fit the user's preference.
    /*************************************************************************/
    public static final double LOAD_THRESHOLD = 0.2;

	public static final String HOST_LIST[] = { "cs1260-02", "cs1260-03", "cs1260-04" };
    public static final String SERVER_HOST = "cs1260-01";
    public static final String CLIENT_HOST = "cs1260-01";

    public static final int SERVER_PORT = 5370;
    public static final int CLIENT_PORT = 5380;
    public static final int NODE_PORT   = 5375;
    /*************************************************************************/

    public static final String LOAD_PATH   = "/proc/loadavg";

    public static final int END_NUM      = -1;
    public static final int BAD_COMMAND  = -2;
    public static final int BAD_FILENAME = -3;

    public static final String OK  = "OK";
    public static final String ERR = "ERR";
    public static final String END = "END";

    public static final String RESULT = "RESULT";
    public static final String SUBMIT = "SUBMIT";
    public static final String STATS  = "STATS";

    public static final String DO_SUB_JOB = "SUB_JOB";
    public static final String SEND_CMD   = "SEND CMD";

    public static final String GET_LOAD_VALUE = "GET_LOAD_VALUE";
}
