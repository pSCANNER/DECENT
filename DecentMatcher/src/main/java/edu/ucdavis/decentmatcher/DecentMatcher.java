package edu.ucdavis.decentmatcher;

/**
 *
 *
 */
import com.google.gson.Gson;

import edu.ualr.oyster.core.OysterServiceMain;
import edu.ucdavis.ehrc.preprocessing.DataPreprocessor;
import edu.ucdavis.ehrc.preprocessing.DataPreprocessor.DOBEmptyException;
import edu.ucdavis.ehrc.preprocessing.DataPreprocessor.FirstNameEmptyException;
import edu.ucdavis.ehrc.preprocessing.DataPreprocessor.LastNameEmptyException;

import java.util.List;
import java.util.ArrayList;
import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.FileWriter;
import java.io.IOException;

public class DecentMatcher
{
    private static final Gson gson = new Gson();
    public static void main( String[] args ) throws FirstNameEmptyException, LastNameEmptyException, DOBEmptyException
    {
        OysterServiceMain oysterServiceMain = new OysterServiceMain("src/main/resources/matching/TestIdentityRunScript.xml");
        String edrsFileName = "src/main/resources/matching/edrs.txt";

        final List<String> edrsRecords = readFile(edrsFileName);

        final String[] lines = edrsRecords.toArray(new String[edrsRecords.size()]);

        //open report file to write into

        PrintWriter pw = null;
        FileWriter writer;
        try {
            File file = new File("src/main/resources/results/DecEntMatches");
            FileWriter fw = new FileWriter(file, true);
            pw = new PrintWriter(fw);

            writer = new FileWriter(file, true);
            PrintWriter printer = new PrintWriter(writer);

            for (String eachLine: lines) {
                pw.println(eachLine + "\n");

                DecentMatchRequestObject matchRequestObject = gson.fromJson(eachLine, DecentMatchRequestObject.class);

                String valueLine = new DataPreprocessor().processRequestRecord(
                        matchRequestObject.getFirstName(),
                        matchRequestObject.getMiddleName(),
                        matchRequestObject.getLastName(),
                        matchRequestObject.getAkaFirst(),
                        matchRequestObject.getAkaMiddle(),
                        matchRequestObject.getAkaLast(),
                        matchRequestObject.getDob(),
                        matchRequestObject.getGender().toString());

                String result = oysterServiceMain.attemptMatch(valueLine);

                pw.println("Result Returned:");
                pw.println(result);
            }

            printer.close();
        }

        catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            if (pw != null) {
                pw.close();
            }
        }
    }
    private static List<String> readFile(String filename)
    {
        List<String> records = new ArrayList<String>();
        try
        {
            BufferedReader reader = new BufferedReader(new FileReader(filename));
            String line;
            while ((line = reader.readLine()) != null)
            {
                records.add(line);
            }
            reader.close();
        }
        catch (Exception e)
        {
            System.err.format("Exception occurred trying to read '%s'.", filename);
            e.printStackTrace();
            records.add("");
        }
        return records;
    }
}


