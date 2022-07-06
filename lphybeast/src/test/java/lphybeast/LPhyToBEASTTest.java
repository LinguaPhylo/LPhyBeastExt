package lphybeast;

import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertNotNull;
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

    @Test
    public void testSimpleCoalescent() {
        System.out.println(simpleCoal);
        LPhyBeast lPhyBEAST = TestUtils.getLPhyBeast();

        String xml = null;
        try {
            xml = lPhyBEAST.lphyStrToXML(simpleCoal, "simpleCoal");
        } catch (IOException e) {
            e.printStackTrace();
        }
        assertNotNull("XML", xml);
        TestUtils.assertXMLTags(xml);
        TestUtils.assertXMLNTaxa(xml, ntaxa);

        assertTrue("Theta prior",  xml.contains("<distribution") && xml.contains("id=\"Theta.prior\"") &&
                xml.contains("x=\"@Theta\"") && xml.contains("spec=\"beast.math.distributions.LogNormalDistributionModel\"") &&
                xml.contains("name=\"M\">3.0</parameter>") && xml.contains("name=\"S\">1.0</parameter>") );

        assertTrue("Coalescent",  xml.contains("<distribution") && xml.contains("id=\"Coalescent\"") );
        assertTrue("popSize",  xml.contains("<populationModel") && xml.contains("popSize=\"@Theta\"") );
    }


}