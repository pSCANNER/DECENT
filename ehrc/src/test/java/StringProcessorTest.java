import edu.ucdavis.pathinfo.ehrc.StringProcessor;
import org.junit.Assert;
import org.junit.Test;

/**
 * Date: 2/11/14
 *
 * @author: Michael Resendez
 */
public class StringProcessorTest {

	@Test
	public void testPunct() {

		Assert.assertEquals(StringProcessor.removePunctuation("John M.D."), "John MD");

	}

	@Test
	public void testTitles() {

		Assert.assertEquals(StringProcessor.removeTitles("John MD"), "John");
		Assert.assertEquals(StringProcessor.removeTitles("John JR"), "John");
		Assert.assertEquals(StringProcessor.removeTitles("John SR"), "John");

	}

	@Test
	public void testNormName() {

		String[] names = StringProcessor.normName("john   boy", null, "walton");
		Assert.assertArrayEquals(names, new String[] {"JOHN", "BOY", "WALTON"});

		String[] names2 = StringProcessor.normName("billy boy", "", "walton");
		Assert.assertArrayEquals(names2, new String[] {"BILLY", "BOY", "WALTON"});

		String[] names3 = StringProcessor.normName("billy boy", "bad", "walton");
		Assert.assertArrayEquals(names3, new String[] {"BILLY BOY", "BAD", "WALTON"});

		String[] names4 = StringProcessor.normName("billy boy", "-", "walton");
		Assert.assertArrayEquals(names4, new String[] {"BILLY", "BOY", "WALTON"});

		String[] names5 = StringProcessor.normName("billy", "-", "walton");
		Assert.assertArrayEquals(names5, new String[] {"BILLY", "", "WALTON"});

		String[] names6 = StringProcessor.normName("billy", "--", "walton");
		Assert.assertArrayEquals(names6, new String[] {"BILLY", "", "WALTON"});


	}

	@Test
	public void testNormalizeLastName() {

		String [] names = new String[] { "LE BLANC-CANTERBURY",
				"VAN PATTEN",
				"DE LA BARRA",
				"DE LA O - ALEJO",
				"MOLINA DE TEJADA",
				"MEYER-VON GLASCOE",
				"TAG-VON STEIN",
				"MC MANUS",
				"VON ASTEN",
				"GILMORE - WELLINGTON",
				"FONSECA-GARCIA",
				"GALINDO-DE LA PAZ",
				"DE LUNA",
				"HERNANDEZ DE LARA",
				"DE LA ROSA",
				"MONTES DE OCA FLORES",
				"DE LA RIVA-VASQUEZ"
		};

		for (String name : names) {
			String[] output = StringProcessor.normLastName(name);

			System.out.println(name + " >> ");
			for (String s : output) {
				System.out.print("'" + s + "', ");
			}
			System.out.println();

		}

	}
}
