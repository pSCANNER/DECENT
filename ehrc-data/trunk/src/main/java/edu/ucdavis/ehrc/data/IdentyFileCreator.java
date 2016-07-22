package edu.ucdavis.ehrc.data;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class IdentyFileCreator {
	
	private static Connection connection;
	
	/**
	 * Patrick's VRBIS credentials
	 */
/*	
	private static final String CONNECT_URL = "jdbc:oracle:thin:@192.168.41.174:1521:EDRSDEV";
	private static final String USERNAME = "VRBISDBUSER";
	private static final String PASSWORD = "oracle";
	private static final String SQL_STATEMENT = "select EDRSID, FIRSTNAME, MIDDLENAME, LASTNAME, DOB, GENDER, BIRTHSTATECODE, BIRTHCOUNTRYCODE, DECEDENTSTATECODE, DECEDENTCOUNTRYCODE, D__CERTIFIERFIRSTNAME, D__CERTIFIERLASTNAME from DC_CERTIFICATE" ;
*/
	
	/**
	 * Credentials for Bill's Test Database.
	 */
	private static final String CONNECT_URL = "jdbc:oracle:thin:@hiro.ucdavis.edu:1521:edrstst";
	private static final String USERNAME = "edrs";
	private static final String PASSWORD = "(40CutDevoidFlier)";
	private static final String SQL_STATEMENT =  
			 "select EDRSID, FIRSTNAME, MIDDLENAME, LASTNAME, DOB, GENDER, 'CA' AS BIRTHSTATECODE, 'US' AS BIRTHCOUNTRYCODE, 'CA' AS DECEDENTSTATECODE, 'US' AS DECEDENTCOUNTRYCODE, 'CERTIFIERFIRSTNAME' AS D__CERTIFIERFIRSTNAME, 'CERTIFIERLASTNAME' AS D__CERTIFIERLASTNAME " +
			 "FROM CERTIFICATE " + 
			 "WHERE OVRREFERENCENUMBER IS NOT NULL " +
			 "AND ROWNUM <= 50000";
	
	
	public static void main(String[] args) {

	       System.out.println("Working Directory = " +
	               System.getProperty("user.dir"));
		load();

	}
	
	private static String getHeader() throws IOException {
		byte[] bytes = Files.readAllBytes(Paths.get("src/main/resources/IdentityHeader.txt"));
		return new String((bytes));
	}

	private static String getFooter() throws IOException {
		byte[] bytes = Files.readAllBytes(Paths.get("src/main/resources/IdentityFooter.txt"));
		return new String((bytes));
	}

	private static void load() {
		try {
			
			final long startTime = System.currentTimeMillis();
			
			int recordCount = 0;
			BufferedWriter writer = Files.newBufferedWriter(Paths.get("/temp/MyIdentityFile.idty"), Charset.forName("US-ASCII"));			
			
			writer.write(getHeader());

			startConnection();
			PreparedStatement  stmt = connection.prepareStatement(SQL_STATEMENT);
			ResultSet rs = stmt.executeQuery();
			while (rs.next()) {

				writer.write(String.format("\t\t<Identity Identifier=\"%016d\" CDate=\"2015-07-31\">\n", recordCount));
                
				writer.write("\t\t\t<References>\n");

				writer.write("\t\t\t\t<Reference>\n");
				
				writer.write(String.format("\t\t\t\t\t<Value>A^source1.%s|C^%s|D^%s|E^%s|F^%s|G^%s|H^%s|I^%s|J^%s|K^%s|L^%s|M^%s</Value>\n", 
																														 rs.getString(1), 
																														 rs.getString(2), 
																														 rs.getString(3), 
																														 rs.getString(4), 
																														 rs.getString(5), 
																														 rs.getString(6),
																														 rs.getString(8),
																														 rs.getString(7),
																														 rs.getString(9),
																														 rs.getString(10),
																														 rs.getString(11),
																														 rs.getString(12)
																														 ));
				writer.write("\t\t\t\t\t<Traces/>\n");
				writer.write("\t\t\t\t</Reference>\n");
				
				writer.write("\t\t\t</References>\n");

				writer.write("\t\t</Identity>\n");
				
				if (++recordCount % 1000 == 0) {
					System.out.printf("%10d records written.\n", recordCount);
				}
			}
			
			System.out.printf("%10d total records written in %.2f seconds.\n", recordCount, (System.currentTimeMillis() - startTime)/ 1000.0 );
			connection.close();

			writer.write(getFooter());
			writer.close();
	
//		} catch (FileNotFoundException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private static String filter(String value) {
		if (value == null) {
			return "";
		}
		return value;
	}

	private static void startConnection() throws ClassNotFoundException, SQLException {
		Class.forName("oracle.jdbc.driver.OracleDriver");
		connection = DriverManager.getConnection(CONNECT_URL, USERNAME, PASSWORD);
	}
}
