package edu.ucdavis.ehrc.data;

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

public class Loader {
	
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
			 "AND ROWNUM <= 2";
	
	
	public static void main(String[] args) {

		load();

	}
	
	private static void load() {
		try {
			
			final long startTime = System.currentTimeMillis();
			
			int recordCount = 0;
			startConnection();
			BufferedWriter writer = Files.newBufferedWriter(Paths.get("/temp/MergePurgeTest.txt"), Charset.forName("US-ASCII"));			
			
			writer.write("EDRSID|FIRSTNAME|MIDDLENAME|LASTNAME|DOB|GENDER|BIRTHSTATECODE|BIRTHCOUNTRYCODE|DECEDENTSTATECODE|DECEDENTCOUNTRYCODE|CERTIFIERFIRSTNAME|CERTIFIERLASTNAME");
			
			PreparedStatement  stmt = connection.prepareStatement(SQL_STATEMENT);
			ResultSet rs = stmt.executeQuery();
			while (rs.next()) {
				StringBuilder sb = new StringBuilder(System.lineSeparator());
				sb.append(filter(rs.getString(1))).append("|");
				sb.append(filter(rs.getString(2))).append("|");
				sb.append(filter(rs.getString(3))).append("|");
				sb.append(filter(rs.getString(4))).append("|");
				sb.append(filter(rs.getString(5))).append("|");
				sb.append(filter(rs.getString(6))).append("|");
				sb.append(filter(rs.getString(7))).append("|");
				sb.append(filter(rs.getString(8))).append("|");
				sb.append(filter(rs.getString(9))).append("|");
				sb.append(filter(rs.getString(10))).append("|");
				sb.append(filter(rs.getString(11))).append("|");
				sb.append(filter(rs.getString(12)));
				writer.write(sb.toString(), 0, sb.length());
				
				if (++recordCount % 1000 == 0) {
					System.out.printf("%10d records written.\n", recordCount);
				}
			}
			
			System.out.printf("%10d total records written in %.2f seconds.\n", recordCount, (System.currentTimeMillis() - startTime)/ 1000.0 );
			writer.close();
			connection.close();
			
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
