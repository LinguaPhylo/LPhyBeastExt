package lphybeast;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertTrue;


/**
 * Check the XML
 * @author Walter Xie
 */
public class LPhyToBEASTTest {

    private int ntaxa = 10;
    private final String simpleCoal = "data {\n" +
            "  L = 200;\n" +
            "  taxa = taxa(names=1:" + ntaxa + ");\n" +
            "}\n" +
            "model {\n" +
            "  Θ ~ LogNormal(meanlog=3.0, sdlog=1.0);\n" +
            "  ψ ~ Coalescent(theta=Θ, taxa=taxa);\n" +
            "  D ~ PhyloCTMC(tree=ψ, L=L, Q=jukesCantor());\n" +
            "}";

    LPhyBeast lPhyBEAST;

    @Before
    public void setUp() {
        lPhyBEAST = new LPhyBeast();
    }

    @Test
    public void testSimpleCoalescent() {
        System.out.println(simpleCoal);
        String xml = null;
        try {
            xml = lPhyBEAST.lphyStrToXML(simpleCoal, "simpleCoal");
        } catch (IOException e) {
            e.printStackTrace();
        }

        TestUtils.assertXML(xml, ntaxa);

        assertTrue("Theta prior",  xml.contains("<distribution id=\"Theta.prior\"") &&
                xml.contains("spec=\"beast.math.distributions.LogNormalDistributionModel\"") &&
                xml.contains("name=\"M\">3.0</parameter>") && xml.contains("name=\"S\">1.0</parameter>") );

    }


}