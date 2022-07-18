package lphybeast;

/**
 * @author Walter Xie
 */
public class LPhyScripts {

    public static String simpleCoal = """
            data {
              L = 200;
              taxa = taxa(names=1:%s);
            }
            model {
              Θ ~ LogNormal(meanlog=3.0, sdlog=1.0);
              ψ ~ Coalescent(theta=Θ, taxa=taxa);
              D ~ PhyloCTMC(tree=ψ, L=L, Q=jukesCantor());
            }""";

    public static String relaxClock = """
            λ ~ LogNormal(meanlog=3.0, sdlog=1.0);
            ψ ~ Yule(lambda=λ, n=%s);
            branchRates ~ LogNormal(meanlog=-0.25, sdlog=0.5, replicates=(2*%s-2));
            D ~ PhyloCTMC(L=200, Q=jukesCantor(), tree=ψ, branchRates=branchRates);""";
}
