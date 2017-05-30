# Chord Protocol

## Overview
This program is a simplified implementation of the Chord protocol.  This system allows a
user to publish and subscribe-to tag/URL pairs.  It is written in the Java programming language
and consists of the following components:

   1. Nodes
   2. SuperNode
   3. Client

Each of these components communicate via sockets.  This simplified implementation has been designed
under the assumption that there will always be one client process, one SuperNode, and a minimum of
nine Nodes (excluding the SuperNode) present.


## Component Description: Node
Each node is responsible for storing tag-to-URL mappings, and together the nodes form Chord's
distributed hash table (DHT).  Each node has its own finger table which contains information
about the node.

Each Node object is created and maintained by a NodeThread object.  The NodeThread listens on
an assigned port for incoming messages from other NodeThreads.  Whenever a message is received by
the NodeThread, the NodeThread handles the message and then returns to its listening state.

The following is a brief overview of each message that the NodeThread can handle:

     1.  JOIN - The JOIN message is sent by the SuperNode upon system startup.  When a JOIN
         message is received, the node initializes its finger table and then alerts other nodes
         in the network that they may need to update their own finger tables.

     2.  PUBLISH - The PUBLISH message is sent by the SuperNode.  When this message is
         received, the node retrieves the necessary information from the calling entity,
         adds the new tag and url data to the node's "entries" table, and then returns a
         confirmation message to the client.  Also, a trace of all nodes contacted for the PUBLISH
         request is sent to the client.

     3.  SUBSCRIBE - The SUBSCRIBE message is sent by the SuperNode.  When this message is
         received, the node retrieves the tag values from the calling entity and then looks
         up the tag(s) in its own "entries" table.  Then, the node returns all URLs associated
         with the tag(s) to the client.  Also, a trace of all nodes contacted for the SUBSCRIBE
         request is sent to the client.

     4.  GET NODE - The GET NODE message is sent by other NodeThreads.  When this message is
         received, the NodeThread returns a copy of its node to the calling entity.

     5.  FIND PRED - The FIND PRED message is sent by other NodeThreads.  When this message is
         received, an id value is also received.  The NodeThread then executes the findPredecessor
         method with the aforementioned id value and returns the result to the calling entity.

     6.  FIND SUCC - The FIND SUCC message is sent by other NodeThreads.  When this message is
         received, an id value and a trace boolean value are also received.  The trace boolean
         indicates to the NodeThread whether or not it should keep track of each node that it
         communicates with.  After receiving this message, the NodeThread executes findSuccessor
         with the aforementioned id value and returns the result to the calling entity.

     7.  SET PRED - The SET PRED message is sent by a node from within initFingerTable.  When this
         message is received, a node object is also received.  After receiving this message, the
         NodeThread sets its predecessor node to the received node object.

     8.  UPDATE FINGER TABLE - The UPDATE FINGER TABLE message is sent by other NodeThreads.  When
         this message is received, a node object and an index value are also received.  After
         receiving this message, the NodeThread executes updateFingerTable and returns the result to
         the calling entity.

     9.  CLOSEST PRE FINGER - The CLOSEST PRE FINGER message is sent by other NodeThreads.  When
         this message is received, an id value is also received.  After receiving this message, the
         NodeThread executes the closestPrecedingFinger method and returns the result to the calling
         entity.

     10. PRINT DATA - The PRINT DATA message is sent by other NodeThreads.  When this message is
         received,the client hostname and port are also received.  After receiving this message, the
         NodeThread executes the sendClientNodeData method and returns a response to the calling 
         entity.


## Component Description: SuperNode
The SuperNode class extends the Node class.  A SuperNode is basically the same thing as a regular
Node object, except that it is created and maintained by a SuperNodeThread instead of a NodeThread.

The SuperNode is a node that listens on a well-known port.  It acts as a regular node, except
that upon system start-up, it generates each node ID and then directs each of the regular nodes
to join the DHT.  In addition, all client messages are sent to the SuperNodeThread.  The
SuperNodeThread parses the message and determines which of the regular nodes to forward the
message to.  The SuperNodeThread then forwards the message and returns to its listening state.


## Component Description: Client
The client process is used by the end user to publish, subscribe, and view the DHT structure.  It is
implemented as a command-line interface.  Upon startup, the following menu is displayed to the
user:

     Welcome to the Chord-based publish/subscribe system.

     ********* Menu *********
     Publish  ------  Press 1
     Subscribe  ----  Press 2
     View DHT  -----  Press 3
     Exit  ---------  Press 4
     ************************

Enter a command:

From there, the user may select one of four options.  For further details regarding the usage of
the client, see the User Document.


# Usage

## System Startup
There are two script files included with this program.  The first script file, build.sh, compiles 
the Java code and puts it into a jar file.  The second script file, runChord.sh, launches the Chord
system.  When running these scripts, ensure that they are located within the PA2 directory.  Also, 
ensure that you have permission to execute the scripts.  To do this, type the following at the
command prompt:
                                   chmod +x build.sh runChord.sh

In order to compile the program, open the program directory (PA2).  There should be a src directory, 
the build.sh script, and the runChord.sh script.  Type the following at the command prompt:

                                           ./build.sh

After this script is finished executing, there should be a lib directory within PA2.  To launch 
the Chord system, type the following at the command prompt:

                                         ./runChord.sh
                                   
This command will start up the nodes, the SuperNode, and present a client display.  Upon a successful
startup, something similar to the following should appear at the console:

     dahl0616@cs1260-01 (/home/it04/dahl0616/PA2) % ./runChord.sh
     Node started on port 55356
     Node started on port 55350
     Node started on port 5358
     Node started on port 5359
     Node started on port 5360
     Node started on port 5361
     Node started on port 5362
     Node started on port 5363
     Node started on port 5364
     Starting SuperNode...

     Sending JOIN request to node on port 55356

     Node 2 received join request.
     Sending JOIN request to node on port 55350

     Node 36 received join request.
     Sending JOIN request to node on port 5358

     Node 10 received join request.
     Sending JOIN request to node on port 5359

     Node 28 received join request.
     Sending JOIN request to node on port 5360

     Node 39 received join request.
     Sending JOIN request to node on port 5361

     Node 50 received join request.
     Sending JOIN request to node on port 5362

     Node 32 received join request.
     Sending JOIN request to node on port 5363

     Node 7 received join request.
     Sending JOIN request to node on port 5364

     Node 53 received join request.

     All nodes have joined.
     Welcome to the Chord-based publish/subscribe system.

     ********* Menu *********
     Publish  ------  Press 1
     Subscribe  ----  Press 2
     View DHT  -----  Press 3
     Exit  ---------  Press 4
     ************************

     Enter a command:


         
## Client Operation -- Publish
To publish a tag and a set of URLs to the distributed hash table, select 1 at the menu and 
then press enter.  The system will then prompt for a tag:

     ------- Publish -------
     Please enter a tag: 


Enter a tag and then press enter.  The system will then prompt for a URL to associate 
with the tag:

     ------- Publish -------
     Please enter a tag: mytag
     Please enter URL:


Enter a URL and then press enter.  The system will then ask whether or not you would like to 
associate an additional URL with the tag.  Enter 'y' for yes and 'n' for no:

     ------- Publish -------
     Please enter a tag: mytag
     Please enter URL: www.myurl.com
     Add another URL? y/n: 


If you select 'y', the system will prompt you to enter another URL:

     ------- Publish -------
     Please enter a tag: mytag
     Please enter URL: www.myurl.com
     Add another URL? y/n: y
     Please enter URL: www.anotherurl.com
     Add another URL? y/n: 


If you select 'n', the system will proceed with the publish operation:

     ------- Publish -------
     Please enter a tag: mytag
     Please enter URL: www.myurl.com
     Add another URL? y/n: y
     Please enter URL: www.anotherurl.com
     Add another URL? y/n: n

     Hashed tag is: 1
     Sending publish data...
     Publish completed successfully!

     ------ Node Trace ------
     Step 0 - Node ID is: 1


The system will return the numerical hashed tag associated with the user-entered tag and 
a confirmation message indicating whether the operation was successful.  In addition, the 
path of nodes followed to find the proper node ID to associate with the tag value is displayed.
Finally, the menu screen will reappear.


## Client Operation -- Subscribe
To query the URLs associated with a specific tag(s), select 2 at the menu and then press
enter.  The system will then prompt for a query:

     ------- Subscribe -------
     Please enter a query: 

Queries may take one of the following forms:

     1.  tag
     2.  tag1 AND tag2
     3.  tag1 OR tag2
     4.  tag1 AND tag2 AND tag3
     5.  tag1 AND tag2 OR tag3
     6.  tag1 OR tag2 AND tag3
     7.  tag1 OR tag2 OR tag3


Enter a query and then press enter.  The system will inform you if you enter a malformed query:

     ------- Subscribe -------
     Please enter a query: Malformed AND

     Invalid query.


If the query is accepted, the system will proceed with the subscription operation:

     ------- Subscribe -------
     Please enter a query: mytag
     Hashed tag is: 1

     Sending subscribe data...

     URLs found:
     - www.myurl.com
     - www.anotherurl.com

     ------ Node Trace ------
     Step 0 - Node ID is: 1


The system will return the numerical hashed tag associated with the user-entered tag(s) and 
the URLs that were found for the given tags.  In addition, the path of nodes followed to 
find the proper node ID to associate with the tag value is displayed.  Finally, the menu screen 
will reappear.


## Client Operation -- View DHT
To view the current structure of the distributed hash table, select 3 at the menu and then press
enter.  The system will proceed to print out information about each node in the system and then
the menu will reappear.  The output will look similar to the following:

     Enter a command: 3
     ===== Node Data =====
     Node ID: 1
     Entries: 1
     Successor ID: 4
     Predecessor ID: 29

         Finger Table:

     index   start   node
     ====================
       1       2      4
       2       3      4
       3       5      9
       4       9      9
       5      17     18


## Client Operation -- Exit
To terminate the client process, select 4 at the menu and then preess enter.  The client will 
display a message and then exit:

     Enter a command: 4
     Now exiting.  Bye!
