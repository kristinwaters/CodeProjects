/*******************************************************************************
 * Filename: torrentstore.x
 * Description:  This is an IDL file that defines the interface for the Torrent 
 *               Store.  This file defines the remote procedures for 
 *               LOCATE_TRACKER, CREATE_TORRENT, and QUERY_FILE.
 *                
 * Author: Kristin Dahl
 * Date: 3-2-2015
 *****************************************************************************/
 
const MAXHOSTNAME = 32;     /* max length of a host name */
const MAXFILELEN  = 65535;  /* max length of a file */
const MAXNAMELEN  = 255;    /* max length of a file name */

/* Param struct for torrent file */
struct torrent_file {
    string filename<MAXNAMELEN>;
	struct torrent_file * next;
};

program TORRENT_STORE_PROG {
	version TORRENT_STORE_VERS {
    
		/* Method to locate tracker */
		string LOCATE_TRACKER() = 1;
        
        /* Method to create a torrent file */
        int CREATE_TORRENT(string) = 2;
        
        /* Method to lookup a torrent file */
        string QUERY_FILE(string) = 3;
		
        /* Method to register new trackers */
		int TRACKER_REGISTER(string) = 4;
		
	} = 1;
} = 0x20000002;
