package edu.ucdavis.ehrc.web;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import edu.ualr.oyster.core.OysterMain;

@Path("/")
public class MatchingService {
	
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/match")
	public MatchingResponse match(MatchingRequest request) {
		MatchingResponse response = new MatchingResponse();
		
		String token = UUID.randomUUID().toString();
//		String inputFile = "C:\\temp\\ehrc-work\\run\\Input\\" + token + ".txt";
//		String outputFile = "C:\\temp\\ehrc-work\\run\\Output\\" + token + ".link";
		String inputFile = "/Users/admin/_Defects/_OysterProject/_PatrickTest/oyster/run/Input/" + token + ".txt";
		String outputFile = "/Users/admin/_Defects/_OysterProject/_PatrickTest/oyster/run/Output/" + token + ".link";
		try {
			generateInput(inputFile, request);
//			String[] args = {"-r ", "C:\\temp\\ehrc-work\\run\\IdentityResolutionRunScript.xml", "-x", inputFile, "-y", outputFile};
			String[] args = {"-r ", "/Users/admin/_Defects/_OysterProject/_PatrickTest/oyster/run/IdentityResolutionRunScript.xml", "-x", inputFile, "-y", outputFile};
		OysterMain oysterMain = new OysterMain();
			oysterMain.process(args);
			populateResponse(outputFile, response);
			
		} catch (IOException e) {
			throw new MatchingException("Unable to perform matching: ", e);
		}
		
		return response;
	}
	
	private void generateInput(String inputFile, MatchingRequest request) throws IOException {
		BufferedWriter inputFileWriter = Files.newBufferedWriter(Paths.get(inputFile), Charset.forName("US-ASCII"));
		inputFileWriter.append("EDRSID|FIRSTNAME|MIDDLENAME|LASTNAME|DOB|GENDER|BIRTHSTATECODE|BIRTHCOUNTRYCODE|DECEDENTSTATECODE|DECEDENTCOUNTRYCODE|CERTIFIERFIRSTNAME|CERTIFIERLASTNAME");
		MatchingRecord[] records = request.getMatchingRecords();
		for (MatchingRecord record : records) {
			StringBuilder sb = new StringBuilder(System.lineSeparator());
			sb.append(filter(record.getEdrsId())).append("|");
			sb.append(filter(record.getFirstName())).append("|");
			sb.append(filter(record.getMiddleName())).append("|");
			sb.append(filter(record.getLastName())).append("|");
			sb.append(filter(record.getDob())).append("|");
			sb.append(filter(record.getGender())).append("|");
			sb.append(filter(record.getBirthStateCode())).append("|");
			sb.append(filter(record.getBirthCountryCode())).append("|");
			sb.append(filter(record.getDecedentStateCode())).append("|");
			sb.append(filter(record.getDecedentCountryCode())).append("|");
			sb.append(filter(record.getCertifierFirstName())).append("|");
			sb.append(filter(record.getCertifierLastName()));
			inputFileWriter.write(sb.toString(), 0, sb.length());
		}
		inputFileWriter.close();
	}
	
	private void populateResponse(String outputFile, MatchingResponse response) throws IOException {
		List<String> lines = Files.readAllLines(Paths.get(outputFile), Charset.forName("US-ASCII"));
		response.setResultString("");
		for (String line : lines) {
			response.setResultString(response.getResultString() + line + System.getProperty("line.separator"));
		}
System.err.println(response.getResultString());		
	}
	
	private static String filter(String value) {
		if (value == null) {
			return "";
		}
		return value;
	}

	public static void main(String[] args) {
		MatchingRequest request = new MatchingRequest();
		MatchingRecord[] records = new MatchingRecord[1];
		MatchingRecord record = new MatchingRecord();
		
		//Patrick's Test Against VRBIS
		/*
		record.setFirstName("FIFTYFIVE");
		record.setMiddleName("TEST");
		record.setLastName("TEN");
		record.setDob("03/29/1950");
		record.setGender("M");
		*/
		
		record.setFirstName("MARTHA");
		record.setMiddleName("E");
		record.setLastName("MOORE");
		record.setDob("12/09/1924");
		record.setGender("F");

		records[0] = record;
		request.setMatchingRecords(records);
		
		final long startTime = System.currentTimeMillis();

		new MatchingService().match(request);
		
		System.out.printf("Total Run Time %.2f seconds.\n", (System.currentTimeMillis() - startTime)/ 1000.0 );
	}
}
