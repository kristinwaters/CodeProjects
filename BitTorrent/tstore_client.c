/*******************************************************************************
 * Filename: tstore_client.c
 * Description:  This file contains the necessary functions to run the torrent
 *               store client of a simplified BitTorrent protocol.
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


/* Function:    get_tracker
 *
 * Description: This function returns the hostname of the tracker node.
 *              It is called by the client.
 ****/
char * get_tracker(char *tstore_host) 
{
    // Create torrent store client
    CLIENT * tor_store_clnt = clnt_create(tstore_host, TORRENT_STORE_PROG, TORRENT_STORE_VERS, "udp");
    if (tor_store_clnt == NULL) {
        clnt_pcreateerror(tstore_host);
        return (char *)NULL;
    }

    // Get hostname of tracker
    void * argp;
    char * trkr_host = (char *)malloc(MAXHOSTNAME);
    trkr_host = *locate_tracker_1(&argp, tor_store_clnt);

    if (trkr_host == NULL) {
        clnt_perror(tor_store_clnt, "call failed:");
    }
    clnt_destroy(tor_store_clnt);
    return(trkr_host);
}   


/* Function:    create_torrent_file
 *
 * Description: This function creates a new torrent file.
 *              It is called by the client.
 ****/
int create_torrent_file(char *filename, char *tstore_host)
{
    // Create torrent store client
    CLIENT *tor_store_clnt = clnt_create(tstore_host, TORRENT_STORE_PROG, TORRENT_STORE_VERS, "udp");
    if (tor_store_clnt == NULL) {
        clnt_pcreateerror(tstore_host);
        return 0;
    }
    
    int *result = create_torrent_1(&filename, tor_store_clnt);
    if (*result == 0) {
        clnt_perror(tor_store_clnt, "call failed:");
        return 0;
    }
    clnt_destroy(tor_store_clnt); 
    return 1;
}


/* Function:    get_torrent_file
 *
 * Description: This function contacts the torrent store server and 
 *              requests a torrent file.  It is called by the client.
 ****/
char * get_torrent_file(char *filename, char *tstore_host) 
{
    // Create torrent store client
    CLIENT * tor_store_clnt = clnt_create(tstore_host, TORRENT_STORE_PROG, TORRENT_STORE_VERS, "udp");
    if (tor_store_clnt == NULL) {
        clnt_pcreateerror(tstore_host);
        return "";
    }

   // Contact the torrent store for the torrent file
    char * tor_filename = (char *)malloc(MAXNAMELEN + 5);
    strcpy(tor_filename, "tor_");
    strcat(tor_filename, filename);
 
    char * torrent = (char *)malloc(MAXNAMELEN + MAXHOSTNAME + 2);
    torrent = *query_file_1(&tor_filename, tor_store_clnt);
    if (strcmp(torrent, "") == 0)  {
        clnt_perror(tor_store_clnt, "call failed:");
        return torrent;
    }
    
    // Clean up
    clnt_destroy(tor_store_clnt);
    free(tor_filename);
    return torrent;
}
