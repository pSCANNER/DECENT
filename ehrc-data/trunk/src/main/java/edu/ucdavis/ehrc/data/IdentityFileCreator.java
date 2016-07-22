package edu.ucdavis.ehrc.data;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

import org.apache.log4j.Logger;

import edu.ucdavis.ehrc.preprocessing.DataPreprocessor;
import static edu.ucdavis.ehrc.preprocessing.DataPreprocessor.DOBEmptyException;
import static edu.ucdavis.ehrc.preprocessing.DataPreprocessor.DODBefore2005Exception;
import static edu.ucdavis.ehrc.preprocessing.DataPreprocessor.FirstNameEmptyException;
import static edu.ucdavis.ehrc.preprocessing.DataPreprocessor.LastNameEmptyException;
import static edu.ucdavis.ehrc.preprocessing.DataPreprocessor.RecordIgnoredException;
import static edu.ucdavis.ehrc.preprocessing.DataPreprocessor.UnparsableDODException;

public class IdentityFileCreator {
	
	private static Logger logger = Logger.getLogger(IdentityFileCreator.class);
	
	private static Connection connection;
	
	private static Properties prop = new Properties();
	static	{
		InputStream is = IdentityFileCreator.class.getResourceAsStream( "/Connection.properties" );
		try {
			prop.load(is);
		} catch (IOException e) {
			logger.fatal("Exception loading Connection properties", e);
			System.exit(-1);
		}
	}
	
	private Integer maxRowsToFetch;
	
	private String outputFile;
	
	private static final String SQL_STATEMENT =  
			"SELECT EDRSID, " + 
		    "   FIRSTNAME, " +  
		    "   MIDDLENAME, " +  
		    "   LASTNAME, " +  
		    "   AKAFIRSTNAME, " +  
		    "   AKAMIDDLENAME, " + 
		    "   AKALASTNAME, " + 
		    "   DOB, " +  
		    "   GENDER, " + 
		    "   TO_CHAR(DEATHDATE, 'MM/DD/YYYY') AS DEATHDATE, " + 
		    "   AMENDMENTSTATEID " + 
			"FROM CERTIFICATE " +   
			"WHERE OVRREFERENCENUMBER IS NOT NULL " + 
			"AND ROWNUM <= ?";

//	private static final String SQL_STATEMENT =  
//			"SELECT '666' AS EDRSID," + 
//		    "   'FIRSTNAME' AS FIRSTNAME," +  
//		    "   'MIDDLENAME' AS MIDDLENAME," +  
//		    "   'LASTNAME' AS LASTNAME," +  
//		    "   'AKAFIRSTNAME' AS AKAFIRSTNAME," +  
//		    "   'AKAMIDDLENAME' AS AKAMIDDLENAME, " + 
//		    "   'AKALASTNAME' AS AKALASTNAME, " + 
//		    "   '01/01/1960' AS DOB, " +  
//		    "   'M' AS GENDER, " + 
//		    "   '09/01/2015' AS DEATHDATE, " + 
//		    "   ''  AS AMENDMENTSTATEID " + 
//			"FROM DUAL WHERE ROWNUM <= ?"; 

	/**
	 * Load a resource with the path specified by filePath
	 * @param fileName
	 * @return String with the contents of the resource, if possible
	 * @throws IOException
	 */
	private static String getResource(String filePath) throws IOException {

		StringBuilder result = new StringBuilder("");
		InputStream is = IdentityFileCreator.class.getResourceAsStream( filePath );
		InputStreamReader isr = new InputStreamReader(is);
		BufferedReader br = new BufferedReader(isr);
		
		String line;
		while ((line = br.readLine()) != null) {
			result.append(String.format("%s%n", line));
		}
		return result.toString();
	}
	
	private static final String VALUE_ELEMENT_START = "\t\t\t\t\t<Value>"; 
	
	private static final String VALUE_ELEMENT_END = "</Value>\n"; 

	/**
	 * Get the Identity file header
	 * @return The contents of the header file
	 * @throws IOException
	 */
	private static String getHeader() throws IOException {
		return getResource("/IdentityHeaderMichael.txt");
	}

	/**
	 * Get the Identity file footer
	 * @return The contents of the footer file
	 * @throws IOException
	 */
	private static String getFooter() throws IOException {
		return getResource("/IdentityFooter.txt");
	}

	/**
	 * Try to get the maximum records as specified in the Connection.properties
	 * 
	 * @return the parsed integer if possible
	 */
	private Integer getMaxRecords(String maxRecords) {
		try 
		{
			return Integer.valueOf(maxRecords);
		} catch (NumberFormatException nfe) {
			System.err.println("Error parsing maxrecords property value.  maxrecord must be an Integer.  Attempted value was: " + maxRecords);
			System.exit(-1);
		}
		
		// this is dead code, but java won't compile without it.
		return null;
	}
	
	/**
	 * Gets a connection to the oracle database specified by the connection information in the Connection.properties file.
	 * 
	 * @throws ClassNotFoundException
	 * @throws SQLException
	 */
	private static void startConnection() throws ClassNotFoundException, SQLException {
		Class.forName("oracle.jdbc.driver.OracleDriver");
		connection = DriverManager.getConnection(prop.getProperty("connection_url"), prop.getProperty("username"), prop.getProperty("password"));
	}

	/**
	 * Main flow here. Writes records from the database using the connection information, the sql statement defined above and the maximum records defined in
	 * the Connection.properties file.
	 * @throws IOException 
	 * @throws SQLException 
	 * @throws ClassNotFoundException 
	 */
	private void load() throws IOException, ClassNotFoundException, SQLException {
		final long startTime = System.currentTimeMillis();
		
		String cDate = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
		DataPreprocessor dataPreprocessor = new DataPreprocessor();
		
		int recordCount = 0;
		int edrsCount = 0;
		int rejectedUnparsableDODCount = 0;
		int rejectedFirstNameEmptyCount = 0;
		int rejectedLastNameEmptyCount = 0;
		int rejectedDOBEmptyCount = 0;
		int rejectedDOBBefore2005Count = 0;
		int rejectedRecordAlreadyProcessedCount = 0;
		
		BufferedWriter writer = Files.newBufferedWriter(Paths.get(outputFile), Charset.forName("US-ASCII"));			
		
		writer.write(getHeader());

		startConnection();
		PreparedStatement  stmt = connection.prepareStatement(SQL_STATEMENT);
		stmt.setInt(1, maxRowsToFetch);
		
		ResultSet rs = stmt.executeQuery();
		while (rs.next()) {

				if (++recordCount % 1000 == 0) {
					logger.info(String.format("%10d records written.", recordCount));
				}

				try {
					String valueLine = dataPreprocessor.processRecord(VALUE_ELEMENT_START, 
																	  rs.getString("EDRSID"), 
																	  rs.getString("FIRSTNAME"), 
																	  rs.getString("MIDDLENAME"), 
																	  rs.getString("LASTNAME"), 
																	  rs.getString("AKAFIRSTNAME"), 
																	  rs.getString("AKAMIDDLENAME"), 
																	  rs.getString("AKALASTNAME"), 
																	  rs.getString("DOB"), 
																	  rs.getString("GENDER"), 
																	  rs.getString("DEATHDATE"), 
																	  rs.getString("AMENDMENTSTATEID"),
																	  VALUE_ELEMENT_END);
					
					writer.write(String.format("\t\t<Identity Identifier=\"%016d\" CDate=\"" + cDate + "\">\n", recordCount));
		            
					writer.write("\t\t\t<References>\n");

					writer.write("\t\t\t\t<Reference>\n");
					
					
					writer.write(valueLine);
					
					writer.write("\t\t\t\t\t<Traces/>\n");
					writer.write("\t\t\t\t</Reference>\n");
					
					writer.write("\t\t\t</References>\n");

					writer.write("\t\t</Identity>\n");
					edrsCount++;
					

				} catch (UnparsableDODException | DODBefore2005Exception
						| ParseException | FirstNameEmptyException
						| LastNameEmptyException | DOBEmptyException
						| RecordIgnoredException e) 
				{
					logger.debug(getRejectInformation(e, 
													 rs.getString("EDRSID"), 
													 rs.getString("FIRSTNAME"), 
													 rs.getString("MIDDLENAME"), 
													 rs.getString("LASTNAME"), 
													 rs.getString("AKAFIRSTNAME"), 
													 rs.getString("AKAMIDDLENAME"), 
													 rs.getString("AKALASTNAME"), 
													 rs.getString("DOB"), 
													 rs.getString("GENDER"), 
													 rs.getString("DEATHDATE"), 
													 rs.getString("AMENDMENTSTATEID")));
					
					if (e instanceof UnparsableDODException) {
						rejectedUnparsableDODCount++;
					} else if (e instanceof DODBefore2005Exception) {
						rejectedDOBBefore2005Count++;
					} else if (e instanceof FirstNameEmptyException) {
						rejectedFirstNameEmptyCount++;
					} else if (e instanceof LastNameEmptyException) {
						rejectedLastNameEmptyCount++;
					} else if (e instanceof DOBEmptyException) {
						rejectedDOBEmptyCount++;
					} else if (e instanceof RecordIgnoredException) {
						rejectedRecordAlreadyProcessedCount++;
					} else {
						System.err.println("Other Exception: " + e);
					}
					
				}
		}

		connection.close();

		writer.write(getFooter());
		writer.close();

		logger.info("Identity File created in " + ((System.currentTimeMillis() - startTime)/ 1000.0 ));
		logger.info(String.format("%10d total records retrieved.", recordCount));
		logger.info(String.format("%10d total  unique EDRSID records written.", edrsCount));
		logger.info(String.format("%10d total records rejected because Death Date cannot be parsed.", rejectedUnparsableDODCount));
		logger.info(String.format("%10d total records rejected because Death Date before 01/01/2005.", rejectedDOBBefore2005Count));
		logger.info(String.format("%10d total records rejected because First Name field empty.", rejectedFirstNameEmptyCount));
		logger.info(String.format("%10d total records rejected because Last Name field empty.", rejectedLastNameEmptyCount));
		logger.info(String.format("%10d total records rejected because Date of Birth field empty.", rejectedDOBEmptyCount));
		logger.info(String.format("%10d total records rejected because previous record with same EDRSID already processed.", rejectedRecordAlreadyProcessedCount));

	}

	private String getRejectInformation(Exception e, 
										 String id, 
										 String firstName, 
										 String middleName,
										 String lastName, 
										 String akaFirstName, 
										 String akaMiddleName,
										 String akaLastName, 
										 String dob, 
										 String gender, 
										 String deathDate,
										 String amendmentState) {
		
		
		return "Record rejected because of Exception: " + e + ", " + 
				printRecord(id, firstName, middleName, lastName, akaFirstName, akaMiddleName, akaLastName, dob, gender, deathDate, amendmentState);

	}
	
	private String printRecord(String id, 
							   String firstName, 
							   String middleName,
							   String lastName, 
							   String akaFirstName, 
							   String akaMiddleName,
							   String akaLastName, 
							   String dob, 
							   String gender, 
							   String deathDate,
							   String amendmentState) {
	
		return "{{\"EDRSID\": \"" + id + "\"}," +
		        "{\"FIRSTNAME\": \"" + firstName + "\"}," +
		        "{\"MIDDLENAME\": \"" + middleName + "\"}," +
		        "{\"LASTNAME\": \"" + lastName + "\"}," +
		        "{\"AKAFIRSTNAME\": \"" + akaFirstName + "\"}," +
		        "{\"AKAMIDDLENAME\": \"" + akaMiddleName + "\"}," +
		        "{\"AKALASTNAME\": \"" + akaLastName + "\"}," +
		        "{\"DOB\": \"" + dob + "\"}," +
		        "{\"GENDER\": \"" + gender + "," +
		        "{\"DEATHDATE\": \"" + deathDate + "," +
		        "{\"AMENDMENTSTATE\": \"" + amendmentState + "\"}}";
 	}
	
	private void usage() {
		System.err.println("Usage: -o <OutputFile> [-m <MaxRowsToFetch>]");
		System.exit(-1);
	}
	
	private void processCommandLineArguments(String[] args) {
		if (args.length <= 0) {
			usage();
		}
		
		for (int i = 0; i < args.length; i++) {
			switch(args[i]) {
			case "-m": 	if (++i >= args.length) {
							System.err.println("-m must be followed by a numeric argument.");
							usage();
						}
						maxRowsToFetch = getMaxRecords(args[i]);
						break;
						
			case "-o": 	if (++i >= args.length) {
							System.err.println("-o must be followed by an output file location.");
							usage();
						}
						this.outputFile = args[i];
						break;
			default:	usage();			
						break;
			}
		}
		
		if (maxRowsToFetch == null) {
			maxRowsToFetch = 1; //Integer.MAX_VALUE;
		}
		
		if (outputFile == null) {
			usage();
		}
	}
	
	public static void main(String[] args) throws IOException {

try {
	Thread.sleep(100000);
} catch (Exception e) {}

		IdentityFileCreator identityFileCreator = new IdentityFileCreator();
		identityFileCreator.processCommandLineArguments(args);
		
		try {
			identityFileCreator.load();
		} catch (ClassNotFoundException | SQLException e) {
			logger.fatal("Exception attempting write of Identity File", e);
			System.exit(-1);
		}
	}
}
