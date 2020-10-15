package lphybeast.tobeast.generators;

import beast.core.BEASTInterface;
import beast.core.parameter.RealParameter;
import beast.math.distributions.Prior;
import lphy.core.distributions.InverseGammaMulti;
import lphybeast.BEASTContext;
import lphybeast.GeneratorToBEAST;

public class InverseGammaMultiToBEAST implements GeneratorToBEAST<InverseGammaMulti, Prior> {
    @Override
    public Prior generatorToBEAST(InverseGammaMulti generator, BEASTInterface value, BEASTContext context) {
        beast.math.distributions.InverseGamma inverseGamma = new beast.math.distributions.InverseGamma();
        inverseGamma.setInputValue("alpha", context.getBEASTObject(generator.getAlpha()));
        inverseGamma.setInputValue("beta", context.getBEASTObject(generator.getBeta()));
        inverseGamma.initAndValidate();
        return BEASTContext.createPrior(inverseGamma, (RealParameter) value);
    }

    @Override
    public Class<InverseGammaMulti> getGeneratorClass() {
        return InverseGammaMulti.class;
    }

    @Override
    public Class<Prior> getBEASTClass() {
        return Prior.class;
    }
}
