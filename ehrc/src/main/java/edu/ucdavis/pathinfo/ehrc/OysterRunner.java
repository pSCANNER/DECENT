package edu.ucdavis.pathinfo.ehrc;

import edu.ualr.oyster.core.OysterMain;

import java.io.IOException;
import java.text.ParseException;
import java.util.Arrays;

/**
 * Date: 2/21/14
 *
 * @author: Michael Resendez
 */
public class OysterRunner {

	public static void main(String[] args) throws IOException, ParseException {

		OysterMain.main(new String[] { args[0], args[1] });

		ResultProcessor.main(new String[] {args[2],args[3],args[4],args[5],args[6],args[7],args[8],args[9],args[10] });

	}

}
