package lphybeast;


import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Check the XML
 * @author Walter Xie
 */
public class JCCoalToBEASTTest {

    @Test
    public void testSimpleCoalescent() {
        int ntaxa = 10;
        String simpleCoal = String.format(LPhyScripts.simpleCoal, ntaxa);
        String xml = TestUtils.lphyScriptToBEASTXML(simpleCoal, "simpleCoal");

        TestUtils.assertXMLNTaxa(xml, ntaxa);

        assertTrue(xml.contains("<distribution") && xml.contains("id=\"Theta.prior\"") && xml.contains("x=\"@Theta\"") &&
                xml.contains("spec=\"beast.math.distributions.LogNormalDistributionModel\"") &&
                xml.contains("name=\"M\">3.0</parameter>") && xml.contains("name=\"S\">1.0</parameter>"), "Theta prior" );

        assertTrue(xml.contains("<distribution") && xml.contains("id=\"Coalescent\""), "Coalescent" );
        assertTrue(xml.contains("<populationModel") && xml.contains("popSize=\"@Theta\""), "popSize" );
    }

}