/*******************************************************************************
 * Filename: node.x
 * Description:  This is an IDL file that defines the interface for the Node. 
 *               This file defines the remote procedures for 
 *               LOCATE_TRACKER, CREATE_TORRENT, and QUERY_FILE.
 *                
 * Author: Kristin Dahl
 * Date: 3-2-2015
 *****************************************************************************/

const MAXHOSTNAME = 32;     /* max length of a user name */
const MAXFILELEN  = 65535;  /* max length of a file */
const MAXNAMELEN  = 255;    /* max length of a file name */

/* Client request data */
struct file_rqst_data {
   string filename<MAXNAMELEN>;
   int    file_position;
   int    chunk_size;
};

/* A complete file: */
struct file {
   string filename<MAXNAMELEN>;
   string file_contents<MAXFILELEN>;
   int    caller;
   int    file_size;
   struct file * next;
};

program NODE_PROG {
   version NODE_VERS {
    
      /* Method to store a new file */
      int STORE_NEW_FILE(file) = 1;
        
      /* Method to send a file */
      string TRANSFER_FILE(file_rqst_data) = 2;
        
   } = 1;
} = 0x20000001;
