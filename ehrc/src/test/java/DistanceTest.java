import edu.ualr.oyster.utilities.Metaphone;
import edu.ualr.oyster.utilities.OysterEditDistance;
import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Date: 2/7/14
 *
 * @author: Michael Resendez
 */
public class DistanceTest {

	@Test
	public void testDistanceMetric() {

		showEditDistance("04/18/1912", "04/18/2012");
		showEditDistance("02/07/1955", "01/07/1955");
		showEditDistance("01/01/2014", "01/01/1914");
		showEditDistance("01/25/1914", "10/25/2014");
		showNameEditDistance("FRANCES", "FRANCISCA");
		showNameEditDistance("BENNETTE", "BENNETT");
		showNameEditDistance("WALT", "WALTER");
		showNameEditDistance("CAROLINE", "CAROLYN");
		showNameEditDistance("DEBRA", "DEBBIE");
		showNameEditDistance("10/21/1961", "10/21/1960");
		showNameEditDistance("ANN", "ANNE");
		showNameEditDistance("KE'YARI", "KE'YARI");

		String foo = "foo 9x";
		if (foo.contains("9")) {
			System.out.println("has 9");
		}
		Matcher m = Pattern.compile("cat").matcher("tje cat");
	}

	private void showEditDistance(String s1, String s2) {

		OysterEditDistance oed = new OysterEditDistance();
		int i = oed.computeDistance(s1, s2);
		float score = oed.computeNormalizedScore();
		System.out.println(String.format("%s <LED> %s : %d, %f", s1, s2, i, score));


	}

	private void showNameEditDistance(String s1, String s2) {

		showEditDistance(s1, s2);
		Metaphone meta = new Metaphone();
		System.out.println("checking metaphone");
		showEditDistance(meta.getMetaphone(s1), meta.getMetaphone(s2));

	}

}
