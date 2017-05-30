/*******************************************************************************
 * Filename: nod_svc.c
 * Description:  This file was generated using rpcgen.
 *                
 * Author: Kristin Dahl
 * Date: 3-2-2015
 *****************************************************************************/

#include "nod.h"
#include "trkr.h"
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


char * tracker_host;

static void
node_prog_1(struct svc_req *rqstp, register SVCXPRT *transp)
{
	union {
		file store_new_file_1_arg;
		file_rqst_data transfer_file_1_arg;
	} argument;
	char *result;
	xdrproc_t _xdr_argument, _xdr_result;
	char *(*local)(char *, struct svc_req *);

	switch (rqstp->rq_proc) {
	case NULLPROC:
		(void) svc_sendreply (transp, (xdrproc_t) xdr_void, (char *)NULL);
		return;

	case STORE_NEW_FILE:
		_xdr_argument = (xdrproc_t) xdr_file;
		_xdr_result = (xdrproc_t) xdr_int;
		local = (char *(*)(char *, struct svc_req *)) store_new_file_1_svc;
		break;

	case TRANSFER_FILE:
		_xdr_argument = (xdrproc_t) xdr_file_rqst_data;
		_xdr_result = (xdrproc_t) xdr_wrapstring;
		local = (char *(*)(char *, struct svc_req *)) transfer_file_1_svc;
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



// This function is called by a node to register with the tracker
int node_reg() 
{
   char *node_host = (char *)malloc(MAXHOSTNAME);
   gethostname(node_host, MAXHOSTNAME);
   
   // Create tracker client
   CLIENT *tracker_clnt = clnt_create(tracker_host, TRACKER_PROG, TRACKER_VERS, "udp");
   if (tracker_clnt == NULL) {
      clnt_pcreateerror(tracker_host);
      return 0;
   }
   
   int * result = node_register_1(&node_host, tracker_clnt);
   if (result == 0) {
      clnt_perror(tracker_clnt, "call failed:");
	  return 0;
   }
   clnt_destroy(tracker_clnt);
   return 1;
}


/* Function:  main
 *
 ****/
main(int argc, char *argv[])
{
   if(argc < 2) {
      printf("usage: %s tracker_host\n", argv[0]);
      exit(1);
   }
   tracker_host = (char *)malloc(sizeof(argv[1]));
   strcpy(tracker_host, argv[1]);
   
   if (node_reg() == 0) {
      printf("Unable to register node with tracker.\n");
      exit(1);
   }
   
    printf("Node successfully registered with tracker.  Now running.\n");
       
	register SVCXPRT *transp;

	pmap_unset (NODE_PROG, NODE_VERS);

	transp = svcudp_create(RPC_ANYSOCK);
	if (transp == NULL) {
		fprintf (stderr, "%s", "cannot create udp service.");
		exit(1);
	}
	if (!svc_register(transp, NODE_PROG, NODE_VERS, node_prog_1, IPPROTO_UDP)) {
		fprintf (stderr, "%s", "unable to register (NODE_PROG, NODE_VERS, udp).");
		exit(1);
	}

	transp = svctcp_create(RPC_ANYSOCK, 0, 0);
	if (transp == NULL) {
		fprintf (stderr, "%s", "cannot create tcp service.");
		exit(1);
	}
	if (!svc_register(transp, NODE_PROG, NODE_VERS, node_prog_1, IPPROTO_TCP)) {
		fprintf (stderr, "%s", "unable to register (NODE_PROG, NODE_VERS, tcp).");
		exit(1);
	}

	svc_run ();
	fprintf (stderr, "%s", "svc_run returned");
	exit (1);
	/* NOTREACHED */
}
