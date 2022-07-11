package mm.lphybeast.tobeast.generators;

import lphybeast.TestUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * Check the XML
 * @author Walter Xie
 */
public class LewisMKToBEASTTest {

    private String lewisMK = """
            Θ ~ LogNormal(meanlog=3.0, sdlog=1.0);
            ψ ~ Coalescent(n=%1$s, theta=Θ);
            Q = lewisMK(numStates=%2$s);
            D ~ PhyloCTMC(L=20, Q=Q, tree=ψ, dataType=standard(%2$s));""";

    @Test
    public void testLewisMK() {
        int ntaxa = 16;
        int nState = 3;
        String xml = TestUtils.lphyScriptToBEASTXML(String.format(lewisMK, ntaxa, nState), "lewisMK");

        TestUtils.assertXMLNTaxa(xml, ntaxa);

        assertTrue(xml.contains("<userDataType") && xml.contains("codeMap=\"0=0,1=1,2=2,") &&
                xml.contains("states=\"" + nState + "\""), "userDataType" );

        assertTrue(xml.contains("<substModel") && xml.contains("id=\"LewisMK\"") && xml.contains("spec=\"LewisMK\"") &&
                xml.contains("stateNumber=\"" + nState + "\""), "LewisMK substModel" );

        assertTrue(xml.contains("<distribution") && xml.contains("id=\"Theta.prior\"") && xml.contains("x=\"@Theta\"") &&
                xml.contains("spec=\"beast.math.distributions.LogNormalDistributionModel\"") &&
                xml.contains("name=\"M\">3.0</parameter>") && xml.contains("name=\"S\">1.0</parameter>"), "Theta prior" );

        assertTrue(xml.contains("<distribution") && xml.contains("id=\"Coalescent\""), "Coalescent" );
        assertTrue(xml.contains("<populationModel") && xml.contains("popSize=\"@Theta\""), "popSize" );
    }

}