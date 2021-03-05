package lphybeast.tobeast.generators;

import beast.core.BEASTInterface;
import beast.math.distributions.ParametricDistribution;
import beast.math.distributions.Prior;
import lphy.core.distributions.RandomBooleanArray;
import lphy.graphicalModel.Generator;
import lphybeast.BEASTContext;
import lphybeast.GeneratorToBEAST;

import java.util.Objects;

public class RandomBooleanArrayToBEAST implements GeneratorToBEAST<RandomBooleanArray, Prior> {
    @Override
    public Prior generatorToBEAST(RandomBooleanArray generator, BEASTInterface value, BEASTContext context) {

//        throw new UnsupportedOperationException("in dev");

        Generator poissonGenerator = Objects.requireNonNull(generator.getParams().get("hammingWeight")).getGenerator();

        Prior poissonPrior = (Prior) context.getBEASTObject(poissonGenerator);
        // need distr not prior
        context.removeBEASTObject(poissonPrior);

        beast.core.util.Sum x = new beast.core.util.Sum();
        x.setInputValue("arg", value);

        ParametricDistribution dist = poissonPrior.distInput.get();
        Prior prior = BEASTContext.createPrior(dist, x);
        prior.setID(value.getID() + ".prior");
        return prior;
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
