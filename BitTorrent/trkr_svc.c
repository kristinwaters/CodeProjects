/*******************************************************************************
 *
 * Filename: trkr_svc.c
 * Description:  This file was generated using rpcgen.
 *                
 * Author: Kristin Dahl
 * Date: 3-2-2015
 *****************************************************************************/

#include "trkr.h"
#include "tstore.h"
#include <stdio.h>
#include <stdlib.h>
#include <rpc/pmap_clnt.h>
#include <string.h>
#include <memory.h>
#include <sys/socket.h>
#include <netinet/in.h>

#ifndef SIG_PF
#define SIG_PF void(*)(int)
#endif

int req_threshold;

static void
tracker_prog_1(struct svc_req *rqstp, register SVCXPRT *transp)
{
	union {
		char *node_lookup_new_1_arg;
		query_param node_lookup_query_1_arg;
		char *node_register_1_arg;
		node file_register_1_arg;
	} argument;
	char *result;
	xdrproc_t _xdr_argument, _xdr_result;
	char *(*local)(char *, struct svc_req *);

	switch (rqstp->rq_proc) {
	case NULLPROC:
		(void) svc_sendreply (transp, (xdrproc_t) xdr_void, (char *)NULL);
		return;

	case NODE_LOOKUP_NEW:
		_xdr_argument = (xdrproc_t) xdr_wrapstring;
		_xdr_result = (xdrproc_t) xdr_two_nodes;
		local = (char *(*)(char *, struct svc_req *)) node_lookup_new_1_svc;
		break;

	case NODE_LOOKUP_QUERY:
		_xdr_argument = (xdrproc_t) xdr_query_param;
		_xdr_result = (xdrproc_t) xdr_query_info;
		local = (char *(*)(char *, struct svc_req *)) node_lookup_query_1_svc;
		break;

	case NODE_REGISTER:
		_xdr_argument = (xdrproc_t) xdr_wrapstring;
		_xdr_result = (xdrproc_t) xdr_int;
		local = (char *(*)(char *, struct svc_req *)) node_register_1_svc;
		break;

	case FILE_REGISTER:
		_xdr_argument = (xdrproc_t) xdr_node;
		_xdr_result = (xdrproc_t) xdr_int;
		local = (char *(*)(char *, struct svc_req *)) file_register_1_svc;
		break;

	default:
		svcerr_noproc (transp);
		return;
	}
	memset ((char *)&argument, 0, sizeof (argument));
	if (!svc_getargs (transp, (xdrproc_t) _xdr_argument, (caddr_t) &argument)) {
		svcerr_decode (transp);
		return;
	}
	result = (*local)((char *)&argument, rqstp);
	if (result != NULL && !svc_sendreply(transp, (xdrproc_t) _xdr_result, result)) {
		svcerr_systemerr (transp);
	}
	if (!svc_freeargs (transp, (xdrproc_t) _xdr_argument, (caddr_t) &argument)) {
		fprintf (stderr, "%s", "unable to free arguments");
		exit (1);
	}
	return;
}


// This function is called by the tracker to register with the torrent store.
int tracker_reg(char *tstore_host) {
   char *tracker_host = (char *)malloc(MAXHOSTNAME);
   gethostname(tracker_host, MAXHOSTNAME);
   
   // Create torrent store client
   CLIENT *tor_store_clnt = clnt_create(tstore_host, TORRENT_STORE_PROG, TORRENT_STORE_VERS, "udp");
   if (tor_store_clnt == NULL) {
      clnt_pcreateerror(tstore_host);
      return 0;
   }
   
   int * result = tracker_register_1(&tracker_host, tor_store_clnt);
   if (result == 0) {
      clnt_perror(tor_store_clnt, "call failed:");
	  return 0;
   }
   clnt_destroy(tor_store_clnt);
   return 1;
}


/* Function:  main
 *
 ****/
main(int argc, char *argv[])
{
    if(argc < 3) {
        printf("usage: %s torrent_store_host  request_threshold\n", argv[0]);
        exit(1);
    }
    
    char * tstore_host = (char *)malloc(sizeof(argv[1]));
    strcpy(tstore_host, argv[1]);
    req_threshold = atoi(argv[2]);
   
    if (tracker_reg(tstore_host) == 0) {
        printf("Unable to register tracker with torrent store.\n");
        exit(1);
    }
    
    printf("Tracker successfully registered with torrent store.  Now running.\n");
    
	register SVCXPRT *transp;

	pmap_unset (TRACKER_PROG, TRACKER_VERS);

	transp = svcudp_create(RPC_ANYSOCK);
	if (transp == NULL) {
		fprintf (stderr, "%s", "cannot create udp service.");
		exit(1);
	}
	if (!svc_register(transp, TRACKER_PROG, TRACKER_VERS, tracker_prog_1, IPPROTO_UDP)) {
		fprintf (stderr, "%s", "unable to register (TRACKER_PROG, TRACKER_VERS, udp).");
		exit(1);
	}

	transp = svctcp_create(RPC_ANYSOCK, 0, 0);
	if (transp == NULL) {
		fprintf (stderr, "%s", "cannot create tcp service.");
		exit(1);
	}
	if (!svc_register(transp, TRACKER_PROG, TRACKER_VERS, tracker_prog_1, IPPROTO_TCP)) {
		fprintf (stderr, "%s", "unable to register (TRACKER_PROG, TRACKER_VERS, tcp).");
		exit(1);
	}

	svc_run ();
	fprintf (stderr, "%s", "svc_run returned");
	exit (1);
	/* NOTREACHED */
}
