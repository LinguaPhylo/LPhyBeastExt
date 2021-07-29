package lphybeast.tobeast.generators;

import beast.core.BEASTInterface;
import beast.core.Function;
import beast.math.distributions.Prior;
import lphy.core.distributions.WeightedDirichlet;
import lphybeast.BEASTContext;
import lphybeast.GeneratorToBEAST;

public class WeightedDirichletToBEAST implements GeneratorToBEAST<WeightedDirichlet, Prior> {
    @Override
    public Prior generatorToBEAST(WeightedDirichlet generator, BEASTInterface value, BEASTContext context) {

        beast.math.distributions.WeightedDirichlet beastDirichlet = new beast.math.distributions.WeightedDirichlet();
        beastDirichlet.setInputValue("alpha", context.getAsRealParameter(generator.getConcentration()));
        beastDirichlet.setInputValue("weights", context.getAsIntegerParameter(generator.getWeights()));
        beastDirichlet.initAndValidate();

        return BEASTContext.createPrior(beastDirichlet, (Function) value);
    }

    @Override
    public Class<WeightedDirichlet> getGeneratorClass() {
        return WeightedDirichlet.class;
    }

    @Override
    public Class<Prior> getBEASTClass() {
        return Prior.class;
    }
}
