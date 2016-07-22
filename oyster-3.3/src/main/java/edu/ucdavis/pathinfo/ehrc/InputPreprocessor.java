package edu.ucdavis.pathinfo.ehrc;

import com.Ostermiller.util.CSVParser;
import com.Ostermiller.util.CSVPrinter;
import com.Ostermiller.util.LabeledCSVParser;

import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static edu.ucdavis.pathinfo.ehrc.StringProcessor.normLastName;
import static edu.ucdavis.pathinfo.ehrc.StringProcessor.normName;

/**
 * Date: 2/5/14
 *
 * @author: Michael Resendez
 */
public class InputPreprocessor {

	protected static SimpleDateFormat inDate1 = new SimpleDateFormat("d-MMM-yyyy");
	protected static SimpleDateFormat inDate2 = new SimpleDateFormat("MM/dd/yyyy");
	protected static SimpleDateFormat outDate = new SimpleDateFormat("MM/dd/yyyy");
	protected SimpleDateFormat year = new SimpleDateFormat("yyyy");

	class PrinterHandler implements Comparable<PrinterHandler> {

		private final Integer year;
		private int counter = 0;
		private CSVPrinter printer;

		PrinterHandler(String file, int year) throws IOException {
			init(new CSVPrinter(new FileOutputStream(file + ((year > 0) ? ("_" + year) : "") + ".csv")));
			this.year = year;
		}

		public PrinterHandler(String file) throws IOException {
			this(file, 0);
		}

		private void init(CSVPrinter printer) throws IOException {
			this.printer = printer;
			this.printer.writeln(new String[]{"FileID", "FirstName", "MiddleName", "LastName", "CS1", "CS2", "AkaFirst", "AkaMiddle", "AkaLast",  "DOB", "Gender"});
		}

		public void writeln(String[] values) throws IOException {
			printer.writeln(values);
			counter++;
		}

		int getCounter() {
			return counter;
		}

		public int compareTo(PrinterHandler o) {
			return year.compareTo(o.year);
		}

		public void close() throws IOException {
			printer.close();
		}

		Integer getYear() {
			return year;
		}
	}

	public void process(File inputFile, String outputFile) throws IOException, ParseException {


		BufferedReader reader = new BufferedReader(new FileReader(inputFile));
		LabeledCSVParser parse = new LabeledCSVParser(new CSVParser(reader));

		SortedMap<Integer, PrinterHandler> printers = new TreeMap<Integer, PrinterHandler>();
		PrinterHandler allPrinter = new PrinterHandler(outputFile + "_all");

		printers.put(0, new PrinterHandler(outputFile + "_rejections", 0));

		PrinterHandler printer = null;
		// PAT_FIRST_NAME,PAT_MIDDLE_NAME,PAT_LAST_NAME,BIRTH_DATE,GENDER,BIRTH_PLACE,ADDRESS,CITY,STATE,ZIP,COUNTRY,PROVIDER,DEATH_DATE

		int counter = 0;

		while (parse.getLine() != null) {
			counter++;
//			Date dod = inDate2.parse(padDate(parse.getValueByLabel("DEATH_DATE"), 16));
			Date dod = inDate2.parse(parse.getValueByLabel("DEATH_DATE"));

			Integer yearInt = Integer.parseInt(year.format(dod));
			if (!printers.containsKey(yearInt)) {
				printers.put(yearInt, new PrinterHandler(outputFile, yearInt));
			}
			printer = printers.get(yearInt);

			String fn = parse.getValueByLabel("PAT_FIRST_NAME");
			String mn = parse.getValueByLabel("PAT_MIDDLE_NAME");
			String ln = parse.getValueByLabel("PAT_LAST_NAME");

			String state = parse.getValueByLabel("STATE");
//			if (!"CA".equals(state)) {
				//System.out.println("Rejecting record because state is: " + state);
//				printer = printers.get(0); //rejection printer
//			}

			String[] names = normName(fn, mn, ln);
			String[] surnames = normLastName(ln);

//			Date dob = inDate2.parse(padDate(parse.getValueByLabel("BIRTH_DATE"), 10));
			Date dob = inDate2.parse(parse.getValueByLabel("BIRTH_DATE"));
			String gender = parse.getValueByLabel("GENDER");

			String [] outString = new String[]{Integer.toString(counter), names[0], names[1], surnames[0], surnames[1], surnames[2], "", "", "", outDate.format(dob), gender.substring(0, 1)};
			printer.writeln(outString);
			if (printer.getYear() > 0) {
				allPrinter.writeln(outString);
			}

		}

		System.out.println(String.format("%d total lines processed.", counter));

		System.out.println(String.format("\n%5s   %12s", "year", "input lines"));
		for ( int i = 0; i < 20; i++) System.out.print("-");
		System.out.println("");
		for (PrinterHandler printerHandler : printers.values()) {
			if (printerHandler.getCounter() > 0) {
				System.out.println(String.format("%05d %12d", printerHandler.getYear(), printerHandler.getCounter() ));
				printerHandler.close();
			}
		}
		System.out.println(String.format("%5s %12d", "TOTAL", allPrinter.getCounter() ));
		allPrinter.close();

	}

	protected static String padDate(String date, int lowerbound) {
		String lastTwo = date.substring(date.length() - 2);
		int i = Integer.parseInt(lastTwo);
		i = (i <= lowerbound) ? 2000+i : 1900+i;
		return date.substring(0, date.length() - 2) + i;
	}

	public static void main(String[] args) throws IOException, ParseException {

		InputPreprocessor ip = new InputPreprocessor();
		ip.process(new File(args[1]), args[2]);

	}

}
