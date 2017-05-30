/*******************************************************************************
 * Filename: trkr_client.c
 * Description:  This file contains the necessary functions to run the tracker
 *               client of a simplified BitTorrent protocol.
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


/* Function:    get_new_nodes
 *
 * Description: This function returns the hostnames of two nodes that can host
 *              a new file.  It is called by the client.
 ****/
struct two_nodes * get_new_nodes(char *filename, char *tracker_host)
{
    // Create tracker client
    CLIENT * tracker_clnt = clnt_create(tracker_host, TRACKER_PROG, TRACKER_VERS, "udp");
    if (tracker_clnt == NULL) {
        clnt_pcreateerror(tracker_host);
        return (struct two_nodes *)NULL;
    }

    struct two_nodes * nodes = (struct two_nodes *)malloc(sizeof(struct two_nodes));
    nodes = node_lookup_new_1(&filename, tracker_clnt);

    if (nodes == (two_nodes *) NULL) {
        clnt_perror(tracker_clnt, "call failed:");
        return;
    }
    clnt_destroy(tracker_clnt);
    return nodes;
}


/* Function:    send_query
 *
 * Description: This function returns the hostnames of nodes that have
 *              the specified file
 ****/
struct query_info * send_query(struct query_param *q_param, char *tracker_host)
{
    // Create tracker client
    CLIENT * tracker_clnt = clnt_create(tracker_host, TRACKER_PROG, TRACKER_VERS, "udp");
    if (tracker_clnt == NULL) {
        clnt_pcreateerror(tracker_host);
        return NULL;
    }

    struct query_info * query_result = (struct query_info *)malloc(sizeof(struct query_info));
    query_result = node_lookup_query_1(q_param, tracker_clnt);

    if (query_result == (struct query_info *)NULL) {
        clnt_perror(tracker_clnt, "call failed:");

        return (struct query_info *)NULL;
    }
    clnt_destroy(tracker_clnt);
    return query_result;
}
