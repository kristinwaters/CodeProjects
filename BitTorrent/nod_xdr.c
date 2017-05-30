/*******************************************************************************
 * Filename: nod_xdr.c
 * Description:  This file was generated using rpcgen.
 *                
 * Author: Kristin Dahl
 * Date: 3-2-2015
 *****************************************************************************/

#include "nod.h"

bool_t
xdr_file_rqst_data (XDR *xdrs, file_rqst_data *objp)
{
	register int32_t *buf;

	 if (!xdr_string (xdrs, &objp->filename, MAXNAMELEN))
		 return FALSE;
	 if (!xdr_int (xdrs, &objp->file_position))
		 return FALSE;
	 if (!xdr_int (xdrs, &objp->chunk_size))
		 return FALSE;
	return TRUE;
}

bool_t
xdr_file (XDR *xdrs, file *objp)
{
	register int32_t *buf;

	 if (!xdr_string (xdrs, &objp->filename, MAXNAMELEN))
		 return FALSE;
	 if (!xdr_string (xdrs, &objp->file_contents, MAXFILELEN))
		 return FALSE;
	 if (!xdr_int (xdrs, &objp->caller))
		 return FALSE;
	 if (!xdr_int (xdrs, &objp->file_size))
		 return FALSE;
	 if (!xdr_pointer (xdrs, (char **)&objp->next, sizeof (file), (xdrproc_t) xdr_file))
		 return FALSE;
	return TRUE;
}
