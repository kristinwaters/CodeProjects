package chord;

/******************************************************************************
 * Filename: ClientUtil.java
 * 
 * Description:  This file contains utility functions that are used by the
 *               Client component of the Chord system.
 *
 * Author: Kristin Dahl
 * Date: 9-6-2016
 *****************************************************************************/

import static chord.ChordUtil.M;
import static chord.ChordUtil.MAX_NODES;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Map;
import java.util.StringTokenizer;

public class ClientUtil {

	/*
	 * Function name: hashTag(String tag)
	 * 
	 * Description: This function returns a hash number created for the 'tag'
	 * input parameter.
	 */
	public int hashTag(String tag) {
		byte[] thedigest = null;
		MessageDigest md5 = null;
		try {
			md5 = MessageDigest.getInstance("MD5");
			byte[] bytesOfTag = tag.getBytes("UTF-8");
			thedigest = md5.digest(bytesOfTag);

		} catch (Exception e) {
			e.printStackTrace();
		}
		return (thedigest.hashCode() % (int) Math.pow(2, M));
	}

	
	/*
	 * Function name: parseSubscribeQuery(String query, Map<String, Integer> hashedTags, ArrayList<Integer> validTagList)
	 * 
	 * Description: This function parses a user-entered query and determines
	 * whether it is properly formatted or not. Also, this method checks the
	 * hashedTags table to see whether the tag(s) already exist.
	 * 
	 * A '0' return value indicates a malformed query. A '1' return value
	 * indicates that the query was successfully parsed.
	 */
	public String[] parseSubscribeQuery(String query, Map<String, Integer> hashedTags, ArrayList<Integer> validTagList) {
		StringTokenizer st = new StringTokenizer(query);
		int stSize = st.countTokens();

		// First, ensure that the user entered a valid number of parameters
		if ((stSize > 5) || (stSize == 2) || (stSize == 4)) {
			return null;
		}

		// Copy tokens to string array
		String strList[] = new String[stSize];
		for (int i = 0; i < stSize; i++) {
			strList[i] = st.nextToken();
		}

		// If the user entered only one tag, check and return.
		if (stSize == 1) {
			if (hashedTags.containsKey(strList[0])) {
				int tag = hashedTags.get(strList[0]);
				validTagList.add(tag);
			}
			return strList;
		}

		boolean tag_idx_0_found = false;
		boolean tag_idx_2_found = false;
		boolean tag_idx_4_found = false;

		int hash0 = 0;
		int hash2 = 0;
		int hash4 = 0;

		// At this point, there must be at least 2 tags to examine (indices 0 and 2)
		if (hashedTags.containsKey(strList[0])) {
			tag_idx_0_found = true;
			hash0 = hashedTags.get((strList[0]));
		}

		if (hashedTags.containsKey(strList[2])) {
			tag_idx_2_found = true;
			hash2 = hashedTags.get((strList[2]));
		}

		// Examine index 1 - it should be either "and" or "or"
		if (strList[1].equalsIgnoreCase("and")) {
			if ((tag_idx_0_found) && (tag_idx_2_found)) {
				validTagList.add(hash0);
				validTagList.add(hash2);
			}
		} else if (strList[1].equalsIgnoreCase("or")) {
			if (tag_idx_0_found) {
				validTagList.add(hash0);
			}
			if (tag_idx_2_found) {
				validTagList.add(hash2);
			}
		} else {
			return null;
		}

		// If this is the end of the query, then return
		if (stSize == 3) {
			return strList;
		}

		// At this point, there must be at least 3 tags to examine (indices 0, 2, and 4)
		if (hashedTags.containsKey(strList[4])) {
			tag_idx_4_found = true;
			hash4 = hashedTags.get((strList[4]));
		}

		// Examine index 3 - it should be either "and" or "or"
		if (strList[3].equalsIgnoreCase("and")) {
			if ((tag_idx_2_found) && (tag_idx_4_found)) {
				validTagList.add(hash4);
			}
		} else if (strList[3].equalsIgnoreCase("or")) {
			if (tag_idx_4_found) {
				validTagList.add(hash4);
			}
		} else {
			return null;
		}
		return strList;
	}

	
	/*
	 * Function name: displayPrintData(ArrayList<PrintData> nodeData)
	 * 
	 * Description: This function formats and prints the node data requested by
	 * the "View DHT" menu option.
	 */
	public void displayPrintData(ArrayList<PrintData> nodeData) {
		PrintData nodeItem = null;

		for (int i = 0; i < MAX_NODES; i++) {
			nodeItem = nodeData.get(i);

			System.out.println("===== Node Data =====");
			System.out.println("Node ID: " + nodeItem.nodeID);
			System.out.println("Entries: " + nodeItem.numEntries);
			System.out.println("Successor ID: " + nodeItem.succID);
			System.out.println("Predecessor ID: " + nodeItem.predID);

			// Print Finger Table
			System.out.println("\n    Finger Table:");
			System.out.println("\nindex   start   node");
			System.out.println("====================");
			for (int j = 0; j < M; j++) {
				Finger nodeFinger = nodeItem.fingers.get(j);
				System.out.format(" %1$2s      %2$2s     %3$2s\n", (j + 1), nodeFinger.start, nodeFinger.node.id);
			}
			System.out.println("\n\n");
		}
	}

	/*
	 * Function name: printNodeTrace(ArrayList<Integer> nodeTrace)
	 * 
	 * Description: This function formats and prints the node trace data for a
	 * publish or subscribe request.
	 */
	public void printNodeTrace(ArrayList<Integer> nodeTrace) {
		// If nodeTrace is empty, just return
		if (nodeTrace.size() == 0) {
			return;
		}
		
		System.out.println("\n------ Node Trace ------");
		for (int i = 0; i < nodeTrace.size(); i++) {
			System.out.println("Step " + i + " - Node ID is: " + nodeTrace.get(i));
		}
		System.out.println("\n");
	}
	
	
	public ArrayList<String> matchQueryURLs(Map<Integer, ArrayList<String>> tagsToURLs, String queryString) {
		StringTokenizer st = new StringTokenizer(queryString);
		int querySize = st.countTokens();

		// User entered only one tag - print all returned URLs
		if (querySize == 1) {
			return tagsToURLs.get(0);
		}
		
		// Copy tokens to string array
		String query[] = new String[querySize];
		for (int i = 0; i < querySize; i++) {
			query[i] = st.nextToken();
		}

		ArrayList<String> urlsToPrint = new ArrayList<String>();
		ArrayList<String> urlsTag0 = tagsToURLs.get(0);
		ArrayList<String> urlsTag2 = tagsToURLs.get(1);
		
		// User must have entered two tags.
		if (querySize == 3) {
			
			// Case:  tag0 AND tag2 - url must appear in BOTH returned URL lists.
			if (query[1].equalsIgnoreCase("AND")) {
				for (int i = 0; i < urlsTag0.size(); i++) {
					if (urlsTag2.contains(urlsTag0.get(i))) {
						urlsToPrint.add(urlsTag0.get(i));
					}
				}
			} 
			// Case:  tag0 OR tag2
			else if (query[1].equalsIgnoreCase("OR")) {
				if (urlsTag0 != null) {
					for (int i = 0; i < urlsTag0.size(); i++) {
						urlsToPrint.add(urlsTag0.get(i));
					}
				}
				if (urlsTag2 != null) {
					for (int i = 0; i < urlsTag2.size(); i++) {
						urlsToPrint.add(urlsTag2.get(i));
					}
				}
			}
			
			return urlsToPrint;
		}
		
		
		ArrayList<String> urlsTag4 = tagsToURLs.get(2);
		
		// Which URL list is the smallest?  Use this size as the index limit.
		int maxSize;
		if ((urlsTag0.size() <= urlsTag2.size()) && (urlsTag0.size() <= urlsTag4.size())) {
			maxSize = urlsTag0.size();
		}
		else if ((urlsTag2.size() <= urlsTag0.size()) && (urlsTag2.size() <= urlsTag4.size())) {
			maxSize = urlsTag2.size();
		}
		else {
			maxSize = urlsTag4.size();
		}
		
		// User must have entered three tags.
		if (querySize == 5) {
			
			// Case:  tag0 AND tag2 AND tag4 - url must appear in all three returned URL lists.
			if ((query[1].equalsIgnoreCase("AND")) && (query[3].equalsIgnoreCase("AND"))) {
				
				for (int i = 0; i < maxSize; i++) {
					if ((urlsTag2.contains(urlsTag0.get(i))) && (urlsTag4.contains(urlsTag0.get(i)))) {
						urlsToPrint.add(urlsTag0.get(i));
					}
				}
			} 
			
			// Case:  tag0 AND tag2 OR tag4
			else if ((query[1].equalsIgnoreCase("AND")) && (query[3].equalsIgnoreCase("OR"))) {
				for (int i = 0; i < maxSize; i++) {
					
					// Must have tag0 along with either tag2 or tag4
					if ((urlsTag0.contains(urlsTag2.get(i))) || (urlsTag0.contains(urlsTag4.get(i)))) {
						urlsToPrint.add(urlsTag0.get(i));
					}
				}
			} 
			
			// Case:  tag0 OR tag2 AND tag4
			else if ((query[1].equalsIgnoreCase("OR")) && (query[3].equalsIgnoreCase("AND"))) {
				for (int i = 0; i < maxSize; i++) {
					
					// Must have tag4 along with either tag0 or tag2
					if ((urlsTag4.contains(urlsTag0.get(i)) || (urlsTag4.contains(urlsTag2.get(i))))) {
						urlsToPrint.add(urlsTag4.get(i));
					}
				}
			} 
			
			// Case:  tag0 OR tag2 OR tag4
			else if ((query[1].equalsIgnoreCase("OR")) && (query[3].equalsIgnoreCase("OR"))) {
				if (urlsTag0 != null) {
					for (int i = 0; i < urlsTag0.size(); i++) {
						urlsToPrint.add(urlsTag0.get(i));
					}
				}
				
				if (urlsTag2 != null) {
					for (int i = 0; i < urlsTag2.size(); i++) {
						urlsToPrint.add(urlsTag2.get(i));
					}
				}
				
				if (urlsTag4 != null) {
					for (int i = 0; i < urlsTag4.size(); i++) {
						urlsToPrint.add(urlsTag4.get(i));
					}
				}
			} 
		}
			return urlsToPrint;
	}
}
