/*******************************************************************************
 * Filename: tstore_server.c
 * Description:  This file contains the necessary functions to run the torrent
 *               store server of a simplified BitTorrent protocol.
 *
 * Author: Kristin Dahl
 * Date: 3-2-2015
 *****************************************************************************/

#include "tstore.h"
#include "trkr.h"
#include <stdio.h>
#include <stdlib.h>
#include <fcntl.h>
#include <string.h>

// Static data
static char * tracker_host;
static char * retbuf;


/* Function:    locate_tracker_1_svc
 *
 * Description: This function returns the hostnames of the tracker.
 *              It is called by the client.
 ****/
char ** locate_tracker_1_svc(void *argp, struct svc_req *rqstp)
{
   return(&tracker_host);
}


/* Function:    create_torrent_1_svc
 *
 * Description: This function creates a new torrent file.
 *              It is called by the client.
 ****/
int * create_torrent_1_svc(char **argp, struct svc_req *rqstp)
{
    static retval = 0;
    char *new_filename = (char *)malloc(MAXNAMELEN + 5);
    strcpy(new_filename, "tor_");
    strcat(new_filename, *argp);

    // Open file
    FILE *file = fopen(new_filename, "w");
    if (!file)
    {
        fprintf(stderr, "Unable to open file %s\n", new_filename);
        return(&retval);
    }

    // Read buffer contents into file
    char *buffer = (char *)malloc(MAXNAMELEN + MAXHOSTNAME + 2);
    strcpy(buffer, tracker_host);
    strcat(buffer, "/");
    strcat(buffer, *argp);
    fprintf(file, buffer);

    // Clean up
    fclose(file);
    free(buffer);
    free(new_filename);
    retval = 1;
    return(&retval);
}


/* Function:    query_file_1_svc
 *
 * Description: This function returns the contents of a torrent file.
 *              It is called by the client.
 ****/
char ** query_file_1_svc(char **argp, struct svc_req *rqstp)
{
    // Open file
    free(retbuf);
    retbuf = (char *)malloc(MAXNAMELEN + MAXHOSTNAME + 2);
    strcpy(retbuf, "");

    FILE *file = fopen(*argp, "r");
    if (!file)
    {
        fprintf(stderr, "Unable to open file %s\n", *argp);
        return(&retbuf);
    }

    // Free the previous result and read file contents into buffer
    fgets(retbuf, MAXNAMELEN + MAXHOSTNAME + 2, file);

    // Clean up
    fclose(file);
    return(&retbuf);
}


/* Function:    tracker_register_1_svc
 *
 * Description: This function registers a tracker hostname.
 *              It is called by the tracker upon tracker startup.
 ****/
int * tracker_register_1_svc(char ** argp, struct svc_req * rqstp)
{
    static int retval = 1;

    tracker_host = (char *)malloc(sizeof(&argp));
    if (strcpy(tracker_host, *argp) <= 0) {
        retval = 0;
    }
    return(&retval);
}
