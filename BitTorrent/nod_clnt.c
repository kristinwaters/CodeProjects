/*******************************************************************************
 * Filename: nod_clnt.c
 * Description:  This file was generated using rpcgen.
 *                
 * Author: Kristin Dahl
 * Date: 3-2-2015
 *****************************************************************************/

#include <memory.h> /* for memset */
#include "nod.h"

/* Default timeout can be changed using clnt_control() */
static struct timeval TIMEOUT = { 25, 0 };

int *
store_new_file_1(file *argp, CLIENT *clnt)
{
	static int clnt_res;

	memset((char *)&clnt_res, 0, sizeof(clnt_res));
	if (clnt_call (clnt, STORE_NEW_FILE,
		(xdrproc_t) xdr_file, (caddr_t) argp,
		(xdrproc_t) xdr_int, (caddr_t) &clnt_res,
		TIMEOUT) != RPC_SUCCESS) {
		return (NULL);
	}
	return (&clnt_res);
}

char **
transfer_file_1(file_rqst_data *argp, CLIENT *clnt)
{
	static char *clnt_res;

	memset((char *)&clnt_res, 0, sizeof(clnt_res));
	if (clnt_call (clnt, TRANSFER_FILE,
		(xdrproc_t) xdr_file_rqst_data, (caddr_t) argp,
		(xdrproc_t) xdr_wrapstring, (caddr_t) &clnt_res,
		TIMEOUT) != RPC_SUCCESS) {
		return (NULL);
	}
	return (&clnt_res);
}
