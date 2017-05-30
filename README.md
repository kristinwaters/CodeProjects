# Projects

/---------------------------------------------------------------------------------------------/
/ Overview
/---------------------------------------------------------------------------------------------/
This program is a simplified implementation of the BitTorrent protocol.  This system allows a 
user to add files to nodes, search for files, and download files.  It is written in the C 
programming language and consists of the following four components:

   1. Node
   2. Tracker
   3. Torrent Store
   4. Client

Each of these components communicate via RPC.  This simplified implementation has been designed
under the assumption that there will always be one client process, one tracker process, one 
torrent store process, and four node processes present.


/---------------------------------------------------------------------------------------------/
/ Component Description: Node
/---------------------------------------------------------------------------------------------/
The node is responsible for storing files and serving chunks of them when requested by the 
client.  There are two structures defined in node.x.  These are "file_rqst_data" and "file."
"file_rqst_data" contains information needed by the node to serve chunks of a file.  The "file"
structure is used as a parameter when storing a new file.  It holds the contents of the file, 
the filename, the file size, a "caller" boolean, and a pointer to the next file being hosted by 
the node.

The purpose of the "caller" boolean is to inform the Node of whether it should register a 
newly received file.  If a Node receives a file with the "caller" set to zero, then this 
indicates that the file is being sent by the Tracker for the purposes of file replication.  In 
this case, the node simply stores the file and does not try to re-register it with the tracker.

On the other hand, if the "caller" is set to one, then this indicates that the file is being 
sent by a client.  In this case, the node registers the file with the tacker.

There are also two functions defined within node.x.  STORE_NEW_FILE accepts a file structure 
as input and writes its contents.  If the "caller" boolean is set to one, then this function 
also registers the file with the tracker.

TRANSFER_FILE is responsible for sending pieces of a file to the requesting client.


/---------------------------------------------------------------------------------------------/
/ Component Description: Tracker
/---------------------------------------------------------------------------------------------/
The tracker process is responsible for keeping track of which nodes are storing which file 
within the distributed store.  It maintains a linked list of "file_data" structures.  Each 
element in this list contains a filename, file size, the number of requests received for the 
particular file, the number of nodes that currently host this file, and a pointer to the first 
of these nodes.

In addition to maintaining a file list, the tracker also maintains a node list.  This list is 
a linked list of type "node" structures.  Each of these structures contains a node's hostname 
and a pointer to its file data.  Each node's file data is contained in a "node_file" structure, 
which maintains the filename and file size for a particular file.

Within tracker.x there are four functions.  The first, NODE_LOOKUP_NEW, is called by the client
and returns the hostnames of two nodes that do not have the specified file.  These hostnames are
contained within the "two_nodes" data structure.  If two nodes cannot be found, then the
"two_nodes" structure is returned empty.

NODE_LOOKUP_QUERY is called by the client.  Its purpose is to return a list of nodes that 
contain the specified filename.  Its input is a structure of type "query_param." "query_param" 
contains the filename to look for and also a "caller" boolean.  The "caller" boolean specifies 
whether this query is for a file download or just a simple query.  If "caller" is set to zero, 
then this indicates that the client does not intend to download the file.  If "caller" is set to 
one, then the query request is being done for a file download.  In this case, the number of 
requests for the given file is incremented.  Once the number of requests for a given file 
reaches the request threshold parameter, then the tracker sends a copy of the file to a node 
that is not currently hosting the file.

NODE_REGISTER is called by each node upon initialization.  This function accepts the node's
hostname and adds it to the node linked list.

FILE_REGISTER is called by a node whenever it receives a new file.  This function accepts node 
and file information.  If the specified file is already being hosted by another node, then the 
file list is updated to indicate that the file is being hosted by an additional node.  If the 
file is not currently in the file list, then a new entry is added.


/---------------------------------------------------------------------------------------------/
/ Component Description: Torrent Store
/---------------------------------------------------------------------------------------------/
The torrent store process keeps track of the torrent file for each file stored in the system.
Each torrent file consists of the following format:

    <Tracker Hostname/Filename>

The "Tracker Hostname" field consists of the hostname which can be used to contact the tracker.
The "Filename" field is used by the tracker to identfy a file.  The name of the torrent file is 
of the following format:

    tor_filename

torrent_store.x defines four functions.  The first of these, LOCATION_TRACKER, is called by the
client.  It accepts no parameters, and returns the hostname of the tracker.

CREATE_TORRENT accepts a filename as a parameter and creates a torrent file for the specified file.

QUERY_FILE accepts a filename as a parameter and returns the contents of the torrent file.  It 
is called by the client.

TRACKER_REGISTER is called by the tracker upon initialization.  This function accepts the
tracker's hostname.


/---------------------------------------------------------------------------------------------/
/ Component Description: Client
/---------------------------------------------------------------------------------------------/
The client process is used by the end user to add files, query files, and download files.  It is
implemented as a command-line interface.  Upon startup, the following menu is displayed to the
user:

    Welcome to the BitTorrent client.

    --------- Menu ---------
    Add File      - Press 1
    Query File    - Press 2
    Download File - Press 3
    Quit          - Press 4
    ------------------------

    Select a command:

From there, the user may select one of four options.  With each option (except for option 4), 
the client will prompt the user for a filename, execute the command, and then display the results.
