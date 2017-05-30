/*******************************************************************************
 * Filename: nod_server.c
 * Description:  This file contains the necessary functions to run the node
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

static char * file_buffer;

/* Function:    file_reg
 *
 * Description: This function is called whenever a new file is added to a node.
 *              Its purpose is to register each new file with the tracker.
 ****/
int file_reg(char *filename, int file_size)
{
    // Create node file
    struct node_file * data = (struct node_file *)malloc(sizeof(struct node_file));
    data->filename = (char *)malloc(MAXNAMELEN);
    strcpy(data->filename, filename);
    data->file_size = file_size;
    data->next = NULL;

    // Create node data
    struct node * node_data = (struct node *)malloc(sizeof(struct node));
    node_data->hostname = (char *)malloc(MAXHOSTNAME);
    gethostname(node_data->hostname, MAXHOSTNAME);
    node_data->node_file_head = data;
    node_data->next = NULL;

    // Create tracker client
    CLIENT *tracker_clnt = clnt_create(tracker_host, TRACKER_PROG, TRACKER_VERS, "udp");
    if (tracker_clnt == NULL) {
        clnt_pcreateerror(tracker_host);
        return 0;
    }

    int * result = file_register_1(node_data, tracker_clnt);
    if (*result == 0) {
        clnt_perror(tracker_clnt, "call failed:");
        return 0;
    }

    // Clean up
    clnt_destroy(tracker_clnt);
    free(node_data);
    free(data);
    return 1;
}


/* Function:    store_new_file_1_svc
 *
 * Description: This function is called by the client (and the tracker)
 *              to store a new file.
 ****/
int * store_new_file_1_svc(file *argp, struct svc_req *rqstp)
{
    static int retval = 1;
    char * filename = (char *)malloc(MAXNAMELEN);
    char * buffer   = (char *)malloc(MAXFILELEN);
    struct file * new_file = (struct file *)malloc(sizeof(struct file));

    new_file = argp;
    int file_size = new_file->file_size;
    strcpy(filename, new_file->filename);
    strcpy(buffer, new_file->file_contents);

    //Open file
    FILE * file = fopen(filename, "wb");
    if (!file)
    {
        fprintf(stderr, "Unable to open file %s", filename);
        return;
    }

    // Write file
    fwrite(buffer, sizeof(char), file_size, file);
    //fprintf(file, buffer);

    // Register new file with tracker only if this was a client request.
    if (new_file->caller == 1) {
        file_reg(filename, file_size);
    }

    // Clean up
    fclose(file);
    free(filename);
    free(buffer);
    return(&retval);
}


/* Function:    transfer_file_1_svc
 *
 * Description: This function is called by the client to transfer a file.
 ****/
char ** transfer_file_1_svc(file_rqst_data *argp, struct svc_req *rqstp)
{
    FILE *file;
    int chunk_size = argp->chunk_size;
    free(file_buffer);
    file_buffer = (char *)malloc(chunk_size + 1);

    //Open file
    file = fopen(argp->filename, "rt");
    if (!file)
    {
        fprintf(stderr, "Unable to open file %s", argp->filename);
        return;
    }

    // Set file position
    long file_pos = chunk_size * argp->file_position;
    if (file_pos > 1) {
        file_pos--;
        fseek(file, file_pos, SEEK_SET);
    }

    //Read file contents into buffer
    size_t i = fread(file_buffer, sizeof(char), chunk_size, file);
    file_buffer[chunk_size + 1] = '\0';

    // Clean up
    fclose(file);
    return(&file_buffer);
}
