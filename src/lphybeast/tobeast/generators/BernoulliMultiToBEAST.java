package lphybeast.tobeast.generators;

import beast.core.BEASTInterface;
import lphy.core.distributions.BernoulliMulti;
import lphybeast.BEASTContext;
import lphybeast.GeneratorToBEAST;
import outercore.math.distributions.BernoulliDistribution;

// Note: because there is a optional condition "minSuccesses",
// it cannot be replaced by {@link IID}.
public class BernoulliMultiToBEAST implements GeneratorToBEAST<BernoulliMulti, BernoulliDistribution> {
    @Override
    public BernoulliDistribution generatorToBEAST(BernoulliMulti generator, BEASTInterface value, BEASTContext context) {

        BernoulliDistribution bernoulliDistribution = new BernoulliDistribution();
        bernoulliDistribution.setInputValue("p", context.getBEASTObject(generator.getP()));
        bernoulliDistribution.setInputValue("parameter", value);
        bernoulliDistribution.setInputValue(generator.minSuccessesParamName, context.getBEASTObject(generator.getMinSuccesses()));
        bernoulliDistribution.initAndValidate();
        return bernoulliDistribution;
    }

    @Override
    public Class<BernoulliMulti> getGeneratorClass() {
        return BernoulliMulti.class;
    }

    @Override
    public Class<BernoulliDistribution> getBEASTClass() {
        return BernoulliDistribution.class;
    }
}
