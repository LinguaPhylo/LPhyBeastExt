package lphybeast;

import org.junit.Before;
import org.junit.Test;
import picocli.CommandLine;

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

    LPhyBEAST lPhyBEAST;

    @Before
    public void setUp() throws Exception {
        lPhyBEAST = new LPhyBEAST();
    }

    @Test
    public void testSimpleCoalescent() {
        System.out.println(simpleCoal);
        String xml = null;
        try {
            xml = lPhyBEAST.lphyToXML(simpleCoal, "simpleCoal", -1, 0);
        } catch (CommandLine.PicocliException e) {
            e.printStackTrace();
        }

        TestUtils.assertXML(xml, ntaxa);

        assertTrue("Theta prior",  xml.contains("<distribution id=\"Theta.prior\"") &&
                xml.contains("spec=\"beast.math.distributions.LogNormalDistributionModel\"") &&
                xml.contains("name=\"M\">3.0</parameter>") && xml.contains("name=\"S\">1.0</parameter>") );

    }


}