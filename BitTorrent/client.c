/*******************************************************************************
 * Filename: client.c
 * Description:  This file contains the necessary functions to run the client
 *               component of a simplified BitTorrent protocol.
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
char * get_tracker(char * tor_store_host);
char * get_file_chunk(struct file_rqst_data * rqst_data, char * node_hostname);
int    send_to_node(struct file * file_data, char * node_hostname);
int    create_torrent_file(char * filename, char * tor_store_host);
struct two_nodes * get_new_nodes(char *tracker_host, char *filename);
struct torrent_file * get_torrent_file(char * filename, char * tor_store_host);
struct query_info * send_query(query_param * q_param, char * tracker_host);
 
static char *tstore_host;

/* Function:  send_file
 *
 * Description:  This function prepares to send a file to a new node host.
 ****/
int send_file(FILE *fp, char *filename, char *node_host)
{
    // Prepare file data
    struct file * file_to_send = (struct file *)malloc(sizeof(struct file));
    file_to_send->filename = (char *)malloc(sizeof(&filename));
    strcpy(file_to_send->filename, filename);

    // Open file
    int ret_val = 0;
    fp = fopen(filename, "rb");
    if (!fp)
    {
        fprintf(stderr, "Unable to open file %s", filename);
        return(ret_val);
    }

    // Allocate memory
    file_to_send->file_contents = (char *)malloc(MAXFILELEN+1);
    if (!file_to_send->file_contents)
    {
        fprintf(stderr, "Memory error!");
        fclose(fp);
        return(ret_val);
    }

    // Read file contents into buffer
    fread(file_to_send->file_contents, sizeof(char), MAXFILELEN, fp);
    fclose(fp);

    file_to_send->file_size = strlen(file_to_send->file_contents);
    file_to_send->next = NULL;
    file_to_send->caller = 1;

    // Send the file
    ret_val = send_to_node(file_to_send, node_host);

    // Clean up
    free(file_to_send->filename);
    free(file_to_send->file_contents);
    return(ret_val);
}


/* Function:  add_file
 *
 * Description:  This function executes the the "Add File" command.  First, it
 *               retrieves two node hostnames.  Then, it sends the file to each
 *               of the nodes.  Last, it contacts the torrent store in order to
 *               create a new torrent file.
 ****/
int add_file(char *filename, FILE *fp)
{
    // Contact the tracker for the identies of 2 nodes that can store the file.
    char *trkr_host = get_tracker(tstore_host);

    struct two_nodes * nodes = (struct two_nodes *)malloc(sizeof(struct two_nodes));
    nodes->hostname1 = (char *)malloc(MAXHOSTNAME);
    nodes->hostname2 = (char *)malloc(MAXHOSTNAME);
    nodes = (struct two_nodes *)get_new_nodes(filename, trkr_host);

    if (strcmp(nodes->hostname1, "") == 0) {
        return 0;
    }
    printf("Node hostnames received: %s  %s\n", nodes->hostname1, nodes->hostname2);

    // Send the file to each of the nodes returned by the tracker.
    int ret_val1 = send_file(fp, filename, nodes->hostname1);
    int ret_val2 = send_file(fp, filename, nodes->hostname2);

    // Lastly, call the torrent store to create a new torrent file.
    create_torrent_file(filename, tstore_host);

    // Clean up
    free(nodes->hostname1);
    free(nodes->hostname2);

    if ((ret_val1 == 0) || (ret_val2 == 0)) {
        return 0;
    }
    else {
        return 1;
    }
}


/* Function:  query_file
 *
 ****/
struct query_info * query_file(struct query_param *q_param)
{
    char *torrent = (char *)malloc(MAXHOSTNAME + MAXNAMELEN + 2);
    struct query_info *q_result = (struct query_info *)malloc(sizeof(struct query_info));

    // Contact the torrent store to retrieve the torrent file.
    torrent = (char *)get_torrent_file(q_param->filename, tstore_host);
    if (strcmp(torrent, "") == 0) {
        free(torrent);
        q_result->num_nodes = 0;
        return(q_result);
    }

    char *trkr_host = (char *)malloc(sizeof(MAXHOSTNAME));
    strcpy(trkr_host, strtok(torrent, "/"));

    //Contact the tracker for the list of nodes that have this file.
    q_result = (struct query_info *)send_query(q_param, trkr_host);
    if (q_result == (query_info *) NULL) {
       q_result->num_nodes = 0;
    }

    // Clean up
    free(torrent);
    free(trkr_host);
    return q_result;
}


/* Function:  download_file
 *
 * Description:  This function executes the the "Download File" command.  Based on the
 *               data contained in q_result, it determines what chunk size of data it
 *               needs to download from each node.  It requests this chunk size from
 *               each host node and saves the returned data into a file.
 ****/
int download_file(char *filename, struct query_info *q_result)
{
    int i;
    FILE *file;
    char * buffer;
    char * download_dir = "downloads/";

    // Create parameters for get_file_chunk
    struct node * cur_node = (struct node *)malloc(sizeof(struct node));
    cur_node = q_result->nodes;
    int file_size = q_result->file_size;
    int num_nodes = q_result->num_nodes;
    int remainder = file_size % num_nodes;

    struct file_rqst_data * info = (struct file_rqst_data *)malloc(sizeof(struct file_rqst_data));
    info->filename = (char *)malloc(MAXNAMELEN);
    strcpy(info->filename, filename);
    info->chunk_size = file_size/num_nodes;

    // Change to download directory
    chdir(download_dir);
    file = fopen(filename, "w");
    if (!file)
    {
        fprintf(stderr, "Unable to open file %s", filename);
        return 0;
    }

    // Download chunks of file.
   for (i = 0; i < num_nodes; i++) {
        info->file_position = i;

        // If this is the last piece to download, be sure to get any remaining bytes.
        if (i == (num_nodes - 1)) {
            info->chunk_size = info->chunk_size + remainder;
        }

        printf("Getting data from hostname %s.  Chunk size is: %d bytes\n", cur_node->hostname, info->chunk_size);
        buffer = get_file_chunk(info, cur_node->hostname);
        fwrite(buffer, sizeof(char), info->chunk_size, file);
        cur_node = cur_node->next;
    }

    // Clean up
    fclose(file);
    free(info);
    free(buffer);
    chdir("../");
    return 1;
}


/* Function:  main
 *
 ****/
main(int argc, char *argv[])
{
    if(argc < 2) {
        printf("usage: %s torrent_store_host\n", argv[0]);
        exit(1);
    }
    tstore_host = (char *)malloc(sizeof(argv[1]));
    strcpy(tstore_host, argv[1]);

    int usr_cmd;
    int ret_val;
    char * filename = (char *)malloc(MAXNAMELEN);
    struct query_info  * q_result = (struct query_info *)malloc(sizeof(struct query_info));
    struct query_param * q_param  = (struct query_param *)malloc(sizeof(struct query_param));
    q_param->filename = (char *)malloc(MAXNAMELEN);

    printf("\nWelcome to the BitTorrent client.\n");
    while(1) {
        printf("\n--------- Menu ---------\n");
        printf("Add File      - Press 1\n");
        printf("Query File    - Press 2\n");
        printf("Download File - Press 3\n");
        printf("Quit          - Press 4\n");
        printf("------------------------\n");
        printf("\nSelect a command: ");
        scanf("%d", &usr_cmd);

        if (usr_cmd == 1) {
            printf("\n\n*****  Add File  *****\n\n");
            printf("Please enter the filename:  ");
            scanf("%s", filename);

            // Check to see if this is a valid file
            FILE *fp = fopen(filename, "r");
            if (fp == NULL) {
                printf("\nInvalid file.\n\n", filename);
            }
            else {
                ret_val = add_file(filename, fp);
                fclose(fp);

                if (ret_val == 0) {
                    printf("\nUnable to add file.\n\n");
                }
                else {
                    printf("\nFile add complete.\n\n");
                }
            }
        }
        else if (usr_cmd == 2) {
            printf("\n\n*****  Query File  *****\n\n");
            printf("Please enter the filename:  ");
            scanf("%s", q_param->filename);
            q_param->caller = 0;
            q_result = query_file(q_param);

	        printf("\n\n-- Query Results --\n");
            if (q_result->num_nodes == 0) {
                printf("\nNo files found.\n\n");
                continue;
            }
            else {
                printf("File name: %s\n", filename);
                printf("File size: %i bytes\n", q_result->file_size);
                printf("Number of host nodes: %i\n\n", q_result->num_nodes);
            }
        }
        else if (usr_cmd == 3) {
            printf("\n\n*****  Download File  *****\n\n");
            printf("Please enter the filename:  ");
            scanf("%s", q_param->filename);
            q_param->caller = 1;
            q_result = query_file(q_param);

            if (q_result->num_nodes == 0) {
                printf("\nFile is unavailable.\n");
            }
            else {
                ret_val = download_file(filename, q_result);
                if (ret_val == 0) {
                    printf("\nUnable to download file.\n\n");
                }
                else {
                    printf("\nFile download complete.\n\n");
                }
            }
        }
        else if (usr_cmd == 4){
            printf("\nNow exiting - bye!\n\n");
            break;
        }
        else {
            printf("\nInvalid command.\n\n");
        }
    }
}
