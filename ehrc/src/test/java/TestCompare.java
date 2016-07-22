import edu.ualr.oyster.core.OysterAttribute;
import edu.ualr.oyster.core.OysterAttributeComparator;
import org.junit.Assert;
import org.junit.Test;

/**
 * Date: 2/18/14
 *
 * @author: Michael Resendez
 */
public class TestCompare {

	@Test
	public void testCompare() {

		OysterAttribute a1 = new OysterAttribute();
		a1.setName("FirstName");
		a1.setAlgorithm("None");

		OysterAttribute a2 = new OysterAttribute();
		a2.setName("MiddleName");
		a2.setAlgorithm("None");

		OysterAttributeComparator comparator = new OysterAttributeComparator();
		System.out.println(comparator.compare(a1, a2));

	}

}
