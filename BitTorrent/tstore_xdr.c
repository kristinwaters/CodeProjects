/*******************************************************************************
 * Filename: tstore_xdr.c
 * Description:  This file was generated using rpcgen.
 *                
 * Author: Kristin Dahl
 * Date: 3-2-2015
 *****************************************************************************/

#include "tstore.h"

bool_t
xdr_torrent_file (XDR *xdrs, torrent_file *objp)
{
	register int32_t *buf;

	 if (!xdr_string (xdrs, &objp->filename, MAXNAMELEN))
		 return FALSE;
	 if (!xdr_pointer (xdrs, (char **)&objp->next, sizeof (torrent_file), (xdrproc_t) xdr_torrent_file))
		 return FALSE;
	return TRUE;
}
