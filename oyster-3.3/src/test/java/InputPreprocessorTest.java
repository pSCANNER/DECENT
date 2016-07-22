import edu.ucdavis.pathinfo.ehrc.InputPreprocessor;
import org.junit.Assert;
import org.junit.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Date: 2/11/14
 *
 * @author: Michael Resendez
 */
public class InputPreprocessorTest extends InputPreprocessor{

	@Test
	public void testDatePad() {

		Assert.assertEquals("03/03/2013", padDate("03/03/13", 15));
		Assert.assertEquals("03/03/2087", padDate("03/03/87", 99));

	}

	@Test
	public void testInputFormat() throws ParseException {

		String date1 = "7-Jun-49";
		String date2 = "5-Dec-13";

		String dateStr = "17-Jun-1975";
		SimpleDateFormat sut = new SimpleDateFormat("dd-MMM-yyyy");
		Date date = sut.parse(dateStr);
		Assert.assertEquals(sut.format(date), dateStr);

		Assert.assertEquals("06/07/1949", outDate.format(inDate1.parse(padDate(date1, 14))));
		Assert.assertEquals("12/05/2013", outDate.format(inDate1.parse(padDate(date2, 14))));

	}


}
