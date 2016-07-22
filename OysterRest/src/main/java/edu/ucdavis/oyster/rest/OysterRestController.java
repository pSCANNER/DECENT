package edu.ucdavis.oyster.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.base.Strings;
import com.google.gson.Gson;

import edu.ualr.oyster.core.OysterServiceMain;
import edu.ucdavis.ehrc.preprocessing.DataPreprocessor;
import edu.ucdavis.ehrc.preprocessing.DataPreprocessor.DOBEmptyException;
import edu.ucdavis.ehrc.preprocessing.DataPreprocessor.FirstNameEmptyException;
import edu.ucdavis.ehrc.preprocessing.DataPreprocessor.LastNameEmptyException;
import edu.ucdavis.oyster.domain.MatchRequestObject;

@RestController
@RequestMapping("/oyster/rest")
/**
 * sample request string:
 * 
 * {"firstName":"NO-MARIA","middleName":"NO-TERESA","lastName":"NO-JARA","dob":"10/15/1954","gender":"F"}

headers = "Accept=application/json"
produces = "application/json"
 * @author wgweis
 *
 */
public class OysterRestController 
{
    private static final Gson gson = new Gson();

	@Autowired
	private OysterServiceMain oysterServiceMain;
	
    @RequestMapping(value = "get/deceased", method = RequestMethod.GET, produces = "application/json")
	public ResponseEntity<String> getInitialConfigurationValues(@RequestParam(value="qs", defaultValue="")  final String query)
	{  
    	if (Strings.isNullOrEmpty(query)) {
    		return new ResponseEntity<String>("{\"response\": \"Bad Request Syntax: qs parameter cannot be empty or not defined.\"}", HttpStatus.BAD_REQUEST);
    	}
    	
		MatchRequestObject matchRequestObject = null;
		
		try {
			matchRequestObject = gson.fromJson(query, MatchRequestObject.class);
		} catch (Exception ise) {
			// ignored
		}
		
    	if (matchRequestObject == null) {
    		return new ResponseEntity<String>("{\"response\": \"Bad Request Syntax\"}", HttpStatus.BAD_REQUEST);
    	}
    	
    	String valueLine;
		try {
			valueLine = new DataPreprocessor().processRequestRecord(matchRequestObject.getFirstName(), 
																   	matchRequestObject.getMiddleName(), 
																   	matchRequestObject.getLastName(), 
																	"", 
																	"", 
																	"", 
																	matchRequestObject.getDob(), 
																	matchRequestObject.getGender().toString());
		} catch (FirstNameEmptyException e) {
    		return new ResponseEntity<String>("{\"response\": \"First Name cannot be empty\"}", HttpStatus.BAD_REQUEST);
		} catch (LastNameEmptyException e) {
    		return new ResponseEntity<String>("{\"response\": \"Last Name cannot be empty\"}", HttpStatus.BAD_REQUEST);
		} catch (DOBEmptyException e) {
    		return new ResponseEntity<String>("{\"response\": \"Date of Birth cannot be empty\"}", HttpStatus.BAD_REQUEST);
		}
		return new ResponseEntity<String>("{\"response\":\"" + oysterServiceMain.attemptMatch(valueLine) + "\"}", HttpStatus.OK);
	}
}
