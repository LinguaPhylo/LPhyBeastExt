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


}
