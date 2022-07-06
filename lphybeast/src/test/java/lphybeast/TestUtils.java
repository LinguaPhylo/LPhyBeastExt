package lphybeast;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Walter Xie
 */
public class TestUtils {

    private static LPhyBeast lPhyBEAST;

    private TestUtils() { }

    public static LPhyBeast getLPhyBeast() {
        if(lPhyBEAST == null) {
            lPhyBEAST = new LPhyBeast();
        }
        return lPhyBEAST;
    }

    public static void assertXMLTags(String xml) {
        assertTrue("<beast></beast>",  xml.contains("<beast") && xml.contains("</beast>"));
        assertTrue("alignment tag",  xml.contains("<data") && xml.contains("id=\""));

        assertTrue("MCMC tag",  xml.contains("<run") && xml.contains("id=\"MCMC\""));
        assertTrue("posterior tag",  xml.contains("<distribution") && xml.contains("id=\"posterior\""));
        assertTrue("prior tag",  xml.contains("<distribution") && xml.contains("id=\"prior\""));
        assertTrue("likelihood tag",  xml.contains("<distribution") && xml.contains("id=\"likelihood\""));
        assertTrue("operator tag",  xml.contains("<operator") && xml.contains("id=\""));
        assertTrue("logger tag",  xml.contains("<logger") && xml.contains("id=\"Logger\""));
    }

    public static void assertXMLNTaxa(String xml, int ntaxa) {

        String alig = xml.substring(xml.indexOf("<data"), xml.indexOf("</data>"));
        // count how many <sequence in the 1st pair of <data> ... </data>
        String temp = alig.replace("<sequence", "");
        int occ = (alig.length() - temp.length()) / "<sequence".length();
        assertEquals("ntaxa", ntaxa,  occ);

    }
}
