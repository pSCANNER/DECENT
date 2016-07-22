package edu.ucdavis.pathinfo.ehrc;

import com.Ostermiller.util.CSVParser;
import com.Ostermiller.util.CSVPrinter;
import com.Ostermiller.util.LabeledCSVParser;

import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;

import static edu.ucdavis.pathinfo.ehrc.StringProcessor.compressString;
import static edu.ucdavis.pathinfo.ehrc.StringProcessor.norm;
import static edu.ucdavis.pathinfo.ehrc.StringProcessor.normLastName;

/**
 * Date: 2/5/14
 *
 * @author: Michael Resendez
 */
public class ReferenceDataPreprocessor {

	public static String [] HEADER = new String[]{"EDRSID", "FirstName", "MiddleName", "LastName", "CS1",  "CS2",  "AkaFirst", "AkaMiddle", "AkaLast", "DOB", "Gender"};

	public void process(File inputFile, File outputFile) throws IOException, ParseException {

		SimpleDateFormat inDate = new SimpleDateFormat("MM/DD/yyyy");
		SimpleDateFormat outDate = new SimpleDateFormat("MM/DD/yyyy");
	    SimpleDateFormat year = new SimpleDateFormat("yyyy");

		BufferedReader reader = new BufferedReader(new FileReader(inputFile));
		LabeledCSVParser parse = new LabeledCSVParser(new CSVParser(reader));
		parse.changeDelimiter('|');

		CSVPrinter all = new CSVPrinter(new FileOutputStream(outputFile + "_all.csv"));
		all.writeln(HEADER);

		HashMap<Integer, CSVPrinter> printers = new HashMap<Integer, CSVPrinter>();

		CSVPrinter printer = null;
		int recordCounter = 0;
		int counter = 0;
		int rejected = 0;
		String[] aLine = null;
		String lastID = null;
		while ((aLine = parse.getLine()) != null) {

			try {

				counter++;

				Date dod = null;
				try {
					dod = inDate.parse(parse.getValueByLabel("DEATHDATE"));
				} catch (Exception e) {
					System.out.println("Cannot parse record: " + Arrays.toString(aLine));
					continue;
				}

				String fn = norm(parse.getValueByLabel("FIRSTNAME"));
				String mn = norm(parse.getValueByLabel("MIDDLENAME"));
				String[] ln = normLastName(parse.getValueByLabel("LASTNAME"));
				String akafn = norm(parse.getValueByLabel("AKAFIRSTNAME"));
				String akamn = norm(parse.getValueByLabel("AKAMIDDLENAME"));
				String akaln = compressString((parse.getValueByLabel("AKALASTNAME")));
				String dobStr = parse.getValueByLabel("DOB");
				String gender = parse.getValueByLabel("GENDER");
				String amendmentState = parse.getValueByLabel("AMENDMENTSTATEID");

				if ("UNK".equals(dobStr)) {
				} else if ("UNKNOWN".equals(dobStr)) {
					dobStr = "UNK";
				} else if ("-".equals(dobStr)) {
					dobStr = "UNK";
				}

				if (dod.before(outDate.parse("01/01/2005"))) {
					rejected++;
					System.out.print("\nREJECTED DOD (" + parse.getValueByLabel("EDRSID") + ") ");
					for (String label : parse.getLabels()) {
						System.out.print(String.format(", %s == %s", label, parse.getValueByLabel(label)));
					}
				} else {

					String id = parse.getValueByLabel("EDRSID");
					Integer yearInt = Integer.parseInt(year.format(dod));
					if (!printers.containsKey(yearInt)) {
						printers.put(yearInt, new CSVPrinter(new FileOutputStream(outputFile + String.format("_%d.csv", yearInt))));
						printers.get(yearInt).writeln(HEADER);
					}
					printer = printers.get(yearInt);

					if (!id.equals(lastID)) { //

						if (amendmentState == null || "".equals(amendmentState) || "51".equals(amendmentState) || "67".equals(amendmentState)) { //original certificate OR registered amendment

							if ((fn.length() == 0)
									|| (ln[0].length() == 0)
									|| (dobStr.length() == 0)) {

								rejected++;
								System.out.print("\nREJECTED FN,LN OR DOB (" + parse.getValueByLabel("EDRSID") + ") ");
								for (String label : parse.getLabels()) {
									System.out.print(String.format(", %s == %s", label, parse.getValueByLabel(label)));
								}

							} else {

								lastID = id; // new record

								printer.writeln(new String[]{id, fn, mn, ln[0], ln[1], ln[2], akafn, akamn, akaln, dobStr, gender});
								all.writeln(new String[]{id, fn, mn, ln[0], ln[1], ln[2], akafn, akamn, akaln, dobStr, gender});
								recordCounter++;
							}
						}
					}
				}

			} catch (Exception e) {
				System.out.println(aLine);
				throw new RuntimeException(e);
			}
		}

		System.out.println(String.format("%d lines processed.", counter));
		System.out.println(String.format("%d lines rejected because deathdate was prior to 01/01/2005, or fields were blank.", rejected));
		System.out.println(String.format("%d records written.", recordCounter));

		for (CSVPrinter csvPrinter : printers.values()) {
			csvPrinter.close();
		}

		all.close();

	}

	private String padDate(String date) {
		String lastTwo = date.substring(date.length() - 2);
		int i = Integer.parseInt(lastTwo);
		i = (i <= 14) ? 2000+i : 1900+i;
		return date.substring(0, date.length() - 2) + i;
	}

	public static void main(String[] args) throws IOException, ParseException {

		try {
			ReferenceDataPreprocessor ip = new ReferenceDataPreprocessor();
			ip.process(new File(args[1]), new File(args[2]));
		} catch (Exception e) {
			System.out.println(e);
		}

	}

}
