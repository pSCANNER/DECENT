package edu.ucdavis.oyster.domain;

import org.junit.Test;

import com.google.gson.Gson;

import edu.ualr.oyster.core.OysterServiceMain;
import edu.ucdavis.ehrc.preprocessing.DataPreprocessor;
import edu.ucdavis.ehrc.preprocessing.DataPreprocessor.DOBEmptyException;
import edu.ucdavis.ehrc.preprocessing.DataPreprocessor.FirstNameEmptyException;
import edu.ucdavis.ehrc.preprocessing.DataPreprocessor.LastNameEmptyException;
import edu.ucdavis.oyster.domain.MatchRequestObject;

public class MatchRequestObjectTest {

    private static final Gson gson = new Gson();
	
	@Test 
	public void multiMatchTest() throws FirstNameEmptyException, LastNameEmptyException, DOBEmptyException {
		
		final String[] requests = {
									"{\"firstName\":\"MUNSTER\",\"middleName\":\"TERESA\",\"lastName\":\"JARA\",\"akaFirst\":\"MARIA\",\"akaMiddle\":\"JON\",\"akaLast\":\"SAN TERESA\",\"dob\":\"10/15/1954\",\"gender\":\"F\"}",
									"{\"firstName\":\"MARIA\",\"middleName\":\"JARA\",\"lastName\":\"NOJARA\",\"akaFirst\":\"MARIA\",\"akaMiddle\":\"JON\",\"akaLast\":\"SAN TERESA\",\"dob\":\"10/15/1954\",\"gender\":\"M\"}", // NO Match
									"{\"firstName\":\"MARIA\",\"middleName\":\"JARA\",\"lastName\":\"JARA\",\"akaFirst\":\"MARIA\",\"akaMiddle\":\"JON\",\"akaLast\":\"SAN TERESA\",\"dob\":\"10/15/1954\",\"gender\":\"M\"}",
									"{\"firstName\":\"MARIA\",\"middleName\":\"TERESA\",\"lastName\":\"JARA\",\"akaFirst\":\"MARIA\",\"akaMiddle\":\"JON\",\"akaLast\":\"SAN TERESA\",\"dob\":\"10/15/1955\",\"gender\":\"F\"}",
									"{\"firstName\":\"WINNIE\",\"middleName\":\"THE\",\"lastName\":\"POOH\",\"akaFirst\":\"WINNY\",\"akaMiddle\":\"D\",\"akaLast\":\"POO\",\"dob\":\"9/15/1919\",\"gender\":\"M\"}",
								  };


		OysterServiceMain oysterServiceMain = new OysterServiceMain("src/test/resources/ehrc/matching/TestIdentityRunScript.xml");

		for (String request: requests) {
			
			MatchRequestObject matchRequestObject = gson.fromJson(request, MatchRequestObject.class);
			
			String valueLine = new DataPreprocessor().processRequestRecord(
					matchRequestObject.getFirstName(), 
				   	matchRequestObject.getMiddleName(), 
				   	matchRequestObject.getLastName(), 
					matchRequestObject.getAkaFirst(), 
					matchRequestObject.getAkaMiddle(), 
					matchRequestObject.getAkaLast(), 
					matchRequestObject.getDob(), 
					matchRequestObject.getGender().toString());
	
			String result = oysterServiceMain.attemptMatch(valueLine);
			System.err.println("Result Returned:");
			System.err.println(result);
		}
	}

}
