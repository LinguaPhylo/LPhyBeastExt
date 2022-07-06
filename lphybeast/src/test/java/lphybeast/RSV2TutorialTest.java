package lphybeast;

import org.junit.Before;
import org.junit.Test;

import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Time stamped data
 * @author Walter Xie
 */
public class RSV2TutorialTest {

    private final int ntaxa = 129;
    private Path fPath;

    @Before
    public void setUp() {
        fPath = TestUtils.getFileForResources("RSV2.nex");
    }

    @Test
    public void testRSV2() {
        String RSV2LPhy = String.format("""
                    data {
                       options = {ageDirection="forward", ageRegex="s(\\d+)$"};
                       D = readNexus(file="%s", options=options);
                       taxa = D.taxa();
                       codon = D.charset(["3-629\\3","1-629\\3", "2-629\\3"]);
                       L = codon.nchar();
                       n=length(codon); // 3 partitions
                     }
                     model {
                       κ ~ LogNormal(meanlog=1.0, sdlog=0.5, replicates=n);
                       π ~ Dirichlet(conc=[2.0,2.0,2.0,2.0], replicates=n);
                       r ~ WeightedDirichlet(conc=rep(element=1.0, times=n), weights=L);
                       μ ~ LogNormal(meanlog=-5.0, sdlog=1.25);
                       Θ ~ LogNormal(meanlog=3.0, sdlog=2.0);
                       ψ ~ Coalescent(taxa=taxa, theta=Θ);
                       codon ~ PhyloCTMC(L=L, Q=hky(kappa=κ, freq=π, meanRate=r), mu=μ, tree=ψ);
                     }""", fPath.toAbsolutePath());

        String xml = TestUtils.lphyScriptToBEASTXML(RSV2LPhy, "RSV2");

        TestUtils.assertXMLNTaxa(xml, ntaxa);

        // 3 alignments
        assertEquals("Codon alignment", 3, xml.split("spec=\"Alignment\"", -1).length - 1);

        assertTrue("Check parameters ID", xml.contains("id=\"kappa\"") &&
                xml.contains("id=\"pi_0\"") && xml.contains("id=\"pi_1\"") && xml.contains("id=\"pi_2\"") &&
                xml.contains("id=\"r_0\"") && xml.contains("id=\"r_1\"") && xml.contains("id=\"r_2\"") &&
                xml.contains("id=\"mu\"") && xml.contains("id=\"Theta\"") && xml.contains("id=\"psi\"") );

        assertTrue("TraitSet",  xml.contains("<trait") &&
                xml.contains("id=\"TraitSet\"") && xml.contains("traitname=\"date-backward\"") );
        assertTrue("Time",  xml.contains("BE8078s92=10.0") &&
                xml.contains("BE332s102=0.0") && xml.contains("USALongs56=46.0") );

        assertTrue("Theta prior",  xml.contains("<distribution") && xml.contains("id=\"Theta.prior\"") &&
                xml.contains("x=\"@Theta\"") && xml.contains("spec=\"beast.math.distributions.LogNormalDistributionModel\"") &&
                xml.contains("name=\"M\">3.0</parameter>") && xml.contains("name=\"S\">2.0</parameter>") );

        assertTrue("kappa prior",  xml.contains("x=\"@kappa\"") && xml.contains("id=\"kappa.prior\"") &&
                xml.contains("name=\"M\">1.0</parameter>") && xml.contains("name=\"S\">0.5</parameter>") );
        assertTrue("pi_2 prior",  xml.contains("x=\"@pi_2\"") && xml.contains("id=\"pi_2.prior\"") &&
                xml.contains("name=\"alpha\">2.0 2.0 2.0 2.0</parameter>") );

        assertTrue("Frequencies",  xml.contains("<frequencies") && xml.contains("spec=\"Frequencies\"") &&
                xml.contains("frequencies=\"@pi_1\"") );

        assertTrue("r.prior WeightedDirichlet",  xml.contains("id=\"WeightedDirichlet\"") &&
                xml.contains("<weights") && xml.contains("dimension=\"3\"") &&
                xml.contains("estimate=\"false\">209 210 210</weights>") );

        assertTrue("mu prior",  xml.contains("x=\"@kappa\"") &&
                xml.contains("name=\"M\">-5.0</parameter>") && xml.contains("name=\"S\">1.25</parameter>") );

        // 3 TreeLikelihood
        assertEquals("Tree Likelihood", 3,
                xml.split("ThreadedTreeLikelihood", -1).length - 1);

        // 5 ScaleOperator, incl. tree
        assertEquals("ScaleOperator", 5,
                xml.split("ScaleOperator", -1).length - 1);

        assertTrue("Tree Operator",  xml.contains("Exchange") &&
                xml.contains("SubtreeSlide") && xml.contains("WilsonBalding") );

        assertTrue("UpDownOperator",  xml.contains("UpDownOperator") &&
                xml.contains("<up") && xml.contains("<down") );
        // 4 DeltaExchangeOperator
        assertEquals("DeltaExchangeOperator", 4,
                xml.split("DeltaExchangeOperator", -1).length - 1);

        assertTrue("logger",  xml.contains("chainLength=\"1000000\"") &&
                xml.contains("logEvery=\"500\"") &&
                xml.contains("fileName=\"RSV2.log\"") && xml.contains("fileName=\"RSV2.trees\"") );
    }

}