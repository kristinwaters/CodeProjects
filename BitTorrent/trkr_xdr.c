/*******************************************************************************
 * Filename: trkr_xdr.c
 * Description:  This file was generated using rpcgen.
 *                
 * Author: Kristin Dahl
 * Date: 3-2-2015
 *****************************************************************************/

#include "trkr.h"

bool_t
xdr_file_data (XDR *xdrs, file_data *objp)
{
	register int32_t *buf;


	if (xdrs->x_op == XDR_ENCODE) {
		 if (!xdr_string (xdrs, &objp->filename, MAXNAMELEN))
			 return FALSE;
		buf = XDR_INLINE (xdrs, 3 * BYTES_PER_XDR_UNIT);
		if (buf == NULL) {
			 if (!xdr_int (xdrs, &objp->file_size))
				 return FALSE;
			 if (!xdr_int (xdrs, &objp->num_requests))
				 return FALSE;
			 if (!xdr_int (xdrs, &objp->num_nodes))
				 return FALSE;

		} else {
		IXDR_PUT_LONG(buf, objp->file_size);
		IXDR_PUT_LONG(buf, objp->num_requests);
		IXDR_PUT_LONG(buf, objp->num_nodes);
		}
		 if (!xdr_pointer (xdrs, (char **)&objp->node_list_head, sizeof (struct node), (xdrproc_t) xdr_node))
			 return FALSE;
		 if (!xdr_pointer (xdrs, (char **)&objp->node_list_tail, sizeof (struct node), (xdrproc_t) xdr_node))
			 return FALSE;
		 if (!xdr_pointer (xdrs, (char **)&objp->next, sizeof (file_data), (xdrproc_t) xdr_file_data))
			 return FALSE;
		return TRUE;
	} else if (xdrs->x_op == XDR_DECODE) {
		 if (!xdr_string (xdrs, &objp->filename, MAXNAMELEN))
			 return FALSE;
		buf = XDR_INLINE (xdrs, 3 * BYTES_PER_XDR_UNIT);
		if (buf == NULL) {
			 if (!xdr_int (xdrs, &objp->file_size))
				 return FALSE;
			 if (!xdr_int (xdrs, &objp->num_requests))
				 return FALSE;
			 if (!xdr_int (xdrs, &objp->num_nodes))
				 return FALSE;

		} else {
		objp->file_size = IXDR_GET_LONG(buf);
		objp->num_requests = IXDR_GET_LONG(buf);
		objp->num_nodes = IXDR_GET_LONG(buf);
		}
		 if (!xdr_pointer (xdrs, (char **)&objp->node_list_head, sizeof (struct node), (xdrproc_t) xdr_node))
			 return FALSE;
		 if (!xdr_pointer (xdrs, (char **)&objp->node_list_tail, sizeof (struct node), (xdrproc_t) xdr_node))
			 return FALSE;
		 if (!xdr_pointer (xdrs, (char **)&objp->next, sizeof (file_data), (xdrproc_t) xdr_file_data))
			 return FALSE;
	 return TRUE;
	}

	 if (!xdr_string (xdrs, &objp->filename, MAXNAMELEN))
		 return FALSE;
	 if (!xdr_int (xdrs, &objp->file_size))
		 return FALSE;
	 if (!xdr_int (xdrs, &objp->num_requests))
		 return FALSE;
	 if (!xdr_int (xdrs, &objp->num_nodes))
		 return FALSE;
	 if (!xdr_pointer (xdrs, (char **)&objp->node_list_head, sizeof (struct node), (xdrproc_t) xdr_node))
		 return FALSE;
	 if (!xdr_pointer (xdrs, (char **)&objp->node_list_tail, sizeof (struct node), (xdrproc_t) xdr_node))
		 return FALSE;
	 if (!xdr_pointer (xdrs, (char **)&objp->next, sizeof (file_data), (xdrproc_t) xdr_file_data))
		 return FALSE;
	return TRUE;
}

bool_t
xdr_two_nodes (XDR *xdrs, two_nodes *objp)
{
	register int32_t *buf;

	 if (!xdr_string (xdrs, &objp->hostname1, MAXHOSTNAME))
		 return FALSE;
	 if (!xdr_string (xdrs, &objp->hostname2, MAXHOSTNAME))
		 return FALSE;
	return TRUE;
}

bool_t
xdr_node_file (XDR *xdrs, node_file *objp)
{
	register int32_t *buf;

	 if (!xdr_string (xdrs, &objp->filename, MAXNAMELEN))
		 return FALSE;
	 if (!xdr_int (xdrs, &objp->file_size))
		 return FALSE;
	 if (!xdr_pointer (xdrs, (char **)&objp->next, sizeof (node_file), (xdrproc_t) xdr_node_file))
		 return FALSE;
	return TRUE;
}

bool_t
xdr_node (XDR *xdrs, node *objp)
{
	register int32_t *buf;

	 if (!xdr_string (xdrs, &objp->hostname, MAXHOSTNAME))
		 return FALSE;
	 if (!xdr_pointer (xdrs, (char **)&objp->node_file_head, sizeof (node_file), (xdrproc_t) xdr_node_file))
		 return FALSE;
	 if (!xdr_pointer (xdrs, (char **)&objp->node_file_tail, sizeof (node_file), (xdrproc_t) xdr_node_file))
		 return FALSE;
	 if (!xdr_pointer (xdrs, (char **)&objp->next, sizeof (node), (xdrproc_t) xdr_node))
		 return FALSE;
	return TRUE;
}

bool_t
xdr_query_info (XDR *xdrs, query_info *objp)
{
	register int32_t *buf;

	 if (!xdr_int (xdrs, &objp->file_size))
		 return FALSE;
	 if (!xdr_int (xdrs, &objp->num_nodes))
		 return FALSE;
	 if (!xdr_pointer (xdrs, (char **)&objp->nodes, sizeof (node), (xdrproc_t) xdr_node))
		 return FALSE;
	return TRUE;
}

bool_t
xdr_query_param (XDR *xdrs, query_param *objp)
{
	register int32_t *buf;

	 if (!xdr_int (xdrs, &objp->caller))
		 return FALSE;
	 if (!xdr_string (xdrs, &objp->filename, MAXNAMELEN))
		 return FALSE;
	return TRUE;
}
