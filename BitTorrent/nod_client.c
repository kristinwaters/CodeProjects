/*******************************************************************************
 * Filename: nod_client.c
 * Description:  This file contains the necessary functions to run the node
 *               client of a simplified BitTorrent protocol.
 *                
 * Author: Kristin Dahl
 * Date: 3-2-2015
 *****************************************************************************/

#include "nod.h"
#include "trkr.h"
#include <stdio.h>
#include <stdlib.h>
#include <string.h>


/* Function:    get_file_copy
 * 
 * Description: This function sends an entire file at once. 
 *              It is called by the tracker to replicate a file.
 ****/
char * get_file_copy(char *node_hostname, struct file_rqst_data *rqst_data) 
{
    // Create node client.
    CLIENT * node_clnt = clnt_create(node_hostname, NODE_PROG, NODE_VERS, "tcp");
    if (node_clnt == NULL) {
        clnt_pcreateerror(node_hostname);
        return "";
    }
   
    // Retrieve file
    char * buffer = (char *)malloc(MAXFILELEN);
    strcpy(buffer, *transfer_file_1(rqst_data, node_clnt));
    if (strcmp(buffer, "") == 0) {
        clnt_perror(node_clnt, "call failed:");
        return "";
    }
    clnt_destroy(node_clnt);
    return(buffer);
}


/* Function:    send_to_node
 * 
 * Description: This function is called by the client (and the tracker) 
 *              to store a new file.
 ****/
int send_to_node(struct file *file_data, char *node_hostname) 
{
    // Create node client
    CLIENT * node_clnt = clnt_create(node_hostname, NODE_PROG, NODE_VERS, "tcp");
    if (node_clnt == NULL) {
        clnt_pcreateerror(node_hostname);
        return 0;
    }

    int * store_result = store_new_file_1(file_data, node_clnt);
    if (*store_result == 0) {
        clnt_perror(node_clnt, "call failed:");
        return 0;
    }
    clnt_destroy(node_clnt);
    return 1;
}


/* Function:    get_file_chunk
 * 
 * Description: This function sends pieces of a file to a client.  
 *              It is called by the client to download a file.
 ****/
char * get_file_chunk(struct file_rqst_data *rqst_data, char *node_hostname) 
{
    // Create node client
    CLIENT *node_clnt = clnt_create(node_hostname, NODE_PROG, NODE_VERS, "tcp");
    if (node_clnt == NULL) {
        clnt_pcreateerror(node_hostname);
        return (char*)NULL;
    }
   
    // Send data
    char * buffer = (char *)malloc(rqst_data->chunk_size);
    buffer = *transfer_file_1(rqst_data, node_clnt);
    if (buffer == NULL) {
        clnt_perror(node_clnt, "call failed:");
    }
    clnt_destroy(node_clnt);    
    return buffer;   
} 
