package lphybeast;

import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import static org.junit.Assert.*;


/**
 * Check the XML
 * @author Walter Xie
 */
public class LPhyToBEASTTest {

    private int ntaxa = 10;
    private final String simpleCoal = "data {\n" +
            "  L = 200;\n" +
            "  taxa = 1:" + ntaxa + ";\n" +
            "}\n" +
            "model {\n" +
            "  Θ ~ LogNormal(meanlog=3.0, sdlog=1.0);\n" +
            "  ψ ~ Coalescent(theta=Θ, taxa=taxa);\n" +
            "  D ~ PhyloCTMC(tree=ψ, L=L, Q=jukesCantor());\n" +
            "}";

    LPhyBEAST lPhyBEAST;

    @Before
    public void setUp() throws Exception {
        lPhyBEAST = new LPhyBEAST();
    }

    @Test
    public void testSimpleCoalescent() {
        Reader inputString = new StringReader(simpleCoal);
        BufferedReader reader = new BufferedReader(inputString);

        String xml = null;
        try {
            xml = lPhyBEAST.toBEASTXML(reader, "simpleCoal", -1);
        } catch (IOException e) {
            e.printStackTrace();
        }

        assertNotNull("IOException readLine()", xml); // IOException

        assertTrue("<beast></beast>",  xml.contains("<beast") && xml.contains("</beast>"));

        assertTrue("alignment tag",  xml.contains("<data") && xml.contains("id=\"D\""));

        String temp = xml.replace("<sequence", "");
        int occ = (xml.length() - temp.length()) / "<sequence".length();
        assertEquals(ntaxa,  occ);

        assertTrue("Theta prior",  xml.contains("<distribution id=\"Theta.prior\"") &&
                xml.contains("spec=\"beast.math.distributions.LogNormalDistributionModel\"") &&
                xml.contains("name=\"M\">3.0</parameter>") && xml.contains("name=\"S\">1.0</parameter>") );

    }
}