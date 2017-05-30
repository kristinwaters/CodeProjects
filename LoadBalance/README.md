# Load Balanced Distributed Computing

## Overview
This program is a simple implementation of a load-balanced distributed computing
system.  This system allows a user to submit jobs to the server, which then
arbitrarily assigns each of these jobs to a compute node.  The compute nodes 
perform load balancing and also process the jobs submitted by the client.

This system is written in the Java programming language and consists of the 
following components:

   1. Compute Nodes
   2. Server
   3. Client

Each of these components communicate via sockets.  This simplified 
implementation has been designed under the assumption that each compute node 
will be running on a separate physical machine.


## Component Description: Compute Node
Each node is responsible for executing jobs sent to it by the server.  Also, 
each node must perform load-balancing if its processing load is higher than the
user-specified threshold.

Each node is maintained by a ComputeNodeThread object.  The ComputeNodeThread 
listens on an assigned port for incoming messages from other nodes and/or the
server.  Whenever a message is received by the ComputeNodeThread, the node
handles the message and then returns to its listening state.

The following is a brief overview of each message that the ComputeNodeThread 
can handle:

     1.  DO_SUB_JOB - The DO_SUB_JOB message is sent by either the server or 
         other nodes whenever they forward a job command to another node.  
         
         When a DO_SUB_JOB message is received, the node increments the number 
         of jobs it has received.  Then, the node retrieves the command and 
         filename from the calling entity and calls handleSubJob.  
         
         handleSubJob performs a load-balancing check and, if necessary, 
         forwards the command to a different node.  Otherwise, if the current 
         node is executing the job, the node creates a new instance of 
         ProcessRunner, which executes the process and then returns the result 
         to the server.
         
         When a DO_SUB_JOB command is sent from one node to another, the sender 
         node increments the number of migrations it has initiated.


     2.  GET_LOAD_VALUE - The GET_LOAD_VALUE message is sent by other nodes
         when trying to perform load-balancing.  Upon receipt of this message,
         a node calls the getCurrentLoad() function, which returns the nodes'
         current processing load.  This value is then returned to the calling
         entity.
         

     3.  STATS - The STATS message is sent by the server whenever a request is 
         made for status information.  When this message is received, the node 
         returns the following data to the server:
            - Its hostname
            - The number of jobs received
            - The number of jobs migrated
            - Its current load value
            - Its average load value


## Component Description: Server
The server is responsible for handling client requests.  Upon receipt of a new 
job command, the server will forward the request to arbitrary nodes for 
execution.  Upon receipt of a status command, the server will request status
information from each node on the system.

The server is maintained by a ServerThread object.  The server listens on a 
well-known port for incoming client requests and incoming process results. 
Whenever a message is received by the ServerThread, the server handles the 
message and then returns to its listening state.

The following is a brief overview of each message that the ServerThread can 
handle:

     1.  SUBMIT - The SUBMIT message is sent by the client when a request is 
         made to execute a remote job.  Upon receipt of this message, the server
         resets its job variables from the previous job.  Then, the server 
         retrieves the command and filenames from the client and forwards 
         the information to a compute node(s).
         
      
     2.  STATS - The STATS message is sent by the client whenever a request is 
         made for status information.  When this message is received, the server
         creates a StatData object for each node in the system and then sends a
         STATS message to each of these nodes.  Once status data has been 
         collected for each node, the server returns its list of StatData 
         objects to the client.

         
     3.  RESULT - The RESULT message is sent by a nodes' ProcessRunner object 
         upon job completion.  When this message is received, the server 
         receives the amount of time needed to complete the job.  If all jobs 
         have been completed (that is, completedJobs == totalJobs), then the 
         results are returned to the client.


     4.  ERR - The ERR message is sent by a nodes' ProcessRunner object in the
         event that an error occurs while trying to complete the job.  When this
         message is received, the server receives an error code that indicates
         the type of error that occured.  If all jobs have been completed 
         (that is, completedJobs == totalJobs), then the results are returned to the client.


## Component Description: Client
The client process is used by the end user to submit jobs to the system and 
also to retrieve status information about each node in the system.  It is
implemented as a command-line interface.  Upon startup, the following menu is 
displayed to the user:

     Welcome to the load-balanced compute cluster.

     ********* Menu *********
     Submit Job ----  Press 1
     View Stats ----  Press 2
     Exit  ---------  Press 3
     ************************

     Enter a command: 


From there, the user may select one of three options.  For further details 
regarding the usage of the client, see the User Document.



# Usage

## System Settings
Prior to compiling the Java code, there are several parameters that can be 
altered within Util.java.  To modify the threshold value, change the 
LOAD_THRESHOLD value.  The default value for this variable is 0.2.

The HOST_LIST array contains the hostnames of all nodes running on the system.  
All node hostnames must be included in this array prior to compilation.  The 
default values in this array are cs1260-02, cs1260-03, and cs1260-04.

The SERVER_HOST and CLIENT_HOST variables contain the hostname of the server and
client, respectively.  Their default values are both cs1260-01.


## Compilation
In order to compile the program, open the program directory (PA3).  It should 
contain the following items:

   - Documentation directory
   - src directory
   - build.sh
   - runClient.sh
   - runNode.sh
   - runServer.sh
   - matrix.c
   - in1.txt, in2.txt, in3.txt, and in4.txt

There are four script files included with this program.  When running these 
scripts, ensure that they are located within the PA3 directory.  Also, ensure 
that you have permission to execute these scripts.  To do this, navigate to the 
PA3 directory and type the following at the command prompt:

                                chmod +x *.sh

The first script file, build.sh, compiles the Java code and puts it into a jar 
file.  In additon, this script compiles matrix.c, a matrix multiplication 
program that was developed for use with this program.

To compile the entire program, type the following at the command prompt:

                                 ./build.sh

After this script is finished executing, there should be a lib directory within 
PA3.  


## System Startup
At this point, ensure that you have a seperate terminal window open for the
client, the server, and each of the nodes.  

To launch a compute node, navigate to the PA3 directory and type the following 
at the command prompt:
                                 ./runNode.sh


To launch the server, navigate to the PA3 directory and type the following at 
the command prompt:
                                 ./runServer.sh


To launch the client, navigate to the PA3 directory and type the following at 
the command prompt:
                                 ./runClient.sh                                    
                                    
                                   

## Client Operation -- Submit Job
To submit a command and filename(s) to the distributed system, select 1 (Submit 
Job) at the menu and then press enter.  The system will then prompt for a job 
command:

     ------- Submit Job -------
     Enter a job command: 
     

Enter a command and one or more filenames and then press enter.  The job command
should be in the following format:

                      command file1 file2 ... fileN

If an invalid job command is entered, the system will return an error message 
and return to the menu.  If the command is valid, the client will send the 
command to the server.  When a job has successfully completed, the job results 
will be presented to the user:

     ------- Job Results -------
     Total time: 16.106 seconds

     Individual Job Times:
     Job 1: 0.27 seconds
     Job 2: 1.765 seconds
     Job 3: 15.926 seconds
     Job 4: 15.99 seconds
     Job 5: 15.849 seconds
     Job 6: 16.097 seconds
     ---------------------------

Upon job completion, the menu will reappear.


## Client Operation -- View Stats
To view information about each of the compute nodes currently in the system, 
select 2 (View Stats) at the menu and then press enter.  The client will request
node information and then present it to the user as follows:

     ------- Stat Data -------

     Node 1: cs1260-01
     Total jobs received: 12
     Total jobs migrated: 0
     Current load: 0.26
     Average load: 0.13

     Node 2: cs1260-02
     Total jobs received: 14
     Total jobs migrated: 3
     Current load: 0.2
     Average load: 0.18

     ---------------------------


Upon completion, the menu will reappear.


## Client Operation -- Exit
To terminate the client process, select 3 (Exit) at the menu and then press 
enter.  The client will display a message and then exit:

     Enter a command: 3
     Now exiting.  Bye!

--------------------------------------------------------------------------------


