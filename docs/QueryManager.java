package it.uniroma2.mcpquerymanagerhelloworld;

import android.app.*;
import android.content.*;
import android.os.*;
import it.uniroma2.mobilecollaborationplatform.querymanager.*;

public class QueryManager extends Service {
	@Override
	public IBinder onBind(Intent intent) {
		return addBinder;
	}

	private final QueryManagerOp.Stub addBinder = new QueryManagerOp.Stub() {
		/**
		 * This function returns a query response string
		 */
		public String getQueryResponse(String query) {
			// HERE THE PROGRAM GENERATES AN ANSWER TO THE QUERY AND RETURNS IT
			return "Query was: "+query;
		}
		
		/**
		 * This function returns a file knowing its MD5 digest, null if file is not present
		 */
		public String getFile(String digest) {
			// THE PROGRAM LOOKS FOR A FILE WITH THE GIVEN DIGEST INTO ITS OWN KNOWN FILES
			// AND RETURNS THE COMPLETE PATH TO THE FILE ON THE PHONE STORAGE
			// (FOR EXAMPLE /mnt/sdcard/path/to/file.ext)
			return null;
		}
	};
}