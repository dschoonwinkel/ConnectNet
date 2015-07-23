package be.ppareit.swiftp.server;

/**
 * Listens and passes along the status of a FTP connection
 * 
 * @author Daniel Schoonwinkel
 *
 */

public interface StatusListener {
	public void updateStatus(String status);
	public String getStatus(String status);
	
	
	public static final String ALL_GOOD = "All is well";
	public static final String STARTED_SUCCESSFULLY = "Started";
	public static final String FINISHED_SUCCESSFULLY = "Finished Successfully";
	public static final String BUSY_WORKING = "Busy working: ";
}
