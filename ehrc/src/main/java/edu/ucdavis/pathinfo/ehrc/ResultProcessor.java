package edu.ucdavis.pathinfo.ehrc;

import com.Ostermiller.util.CSVParser;
import com.Ostermiller.util.CSVPrinter;
import com.Ostermiller.util.LabeledCSVParser;
import edu.ualr.oyster.utilities.OysterEditDistance;

import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static edu.ucdavis.pathinfo.ehrc.StringProcessor.normName;

/**
 * Date: 2/5/14
 *
 * @author: Michael Resendez
 */
public class ResultProcessor {

	private HashMap<Integer, String> ruMap = new HashMap<Integer, String>();
	private HashMap<Integer, String[]> oReferenceMap = new HashMap<Integer, String[]>();
	private HashMap<Integer, String[]> oInputMap = new HashMap<Integer, String[]>();

	HashMap<String, Integer> rulePrecedence = new HashMap<String, Integer>();

	public void process(File[] assertFiles, File[] identFiles, File reference, File inputs, File oInput, File oReference, File rulesFile, String outputName) throws IOException, ParseException {

		int matchCount = 0;
		int unmatchedCount = 0;
		int totalCount = 0;
		float percentage = 0.0f;

		{
			BufferedReader reader = new BufferedReader(new FileReader(rulesFile));
			String line = null;
			Pattern p = Pattern.compile("Rule\\s+Ident\\s*=\\s*\"([^\"]+)\""); //<Rule Ident="fnlndob_X">
			while ((line = reader.readLine()) != null) {
				Matcher m = p.matcher(line);
				if (m.find()) {
					String key = m.group(1).intern();
					rulePrecedence.put(key, rulePrecedence.size());
				}
			}
			rulePrecedence.put("[@]", rulePrecedence.size() + 1);
			reader.close();
		}

		int totalInputLines = 0;
		{
			LabeledCSVParser oInputParser = new LabeledCSVParser(new CSVParser(new BufferedReader(new FileReader(oInput))));
			oInputParser.changeDelimiter(',');
			System.out.println("reading original input file..." + oInput.getCanonicalPath());
			int lineCounter = 0;
			String [] aLine = null;
			while ((aLine = oInputParser.getLine()) != null) {
				lineCounter++;
				oInputMap.put(lineCounter, aLine);
			}
			oInputParser.close();
			totalInputLines = lineCounter;
		}

		{
			LabeledCSVParser oReferenceParser = new LabeledCSVParser(new CSVParser(new BufferedReader(new FileReader(oReference))));
			oReferenceParser.changeDelimiter('|');
			System.out.println("reading original reference file..." + oReference.getCanonicalPath());
			String [] aLine = null;
			while ((aLine = oReferenceParser.getLine()) != null) {
				oReferenceMap.put(Integer.parseInt(oReferenceParser.getValueByLabel("EDRSID")), aLine);
			}
			oReferenceParser.close();
		}

		LabeledCSVParser oReferenceParser = new LabeledCSVParser(new CSVParser(new BufferedReader(new FileReader(oReference))));
		oReferenceParser.changeDelimiter('\t');

		File unmatchedFile = new File(String.format("/Volumes/EHRC/%s_unmatched_report.txt", outputName));
		File matchedFile = new File(String.format("/Volumes/EHRC/%s_matched_report.txt", outputName));

		BufferedWriter unmatchedWriter = new BufferedWriter(new FileWriter(unmatchedFile));
		BufferedWriter matchedWriter = new BufferedWriter(new FileWriter(matchedFile));

		for (File assertFile : assertFiles) {
			System.out.println(assertFile.getCanonicalPath());
		}

		for (File identFile : identFiles) {
			System.out.println(identFile.getCanonicalPath());
		}

		System.out.println(reference.getCanonicalPath());
		System.out.println(inputs.getCanonicalPath());

		HashMap<String, Integer> asMap = new HashMap<String, Integer>();
		int assertionLineCount = 0;
		for (File assertFile : assertFiles) {
			//RefID	OysterID	Rule
			LabeledCSVParser asP = new LabeledCSVParser(new CSVParser(new BufferedReader(new FileReader(assertFile))));
			asP.changeDelimiter('\t');
			System.out.println("reading assertions file..." + assertFile.getCanonicalPath());
			while (asP.getLine() != null) {

				String oysterId =  asP.getValueByLabel("OysterID");
				if (asMap.containsKey(oysterId)) throw new RuntimeException(String.format("Duplicate oysterId '%s' in file '%s'", oysterId, assertFile.getCanonicalPath()));

				asMap.put(oysterId, Integer.parseInt(asP.getValueByLabel("RefID").split("\\.")[1]));

				assertionLineCount++;
			}
			asP.close();
		}

		HashMap<Integer, String> idMap = new HashMap<Integer, String>();
		TreeSet<Integer> sortedIds = new TreeSet<Integer>();
		ruMap = new HashMap<Integer, String>();

		for (File identFile : identFiles) {
			//RefID	OysterID	Rule
			LabeledCSVParser idP = new LabeledCSVParser(new CSVParser(new BufferedReader(new FileReader(identFile))));
			idP.changeDelimiter('\t');
			System.out.println("reading identities file..." + identFile.getCanonicalPath());
			while (idP.getLine() != null) {

				String oysterId =  idP.getValueByLabel("OysterID");
				Integer refID = Integer.parseInt(idP.getValueByLabel("RefID").split("\\.")[1]);
				sortedIds.add(refID);

				String rule = idP.getValueByLabel("Rule").intern();
				String previousRule = ruMap.get(refID);


				if ("[@]".equals(rule)) continue;

				if (previousRule != null) { //duplicate rule
					System.out.println(String.format("Duplicate matching rule found. OysterID '%s', Rule1 '%s', Rule2 '%s' in file '%s'",
							oysterId, previousRule, rule, identFile.getCanonicalPath()));

					if (rScore(previousRule) <= rScore(rule)) continue;

				}

				idMap.put(refID, oysterId);
				ruMap.put(refID, rule);

			}
			idP.close();
		}

		HashMap<Integer, String[]> reMap = new HashMap<Integer, String[]>();
		//EDRSID,FirstName,MiddleName,LastName,DOB,Gender
		LabeledCSVParser reP = new LabeledCSVParser(new CSVParser(new BufferedReader(new FileReader(reference))));
		System.out.println("reading reference file..." + reference.getCanonicalPath());
		String [] line = null;
		while ((line = reP.getLine()) != null) {
			reMap.put(Integer.parseInt(reP.getValueByLabel("EDRSID")), line);
		}

		//FileID,FirstName,MiddleName,LastName,DOB,Gender
		HashMap<Integer, String[]> inMap = new HashMap<Integer, String[]>();
		LabeledCSVParser inP = new LabeledCSVParser(new CSVParser(new BufferedReader(new FileReader(inputs))));
		System.out.println("reading inputs file..." + inputs.getCanonicalPath());
		line = null;
		while ((line = inP.getLine()) != null) {
			inMap.put(Integer.parseInt(inP.getValueByLabel("FileID")), line);
		}

		System.out.println("processing matches...");

		totalCount = sortedIds.size();
		for (Integer sortedId : sortedIds) {

			Integer inId = asMap.get(idMap.get(sortedId));

			String [] inputA = inMap.get(sortedId);
			if (inId != null) {

				String qId = idMap.get(sortedId);
				Integer refId = asMap.get(qId);
				String[] refA = reMap.get(refId);
				handleMatched(inputA, refA, matchedWriter);
				matchCount++;
			} else {

				handleUnmatched(inMap.get(sortedId), unmatchedWriter);
				unmatchedCount++;
			}
		}

		reP.close();
		inP.close();

		HashMap<String, Integer> ruleCounter = new HashMap<String, Integer>();
		for (String rule : ruMap.values()) {

			if (!ruleCounter.containsKey(rule)) ruleCounter.put(rule, 0);
			ruleCounter.put(rule, ruleCounter.get(rule) + 1);

		}

		ruleCounter.remove("[@]");

		matchedWriter.write("\n");

		matchedWriter.write("Matching rules used:");
		float percentTotals = 0.0f;
		int totalCounter = 0;
		for (String rule : sortByComparator(ruleCounter, false).keySet()) {
			float percent = (ruleCounter.get(rule) / (float) totalCount) * 100;
			int count = ruleCounter.get(rule);
			percentTotals += percent;
			totalCounter += count;
			matchedWriter.write(String.format("\n%30s %4d [%4d] times %05.2f%% [%05.2f%%]", rule, count, totalCounter, percent, percentTotals));
		}

		matchedWriter.close();
		unmatchedWriter.close();

		System.out.println("-----------------");


		System.out.println(String.format("%d input lines", totalInputLines));
		System.out.println(String.format("%d records", totalCount));
		System.out.println(String.format("%d records matched", matchCount));
		System.out.println(String.format("%d records unmatched", unmatchedCount));
		percentage = matchCount / (float) totalCount;

		System.out.println(String.format("\n%.2f%% records matched (%d / %d)", percentage*100, matchCount, totalCount));

	}

	private int rScore(String key) {
		for (String s : rulePrecedence.keySet()) {
			if (s.equals(key)) return rulePrecedence.get(s);
		}
		return rulePrecedence.size();
	}

	private static Map<String, Integer> sortByComparator(Map<String, Integer> unsortMap, final boolean order)
	{

		List<Map.Entry<String, Integer>> list = new LinkedList<Map.Entry<String, Integer>>(unsortMap.entrySet());

		// Sorting the list based on values
		Collections.sort(list, new Comparator<Map.Entry<String, Integer>>()
		{
			public int compare(Map.Entry<String, Integer> o1,
			                   Map.Entry<String, Integer> o2)
			{
				if (order)
				{
					return o1.getValue().compareTo(o2.getValue());
				}
				else
				{
					return o2.getValue().compareTo(o1.getValue());

				}
			}
		});

		// Maintaining insertion order with the help of LinkedList
		Map<String, Integer> sortedMap = new LinkedHashMap<String, Integer>();
		for (Map.Entry<String, Integer> entry : list)
		{
			sortedMap.put(entry.getKey(), entry.getValue());
		}

		return sortedMap;
	}

	private void handleUnmatched(String[] m, BufferedWriter writer) {
		int [] lens = new int[] {
				7,
				len(m, m, 1), len(m, m, 2), len(m, m, 3), len(m, m, 4), len(m, m, 5), len(m, m, 6), len(m, m, 7), len(m, m, 8), len(m, m, 9), len(m, m, 10)
		};

		String format = String.format("%%%ds, %%%ds, %%%ds, %%%ds, %%%ds, %%%ds, %%%ds, %%%ds, %%%ds, %%%ds, %%%ds",
				lens[0], lens[1], lens[2], lens[3], lens[4], lens[5], lens[6], lens[7], lens[8], lens[9], lens[10]);

		String outputLine1 = String.format(format, m[0], fld(m[1]), fld(m[2]), fld(m[3]), fld(m[4]),
				fld(m[5]), fld(m[6]), fld(m[7]), fld(m[8]), fld(m[9]), fld(m[10]));

		try {
			writer.write(outputLine1 + "\n");

			String [] aLine = oInputMap.get(Integer.parseInt(m[0]));
			for (int i = 0; i < aLine.length; i++) {
				writer.write(aLine[i]);
				if (i < aLine.length - 1) writer.write(',');
			}

			writer.write("\n\n");

		} catch (IOException e) {
			System.out.println(e);
			throw new RuntimeException(e);
		}


	}

	protected int len(String[] m1, String[] m2, int index) {
		return Math.max(1, Math.max(fld(m1[index]).length(), fld(m2[index]).length()));
	}

	protected String fld(String value) {
		if (value == null || value.length() == 0) return "<BLANK>";
		return value;
	}

	private void handleMatched(String[] m, String[] m2, BufferedWriter writer) throws ParseException {

		int [] lens = new int[] {
				7,
				len(m, m2, 1), len(m, m2, 2), len(m, m2, 3), len(m, m2, 4), len(m, m2, 5), len(m, m2, 6), len(m, m2, 7), len(m, m2, 8), len(m, m2, 9), len(m, m2, 10)
		};

		String format =  String.format("%%%ds, %%%ds, %%%ds, %%%ds, %%%ds, %%%ds, %%%ds, %%%ds, %%%ds, %%%ds, %%%ds",
				lens[0], lens[1], lens[2], lens[3], lens[4], lens[5], lens[6], lens[7], lens[8], lens[9], lens[10]);
		String format2 = String.format("%%%ds  %%%ds  %%%ds  %%%ds  %%%ds  %%%ds  %%%ds  %%%ds  %%%ds  %%%ds  %%%ds",
				lens[0], lens[1], lens[2], lens[3], lens[4], lens[5], lens[6], lens[7], lens[8], lens[9], lens[10]);

		String outputLine1 = String.format(format, m[0], fld(m[1]), fld(m[2]), fld(m[3]), fld(m[4]), fld(m[5]), fld(m[6]), fld(m[7]), fld(m[8]), fld(m[9]), fld(m[10]));
		String outputLine2 = String.format(format, m2[0],fld(m2[1]), fld(m2[2]), fld(m2[3]), fld(m2[4]), fld(m2[5]), fld(m2[6]), fld(m2[7]), fld(m2[8]), fld(m2[9]), fld(m2[10]));

		try {
			for (int i = 0; i < 80; i++) writer.write('-');

			writer.write(String.format("\n %6s matched to %s with rule %s\n", m[0], m2[0], ruMap.get(Integer.parseInt(m[0]))));
			writer.write(outputLine1 + "\n");
			writer.write(outputLine2 + "\n");

			float [] ledValues = new float[11];

			boolean hasLED = false;
			for (int x = 0; x < lens[0] + 2; x++) writer.write(" ");
			for (int i = 1; i <= 10; i++) {
				char token = (m[i].equals(m2[i])) ? ' ' : '^';
				for (int x = 0; x < lens[i]; x++) writer.write(token);
				for (int x = 0; x < 2; x++) writer.write(' ');
				if (!m[i].equals(m2[i])) {
					OysterEditDistance oed = new OysterEditDistance();
					int distance = oed.computeDistance(m[i], m2[i]);
					ledValues[i] = oed.computeNormalizedScore();
					hasLED = true;
				} else {
					ledValues[i] = -1;
				}
			}

			if (hasLED) {
				String ledLine = String.format(format2, "LED",
						(ledValues[1] > -1) ? String.format("%.2f", ledValues[1]) : "",
						(ledValues[2] > -1) ? String.format("%.2f", ledValues[2]) : "",
						(ledValues[3] > -1) ? String.format("%.2f", ledValues[3]) : "",
						(ledValues[4] > -1) ? String.format("%.2f", ledValues[4]) : "",
						(ledValues[5] > -1) ? String.format("%.2f", ledValues[5]) : "",
						(ledValues[6] > -1) ? String.format("%.2f", ledValues[6]) : "",
						(ledValues[7] > -1) ? String.format("%.2f", ledValues[7]) : "",
						(ledValues[8] > -1) ? String.format("%.2f", ledValues[8]) : "",
						(ledValues[9] > -1) ? String.format("%.2f", ledValues[9]) : "",
						(ledValues[10] > -1) ? String.format("%.2f", ledValues[10]) : "");
				writer.write("\n" + ledLine);
			}
			writer.write("\nRaw: \n\nEHR  :");

			String [] aLine1 = oInputMap.get(Integer.parseInt(m[0]));
			for (int i = 0; i < aLine1.length; i++) {
				writer.write(aLine1[i]);
				if (i < aLine1.length - 1) writer.write(',');
			}
			writer.write("\nEDRS :");
			String [] aLine2 = oReferenceMap.get(Integer.parseInt(m2[0]));
			for (int i = 1; i < aLine2.length; i++) {
				writer.write(aLine2[i]);
				if (i < aLine2.length - 1) writer.write(',');
			}

			{
				String date1 = InputPreprocessor.
						outDate.format(InputPreprocessor.
						inDate2.parse(InputPreprocessor.padDate(aLine1[12], 99)));



				String city1 = aLine1[7];
				String gender1 = aLine1[4];

				String gender2 = aLine2[5];
				String date2 = aLine2[10];
				String city2 = aLine2[9];

				writer.write("\n\nValidations:");
				if (gender1.charAt(0) != gender2.charAt(0)) {
					writer.write(String.format("\nGenders do not match: %s != %s", gender1, gender2));
				} else {
					writer.write(String.format("\nGenders match: %s", gender1));
				}

				if (!date1.equals(date2)) {
					writer.write(String.format("\nDeath dates do not match: %s != %s", date1, date2));
				} else {
					writer.write(String.format("\nDeath dates match: %s", date1));
				}

				if (!city1.equals(city2)) {
					writer.write(String.format("\nCities do not match: %s != %s", city1, city2));
				} else {
					writer.write(String.format("\nCities match: %s", city1));
				}


			}

			writer.write("\n\n");

		} catch (IOException e) {
			throw new RuntimeException(e);
		}

	}

	public static void main(String[] args) throws IOException, ParseException {

		ResultProcessor ip = new ResultProcessor();

		String [] years = args[0].split(",");
		File [] assertFiles = new File[years.length];
		File [] identFiles = new File[years.length];

		for (int i = 0; i < years.length; i++) {
			assertFiles[i] = new File(String.format(args[1], years[i]));
			identFiles[i] = new File(String.format(args[2], years[i]));
		}

		ip.process(assertFiles, identFiles, new File(args[3]), new File(args[4]), new File(args[5]), new File(args[6]), new File(args[7]), args[8]);

	}

}
