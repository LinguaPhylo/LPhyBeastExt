package lphybeast.tobeast.generators;

import beast.core.BEASTInterface;
import beast.math.distributions.ParametricDistribution;
import beast.math.distributions.Poisson;
import beast.math.distributions.Prior;
import lphy.core.distributions.RandomBooleanArray;
import lphybeast.BEASTContext;
import lphybeast.GeneratorToBEAST;

public class RandomBooleanArrayToBEAST implements GeneratorToBEAST<RandomBooleanArray, Prior> {
    @Override
    public Prior generatorToBEAST(RandomBooleanArray generator, BEASTInterface value, BEASTContext context) {


        ParametricDistribution distr = new Poisson();

        beast.core.Function x = new beast.core.util.Sum();




        return BEASTContext.createPrior(distr, x);
    }

    @Override
    public Class<RandomBooleanArray> getGeneratorClass() {
        return RandomBooleanArray.class;
    }

    @Override
    public Class<Prior> getBEASTClass() {
        return Prior.class;
    }
}
