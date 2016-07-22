package edu.ucdavis.pathinfo.ehrc;

import java.text.SimpleDateFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Date: 2/11/14
 *
 * @author: Michael Resendez
 */
public class StringProcessor {


	protected static Pattern codePattern = Pattern.compile(" \\w{0,3}\\d.{1,}");
	protected static String [] removableSuffixPatterns = new String[] { " MD$", " JR$", " SR$" };


	public static String removePunctuation(String input) {
		return input.replaceAll("\\.", "").replaceAll(", ", " ").replaceAll("\\s{2,}", " ");
	}

	public static String removeSpaces(String input) {
		return input.replaceAll("\\s{2,}", "");
	}

	public static String removeTitles(String input) {

		for (String pattern : removableSuffixPatterns) {
			input = input.replaceAll(pattern, "");
		}

		return input;
	}

	public static String removeEmptyDash(String input) {

		if (input.matches("^-+$")) {
			return "";
		}

		return input;
	}

	public static String [] normLastName(String lastName) {

		if (lastName == null || "-".equals(lastName)) return new String[] { "", "", "" };

		String normalizedLastName = compressString(lastName);
		String[] surnames = compressLastName(lastName).split("[ -]+");

		String surname1 = norm(surnames[0]);
		String surname2 = "";
		if (surnames.length > 1) {
			surname2 = norm(surnames[1]);
		}

		return new String[] { normalizedLastName, surname1, surname2 };
	}

	public static String compressString(String string) {
		return norm(string).replace(" ", "").replace("-", "");
	}

	public static String compressLastName(String lastName) {

		return lastName
				.replaceAll("\\b\\s*SAN\\s+", "SAN")
				.replaceAll("\\b\\s*DOS\\s+", "DOS")
				.replaceAll("\\b\\s*DEL\\s+", "DEL")
				.replaceAll("\\b\\s*DE\\s+LA\\s+", "DELA")
				.replaceAll("\\b\\s*DE\\s+", "DE")
				.replaceAll("\\b\\s*DU\\s+", "DU")
				.replaceAll("\\b\\s*DI\\s+", "DI")
				.replaceAll("\\b\\s*DAL\\s+", "DAL")
				.replaceAll("\\b\\s*MAC\\s+", "MAC")
				.replaceAll("\\b\\s*MC\\s+", "MC")
				.replaceAll("\\b\\s*ST\\s+", "ST")
				.replaceAll("\\b\\s*MT\\s+", "MT")
				.replaceAll("\\b\\s*VON\\s+", "VON")
				.replaceAll("\\b\\s*VAN\\s+", "VAN")
				.replaceAll("\\b\\s*CON\\s+", "CON")
				.replaceAll("\\b\\s*LE\\s+", "LE")
				.replaceAll("\\b\\s*O\\s+", "O");

	}

	public static String norm(String input) {
		if (input == null) return "";
		return removeSpaces(removeTitles(removePunctuation(removeEmptyDash(removeCodes(input.toUpperCase())))).trim());
	}

	private static String removeCodes(String string) {

		Matcher m = codePattern.matcher(string);
		if (m.find()) {
			return m.replaceFirst("");
		}

		return string;

	}

	public static String [] normName(String first, String middle, String last) {

		String[] normalizedName = new String[3];

		normalizedName[0] = norm(first);
		normalizedName[1] = norm(middle);
		normalizedName[2] = norm(last).replaceAll(" ", "");

		if (normalizedName[1].length() == 0) {
			String[] fns = normalizedName[0].split(" ");
			if (fns.length > 1) {
				normalizedName[0] = fns[0];
				normalizedName[1] = fns[1];
			}
		}

		return normalizedName;
	}

}
