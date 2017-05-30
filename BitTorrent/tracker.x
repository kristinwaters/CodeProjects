/*******************************************************************************
 * Filename: tracker.x
 * Description:  This is an IDL file that defines the interface for the Tracker. 
 *               This file defines the remote procedures for 
 *               LOCATE_TRACKER, CREATE_TORRENT, and QUERY_FILE.
 *                
 * Author: Kristin Dahl
 * Date: 3-2-2015
 *****************************************************************************/

const MAXHOSTNAME = 32;     /* max length of a host name */
const MAXFILELEN  = 65535;  /* max length of a file */
const MAXNAMELEN  = 255;    /* max length of a file name */

/* Param struct for file */
struct file_data {
 	string filename<MAXNAMELEN>;        /* name of file */
    int    file_size;
    int    num_requests;
	int    num_nodes;
	
	struct node * node_list_head;
	struct node * node_list_tail;
	
    struct file_data * next;
};

struct two_nodes {
   string hostname1<MAXHOSTNAME>;
   string hostname2<MAXHOSTNAME>;
};

struct node_file {
   string filename<MAXNAMELEN>;
   int    file_size;
   struct node_file * next;
};

struct node {
   string hostname<MAXHOSTNAME>;
   struct node_file * node_file_head;
   struct node_file * node_file_tail;
   struct node * next;
};

/* Return value for query */
struct query_info {
   int file_size;
   int num_nodes;
   struct node * nodes;
};

struct query_param {
   int caller;
   string filename<MAXNAMELEN>;
};

program TRACKER_PROG {
	version TRACKER_VERS {
    
		/* Method to lookup nodes to host a new file */
		two_nodes NODE_LOOKUP_NEW(string) = 1;
        
        /* Method to find nodes that contain the specified file */
        query_info NODE_LOOKUP_QUERY(query_param) = 2;
		
		/* Method to register new nodes */
		int NODE_REGISTER(string) = 3;
		
		/* Method to register new files */
		int FILE_REGISTER(node) = 4;
        
	} = 1;
} = 0x20000003;
