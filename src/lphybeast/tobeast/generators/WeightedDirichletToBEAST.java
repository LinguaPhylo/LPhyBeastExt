package lphybeast.tobeast.generators;

import beast.core.BEASTInterface;
import beast.core.parameter.RealParameter;
import beast.math.distributions.Prior;
import lphy.core.distributions.WeightedDirichlet;
import lphybeast.BEASTContext;
import lphybeast.GeneratorToBEAST;

public class WeightedDirichletToBEAST implements GeneratorToBEAST<WeightedDirichlet, Prior> {
    @Override
    public Prior generatorToBEAST(WeightedDirichlet generator, BEASTInterface value, BEASTContext context) {
        outercore.math.distributions.WeightedDirichlet beastDirichlet = new outercore.math.distributions.WeightedDirichlet();
        beastDirichlet.setInputValue("alpha", context.getBEASTObject(generator.getConcentration()));
        beastDirichlet.setInputValue("weights", context.getBEASTObject(generator.getWeights()));
        beastDirichlet.initAndValidate();

        return BEASTContext.createPrior(beastDirichlet, (RealParameter) value);


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
