package lphybeast;

import static org.junit.Assert.*;

/**
 * @author Walter Xie
 */
public class TestUtils {


    public static void assertXML(String xml, int ntaxa) {
        assertNotNull("IOException readLine()", xml); // IOException
        assertTrue("<beast></beast>",  xml.contains("<beast") && xml.contains("</beast>"));
        assertTrue("alignment tag",  xml.contains("<data") && xml.contains("id=\"D\""));

        String temp = xml.replace("<sequence", "");
        int occ = (xml.length() - temp.length()) / "<sequence".length();
        assertEquals(ntaxa,  occ);
    }

}
