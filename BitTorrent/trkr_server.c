/*******************************************************************************
 * Filename: trkr_server.c
 * Description:  This file contains the necessary functions to run the tracker
 *               server of a simplified BitTorrent protocol.
 *
 * Author: Kristin Dahl
 * Date: 3-2-2015
 *****************************************************************************/

#include "nod.h"
#include "tstore.h"
#include "trkr.h"
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

// Function prototypes
char *get_file_copy(char *node_hostname, struct file_rqst_data *rqst_data);
int  send_to_node(struct file *new_file, char *node_hostname);
void update_node_file_list(char *hostname , struct node_file *new_node_file);

// Static data
static struct file_data * file_head;
static struct file_data * file_tail;

static struct node * node_head;
static struct node * node_tail;
static struct node * last_updated_node;

static int num_nodes;


/* Function:    node_lookup_new_1_svc
 *
 * Description: This function handles a call from the user interface client.
 *              It returns two nodes that are eligible to host a new file.
 ****/
struct two_nodes * node_lookup_new_1_svc(char **argp, struct svc_req *rqstp)
{
    struct two_nodes * result = (struct two_nodes *)malloc(sizeof(struct two_nodes));
    result->hostname1 = (char *)malloc(MAXHOSTNAME);
    result->hostname2 = (char *)malloc(MAXHOSTNAME);

    struct node * cur_node = node_head;
    struct node_file * cur_node_file;

    int found = 1;

    while (cur_node != NULL) {
        found = 0;
        cur_node_file = cur_node->node_file_head;
        while (cur_node_file != NULL) {
            if (strcmp(cur_node_file->filename, *argp) == 0) {
                found = 1;
                break;
            }
            cur_node_file = cur_node_file->next;
        }
        if (!found) {
            strcpy(result->hostname1, cur_node->hostname);
            break;
        }
        cur_node = cur_node->next;
    }

    // If found is set to true at this point, then all nodes must have this file.  Return.
    if (found == 1) {
        strcpy(result->hostname1, "");
        strcpy(result->hostname2, "");
        return(result);
    }

    // At this point, found must be set to false.  Iterate through the rest of the node list.
    found = 1;
    cur_node = cur_node->next;
    while (cur_node != NULL) {
        found = 0;
        cur_node_file = cur_node->node_file_head;
        while (cur_node_file != NULL) {
            if (strcmp(cur_node_file->filename, *argp) == 0) {
                found = 1;
                break;
            }
            cur_node_file = cur_node_file->next;
        }
        if (!found) {
            strcpy(result->hostname2, cur_node->hostname);
            break;
        }
        cur_node = cur_node->next;
    }

    // If found is set to true at this point, then all but one node must have this file.  Return.
    if (found == 1) {
        strcpy(result->hostname1, "");
        strcpy(result->hostname2, "");
    }
    return(result);
}


/* Function:    find_new_node
 *
 * Description: This function looks for a node that does not have.
 *              the specified file.  It is used to as part of the replicate_file
 *              implementation.
 ****/
struct node * find_new_node(char *filename)
{
    struct file_data * cur_file = file_head;
    struct node * cur_node = node_head;
    struct node_file * cur_node_file;
    char * new_node_host = (char *)malloc(MAXHOSTNAME);
    int found = 0;

    // Search the file list of every node for a filename match.
    while (cur_node != NULL) {
        found = 0;
        cur_node_file = cur_node->node_file_head;

        // As soon as a match is found, break out of the loop and
        // begin searching the next node.
        while (cur_node_file != NULL) {
            if (strcmp(cur_node_file->filename, filename) == 0) {
                found = 1;
                break;
            }
            cur_node_file = cur_node_file->next;
        }

        // If found_file is false, no match was found.  This node must not
        // have a copy of the specified file.
        if (!found) {
            return cur_node;
        }
        cur_node = cur_node->next;
    }

    // Every node has the file.  Return NULL.
    return NULL;
}


/* Function:  replicate_file
 *
 * Description: This function is used to replicate a file to a new node.
 ****/
void replicate_file(struct file_data * file_to_replicate, struct node *cur_node)
{
    struct node * new_node = (struct node *)malloc(sizeof(struct node));
    char * buffer;// = (char *)malloc(MAXFILELEN);
    struct file * new_file = (struct file *)malloc(sizeof(struct file));

    struct file_rqst_data * rqst_data = (struct file_rqst_data *)malloc(sizeof(struct file_rqst_data));
    rqst_data->filename = (char *)malloc(MAXNAMELEN);
    strcpy(rqst_data->filename, file_to_replicate->filename);
    rqst_data->chunk_size = file_to_replicate->file_size;
    rqst_data->file_position = 0;

    // First, copy file from node
    buffer = get_file_copy(cur_node->hostname, rqst_data);

    // Find a node that does not have this file
    new_node = find_new_node(file_to_replicate->filename);
    if (new_node == NULL) {
        fprintf(stderr, "All nodes have file %s", file_to_replicate->filename);
        return;
    }

    new_file->filename = (char *)malloc(MAXNAMELEN);
    new_file->file_contents = (char *)malloc(MAXFILELEN);

    strcpy(new_file->filename, file_to_replicate->filename);
    strcpy(new_file->file_contents, buffer);
    new_file->file_size = file_to_replicate->file_size;
    new_file->caller = 0;                                   // Caller is 0.  Do not register file.
    new_file->next = NULL;

    // Now, send the file to the new host.
    if (send_to_node(new_file, new_node->hostname) == 0) {
        fprintf(stderr, "Unable to replicate file %s", file_to_replicate->filename);
        return;
    }

    // Finally, ensure that the node file list is updated.
    file_to_replicate->num_nodes++;
    file_to_replicate->node_list_tail->next = new_node;
    file_to_replicate->node_list_tail = new_node;

    struct node_file * new_node_file = (struct node_file *)malloc(sizeof(struct node_file));
    new_node_file->filename = (char *)malloc(MAXNAMELEN);

    strcpy(new_node_file->filename, new_file->filename);
    new_node_file->file_size = new_file->file_size;
    new_node_file->next = NULL;

    if (new_node->node_file_head == NULL) {
        new_node->node_file_head = new_node_file;
        new_node->node_file_tail = new_node_file;
    }
    else {
        new_node->node_file_tail->next = new_node_file;
        new_node->node_file_tail = new_node_file;
    }

    // Clean up
    free(buffer);
    free(rqst_data);
}


/* Function:  node_lookup_query_1_svc
 *
 * Description: This function handles returns query information about
 *              the specified file.  It is called by the user interface client.
 ****/
struct query_info * node_lookup_query_1_svc(struct query_param *argp, struct svc_req *rqstp)
{
    struct query_info * query_result = (struct query_info *)malloc(sizeof(struct query_info));
    query_result->nodes = (struct node *)malloc(sizeof(struct node));
    struct file_data  * current_file = file_head;
    char * filename = (char *)malloc(MAXNAMELEN);
    strcpy(filename, argp->filename);

    struct node * node_list_head = (struct node *)malloc(sizeof(struct node));
    struct node * node_list_tail = (struct node *)malloc(sizeof(struct node));

    // Search file list for filename
    while(current_file != NULL) {
        if (strcmp(current_file->filename, filename) == 0) {
            query_result->file_size = current_file->file_size;
            query_result->num_nodes = current_file->num_nodes;
            query_result->nodes = current_file->node_list_head;

            // Copy node information
            if (argp->caller == 1) {
                struct node *cur_node = current_file->node_list_head;
                struct node *new_node;
                query_result->nodes = NULL;

                current_file->num_requests++;

                // Copy node information.
                while (cur_node != NULL) {
                    new_node = (struct node *)malloc(sizeof(struct node));
                    new_node->hostname = (char *)malloc(MAXHOSTNAME);
                    strcpy(new_node->hostname, cur_node->hostname);

                    new_node->node_file_head = NULL;
                    new_node->node_file_tail = NULL;
                    new_node->next = NULL;

                    if (query_result->nodes == NULL) {
                        query_result->nodes = new_node;
                        node_list_tail = new_node;
                    }
                    else {
                        node_list_tail->next = new_node;
                        node_list_tail = new_node;
                    }
                    cur_node = cur_node->next;
                }
            }

            // Request threshold reached - replicate file and reset the number of requests.
            if (current_file->num_requests == req_threshold) {
                printf("File request threshold reached.  Now replicating file %s\n", filename);
                replicate_file(current_file, current_file->node_list_head);
                current_file->num_requests = 0;
            }
            return(query_result);

        }
        current_file = current_file->next;
    }
    // Return result
    free(filename);
    return(query_result);
}


/* Function:  node_register_1_svc
 *
 * Description: This function registers a node hostname.
 *              It is called by the node upon node startup.
 ****/
int * node_register_1_svc(char **argp, struct svc_req *rqstp)
{
    static int retval = 1;
    struct node * new_node = (struct node *)malloc(sizeof(struct node));
    new_node->hostname = (char *)malloc(MAXHOSTNAME);

    if (strcpy(new_node->hostname, *argp) <= 0) {
        retval = 0;
        return(&retval);
    }
    new_node->node_file_head = NULL;
    new_node->node_file_tail = NULL;
    new_node->next = NULL;

    // Add new node to node list.
    if (node_head == NULL) {
        node_head = new_node;
        node_tail = new_node;
    }
    else {
        node_tail->next = new_node;
        node_tail = new_node;
    }

    num_nodes++;
    printf("Node registered.  Hostname:  %s  Node count:  %d\n", new_node->hostname, num_nodes);
    return(&retval);
}


/* Function:  file_register_1_svc
 *
 * Description: This function registers a new file.
 *              It is called by the node upon a new file add.
 ****/
int * file_register_1_svc(node *argp, struct svc_req *rqstp)
{
    static int retval = 1;

    // Create node data
    struct node * node_data = (struct node *)malloc(sizeof(struct node));
    node_data->hostname = (char *)malloc(MAXHOSTNAME);
    strcpy(node_data->hostname, argp->hostname);

    node_data->node_file_head = NULL;
    node_data->node_file_tail = NULL;
    node_data->next = NULL;

    struct node_file * new_node_file = (struct node_file *)malloc(sizeof(struct node_file));
    new_node_file->filename = (char *)malloc(MAXNAMELEN);

    strcpy(new_node_file->filename, argp->node_file_head->filename);
    new_node_file->file_size = argp->node_file_head->file_size;
    new_node_file->next = NULL;

    // Has this file already been registered?
    struct file_data * cur_file = file_head;

    while(cur_file != NULL) {
        if (strcmp(cur_file->filename, argp->node_file_head->filename) == 0) {
            cur_file->num_nodes++;
            cur_file->node_list_tail->next = node_data;
            cur_file->node_list_tail = node_data;
            update_node_file_list(node_data->hostname, new_node_file);
            return(&retval);
        }
        cur_file = cur_file->next;
    }

    // Create new tracker file data
    struct file_data * new_file = (struct file_data *)malloc(sizeof(struct file_data));
    new_file->filename = (char *)malloc(MAXNAMELEN);
    strcpy(new_file->filename, new_node_file->filename);

    new_file->file_size = new_node_file->file_size;
    new_file->num_requests = 0;
    new_file->num_nodes = 1;
    new_file->node_list_head = node_data;
    new_file->node_list_tail = node_data;
    new_file->next = NULL;

    if (file_head == NULL) {
        file_head = new_file;
        file_tail = new_file;
    }
    else {
        file_tail->next = new_file;
        file_tail = new_file;
    }

    // Now, update the file list for the individual node.
    update_node_file_list(node_data->hostname, new_node_file);

    printf("File registered.  Hostname:  %s  Node count:  %d\n", node_data->hostname, new_file->num_nodes);
    return(&retval);
}


void update_node_file_list(char *hostname , struct node_file *new_node_file)
{
    struct node * cur_node = node_head;
    while (cur_node != NULL) {
        if (strcmp(cur_node->hostname, hostname) == 0) {
            if (cur_node->node_file_head == NULL) {
                cur_node->node_file_head = new_node_file;
                cur_node->node_file_tail = new_node_file;
            }
            else {
                cur_node->node_file_tail->next = new_node_file;
                cur_node->node_file_tail = new_node_file;
            }
            break;
        }
        cur_node = cur_node->next;
    }
}
