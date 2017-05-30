/*******************************************************************************
 * Filename: tstore_svc.c
 * Description:  This file was generated using rpcgen.
 *                
 * Author: Kristin Dahl
 * Date: 3-2-2015
 *****************************************************************************/

#include "tstore.h"
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

static void
torrent_store_prog_1(struct svc_req *rqstp, register SVCXPRT *transp)
{
	union {
		char *create_torrent_1_arg;
		char *query_file_1_arg;
		char *tracker_register_1_arg;
	} argument;
	char *result;
	xdrproc_t _xdr_argument, _xdr_result;
	char *(*local)(char *, struct svc_req *);

	switch (rqstp->rq_proc) {
	case NULLPROC:
		(void) svc_sendreply (transp, (xdrproc_t) xdr_void, (char *)NULL);
		return;

	case LOCATE_TRACKER:
		_xdr_argument = (xdrproc_t) xdr_void;
		_xdr_result = (xdrproc_t) xdr_wrapstring;
		local = (char *(*)(char *, struct svc_req *)) locate_tracker_1_svc;
		break;

	case CREATE_TORRENT:
		_xdr_argument = (xdrproc_t) xdr_wrapstring;
		_xdr_result = (xdrproc_t) xdr_int;
		local = (char *(*)(char *, struct svc_req *)) create_torrent_1_svc;
		break;

	case QUERY_FILE:
		_xdr_argument = (xdrproc_t) xdr_wrapstring;
		_xdr_result = (xdrproc_t) xdr_wrapstring;
		local = (char *(*)(char *, struct svc_req *)) query_file_1_svc;
		break;

	case TRACKER_REGISTER:
		_xdr_argument = (xdrproc_t) xdr_wrapstring;
		_xdr_result = (xdrproc_t) xdr_int;
		local = (char *(*)(char *, struct svc_req *)) tracker_register_1_svc;
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

/* Function:  main
 *
 ****/
main(int argc, char *argv[])
{
    printf("Now running torrent store.\n");
    
	register SVCXPRT *transp;

	pmap_unset (TORRENT_STORE_PROG, TORRENT_STORE_VERS);

	transp = svcudp_create(RPC_ANYSOCK);
	if (transp == NULL) {
		fprintf (stderr, "%s", "cannot create udp service.");
		exit(1);
	}
	if (!svc_register(transp, TORRENT_STORE_PROG, TORRENT_STORE_VERS, torrent_store_prog_1, IPPROTO_UDP)) {
		fprintf (stderr, "%s", "unable to register (TORRENT_STORE_PROG, TORRENT_STORE_VERS, udp).");
		exit(1);
	}

	transp = svctcp_create(RPC_ANYSOCK, 0, 0);
	if (transp == NULL) {
		fprintf (stderr, "%s", "cannot create tcp service.");
		exit(1);
	}
	if (!svc_register(transp, TORRENT_STORE_PROG, TORRENT_STORE_VERS, torrent_store_prog_1, IPPROTO_TCP)) {
		fprintf (stderr, "%s", "unable to register (TORRENT_STORE_PROG, TORRENT_STORE_VERS, tcp).");
		exit(1);
	}

	svc_run ();
	fprintf (stderr, "%s", "svc_run returned");
	exit (1);
	/* NOTREACHED */
}
