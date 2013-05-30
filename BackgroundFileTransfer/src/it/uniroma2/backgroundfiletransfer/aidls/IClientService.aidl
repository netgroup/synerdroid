/* The package where the aidl file is located */

package it.uniroma2.backgroundfiletransfer.aidls;

/* The name of the remote service */
interface IClientService {

	/* Subscribes to a specific tag, to get intents when the other devices publish sth */
	String subscribe (String tag, String options);
	
	/* Unsubscribe to a specific tag */
	String unsubscribe (String tag);
	
	/**
	  * Publish a file that contains the digest of the files that you wish to share and the information 
	  * necessary for the other apps to recognize the files 
	  **/
	String publish (String fileLocation, String metadata, String options);
	
	
	/**
	  * Unpublish a file based on the md5digest that was returned by the publish function.
	  **/
	String unpublish (String md5digest);
	
	/**
	  * Get file from other devices, knowing its md5digest.
	  **/
	String get(String md5digest,String options);
	
	String deleteGet(String key);
	
	/**
	  *Automatically download the result files
	  */
	 String autoDownload(String tag, String options);
	
	/**
	  * Get notifications regarding the downloads of metadata or md5digest submitted.
	  **/ 
	
	String[] notify(String k);
	
}