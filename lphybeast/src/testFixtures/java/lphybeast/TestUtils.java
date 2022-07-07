package lphybeast;

import lphy.system.UserDir;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

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

    public static Path getFileForResources(String fileName) {
        System.out.println("WD = " + UserDir.getUserDir());
        Path fPath = Paths.get("src","test", "resources", fileName);
        System.out.println("Input file = " + fPath.toAbsolutePath());
        return fPath;
    }

    public static String lphyScriptToBEASTXML(String lphyScript, String fileNameStem) {
        LPhyBeast lPhyBEAST = getLPhyBeast();

        String xml = null;
        try {
            System.out.println("\n" + lphyScript + "\n");

            xml = lPhyBEAST.lphyStrToXML(lphyScript, fileNameStem);
        } catch (IOException e) {
            e.printStackTrace();
        }
        assertNotNull(xml, "XML");
        assertXMLTags(xml);

        return xml;
    }

    private static void assertXMLTags(String xml) {
        assertTrue(xml.contains("<beast") && xml.contains("</beast>"), "<beast></beast>");

        assertTrue(xml.contains("<run") && xml.contains("id=\"MCMC\""), "MCMC tag" );
        assertTrue(xml.contains("<distribution") && xml.contains("id=\"posterior\""), "posterior tag");
        assertTrue(xml.contains("<distribution") && xml.contains("id=\"prior\""), "prior tag");
        assertTrue(xml.contains("<distribution") && xml.contains("id=\"likelihood\""), "likelihood tag");
        assertTrue(xml.contains("<operator"), "operator tag");
        assertTrue(xml.contains("<logger") && xml.contains("id=\"Logger\""), "logger tag");
    }

    public static void assertXMLNTaxa(String xml, int ntaxa) {
        assertTrue(xml.contains("<data") && xml.contains("</data>"), "alignment tag");

        // take the 1st <data> ... </data>
        String alig = xml.substring(xml.indexOf("<data"), xml.indexOf("</data>"));
        // count how many <sequence in the 1st pair of <data> ... </data>
        String temp = alig.replace("<sequence", "");
        int occ = (alig.length() - temp.length()) / "<sequence".length();
        assertEquals(ntaxa,  occ, "ntaxa");
    }
}
