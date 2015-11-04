package fr.lis.mkeyplusWSREST.constant;

public class Constant {

	// Upload destination 
	public static final String UPLOAD_DESTINATION = "/var/www/mkeyhistock/"; // "/var/www/histock/";
	
	// Upload destination 
	public static final String URL_HISTOCK = "http://queen.snv.jussieu.fr/mkeyhistock/"; // "http://localhost/mkeyhistock/"
	
	// Maximum for random number of unknow user id
	public static final int MAX_RAND_NUMBER_UNKNOWN = 10000;
	
	
	// Extension type for the text file
	public static final String EXTENSION_TEXTFILE = "json";	
		
	// Encoding type for the text file
	public static final String ENCODING_TEXTFILE = "UTF-8";
	
	// Name for the description chosen entry in text file 
	public static final String ENTRY_NAME_DESCRIPTIONS = "history";
	
	// Name for the taxa chosen entry in text file 
	public static final String ENTRY_NAME_TAXA = "itemsselected";
	
	// Name for the SDD version entry in text file 
	public static final String ENTRY_NAME_SDDVERSION = "sddversion";	
	
}
