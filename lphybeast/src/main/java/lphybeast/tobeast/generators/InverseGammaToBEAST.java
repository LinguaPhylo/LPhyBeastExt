package lphybeast.tobeast.generators;

import beast.core.BEASTInterface;
import beast.core.parameter.RealParameter;
import beast.math.distributions.Prior;
import lphy.core.distributions.InverseGamma;
import lphybeast.BEASTContext;
import lphybeast.GeneratorToBEAST;

public class InverseGammaToBEAST implements GeneratorToBEAST<InverseGamma, Prior> {
    @Override
    public Prior generatorToBEAST(InverseGamma generator, BEASTInterface value, BEASTContext context) {
        beast.math.distributions.InverseGamma inverseGamma = new beast.math.distributions.InverseGamma();
        inverseGamma.setInputValue("alpha", context.getBEASTObject(generator.getAlpha()));
        inverseGamma.setInputValue("beta", context.getBEASTObject(generator.getBeta()));
        inverseGamma.initAndValidate();
        return BEASTContext.createPrior(inverseGamma, (RealParameter) value);
    }

    @Override
    public Class<InverseGamma> getGeneratorClass() {
        return InverseGamma.class;
    }

    @Override
    public Class<Prior> getBEASTClass() {
        return Prior.class;
    }
}
