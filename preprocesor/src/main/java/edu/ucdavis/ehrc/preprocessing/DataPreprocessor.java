package edu.ucdavis.ehrc.preprocessing;

import static edu.ucdavis.ehrc.preprocessing.StringProcessor.compressString;
import static edu.ucdavis.ehrc.preprocessing.StringProcessor.norm;
import static edu.ucdavis.ehrc.preprocessing.StringProcessor.normLastName;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import com.google.common.base.Strings;

/**
 * Date: 2/5/14
 *
 * @author: Michael Resendez
 */
public class DataPreprocessor {

	private SimpleDateFormat inDate = new SimpleDateFormat("MM/DD/yyyy");
	private SimpleDateFormat outDate = new SimpleDateFormat("MM/DD/yyyy");
	private Set<String> processedEdrsIds = new HashSet<>();
	
	private static final String[] VALUE_LABELS = {"A^", "B^", "C^", "D^", "E^", "F^", "G^", "H^", "I^", "J^", "K^"};

	public String processRequestRecord(String firstName, 
									   String middleName,
									   String lastName, 
									   String akaFirstName, 
									   String akaMiddleName,
									   String akaLastName, 
									   String dob, 
									   String gender) 
	throws 	FirstNameEmptyException, 
			LastNameEmptyException, 
			DOBEmptyException 
	{
		String fn = norm(firstName);
		String mn = norm(middleName);
		String[] ln = normLastName(lastName);
		String akafn = norm(akaFirstName);
		String akamn = norm(akaMiddleName);
		String akaln = compressString((akaLastName));
		String dobStr = nvl(dob);

		if ("UNKNOWN".equals(dobStr)) {
			dobStr = "UNK";
		} else if ("-".equals(dobStr)) {
			dobStr = "UNK";
		}
		
		if (fn.length() == 0) {
			throw new FirstNameEmptyException("Rejected empty first name");
		}

		if (ln[0].length() == 0) {
			throw new LastNameEmptyException("Rejected empty last name");
		}

		if (dobStr.length() == 0) {
			throw new DOBEmptyException("Rejected empty date of birth");
		}
		return buildRequestValueElement(new String[] {fn, mn, ln[0], ln[1], ln[2], akafn, akamn, akaln, dobStr, gender});
	}
	
	private String buildRequestValueElement(String[] values) {
		
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < values.length; i++) {
			sb.append("|"); // append a delimiter
			sb.append(values[i]);
			}
		return sb.toString();
	}

			
	public String processRecord(String elementStart,
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
								String amendmentState,
								String elementEnd) 
	throws 	UnparsableDODException, 
			DODBefore2005Exception, 
			ParseException, 
			FirstNameEmptyException, 
			LastNameEmptyException, 
			DOBEmptyException, 
			RecordIgnoredException 
	{
		Date dod = null;
		try {
			dod = inDate.parse(deathDate);
		} catch (Exception e) {
			throw new UnparsableDODException("Cannot parse death date: " + deathDate);
		}

		String fn = norm(firstName);
		String mn = norm(middleName);
		String[] ln = normLastName(lastName);
		String akafn = norm(akaFirstName);
		String akamn = norm(akaMiddleName);
		String akaln = compressString((akaLastName));
		String dobStr = nvl(dob);
		String amendmentStateStr = nvl(amendmentState);

		if ("UNKNOWN".equals(dobStr)) {
			dobStr = "UNK";
		} else if ("-".equals(dobStr)) {
			dobStr = "UNK";
		}

		if (dod.before(outDate.parse("01/01/2005"))) {
				throw new DODBefore2005Exception("death date falls before 01/01/2005: " + dod);
		}

		if (!processedEdrsIds.contains(id)) { // then we haven't seen this one before
			
			// original certificate OR registered amendment
			if ("".equals(amendmentStateStr) || "51".equals(amendmentStateStr) || "67".equals(amendmentStateStr)) {
				
				if (fn.length() == 0) {
					throw new FirstNameEmptyException("Rejected empty first name");
				}

				if (ln[0].length() == 0) {
					throw new LastNameEmptyException("Rejected empty last name");
				}

				if (dobStr.length() == 0) {
					throw new DOBEmptyException("Rejected empty date of birth");
				}

				// new record
				processedEdrsIds.add(id);
				
				return elementStart + 
						buildValueElement(VALUE_LABELS, new String[] {"RefToRefAS1." + id, fn, mn, ln[0], ln[1], ln[2], akafn, akamn, akaln, dobStr, gender}) +
						elementEnd;
			}
		}
		throw new RecordIgnoredException("Record already processed or amendmentmentState does not allow:  amendmentStateId = " + amendmentStateStr);
	}

	/**
	 * We are making the assumption here that labels and values have the same length.
	 * 
	 * @param labels
	 * @param values
	 * @return
	 */
	private String buildValueElement(String[] labels, String[] values) {
		
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < labels.length && i < values.length; i++) {
			String label = labels[i];
			String value = values[i];
			if (!Strings.isNullOrEmpty(value)) {
				if (sb.length() > 0) {
					sb.append("|"); // append a delimiter
				}
				sb.append(label);
				sb.append(value);
			}
		}
		return sb.toString();
	}
	
	public static void main(String[] argv) throws FirstNameEmptyException, LastNameEmptyException, DOBEmptyException {
		String firstName = "firstName";
		String middleName = "middleName";
		String lastName = "lastName";
		String akaFirst = "";
		String akaMiddle = "";
		String akaLast = "butch";
		String dob = "01/01/2000";
		String gender = "M";
		
//		String[] values = {"RefToRefAS1.5180", "NICK", null, "PETERS", "PETERS", "", "", "", "", "02/13/1932", "M"};
//		System.out.println(new DataPreprocessor().buildValueElement(VALUE_LABELS, values));
		System.out.println(new DataPreprocessor().processRequestRecord(firstName, middleName, lastName, akaFirst, akaMiddle, akaLast, dob, gender));
	}
	
	private String nvl(String s) {
		return s == null? "": s.trim();
	}
	
	public static class UnparsableDODException extends Exception {
		private static final long serialVersionUID = 1L;

		public UnparsableDODException(String s) {
			super(s);
		}
	}

	public static class DODBefore2005Exception extends Exception {
		private static final long serialVersionUID = 1L;

		public DODBefore2005Exception(String s) {
			super(s);
		}
	}

	public static class FirstNameEmptyException extends Exception {
		private static final long serialVersionUID = 1L;

		public FirstNameEmptyException(String s) {
			super(s);
		}
	}

	public static class LastNameEmptyException extends Exception {
		private static final long serialVersionUID = 1L;

		public LastNameEmptyException(String s) {
			super(s);
		}
	}

	public static class DOBEmptyException extends Exception {
		private static final long serialVersionUID = 1L;

		public DOBEmptyException(String s) {
			super(s);
		}
	}

	public static class RecordIgnoredException extends Exception {
		private static final long serialVersionUID = 1L;

		public RecordIgnoredException(String s) {
			super(s);
		}
	}

}
