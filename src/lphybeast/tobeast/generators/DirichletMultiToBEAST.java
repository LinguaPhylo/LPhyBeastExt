package lphybeast.tobeast.generators;

import beast.core.BEASTInterface;
import beast.core.parameter.RealParameter;
import beast.math.distributions.Prior;
import lphy.core.distributions.Dirichlet;
import lphy.core.distributions.DirichletMulti;
import lphybeast.BEASTContext;
import lphybeast.GeneratorToBEAST;

public class DirichletMultiToBEAST implements GeneratorToBEAST<DirichletMulti, Prior> {

    @Override
    public Prior generatorToBEAST(DirichletMulti generator, BEASTInterface value, BEASTContext context) {
        beast.math.distributions.Dirichlet beastDirichlet = new beast.math.distributions.Dirichlet();
        beastDirichlet.setInputValue("alpha", context.getAsRealParameter(generator.getConcentration()));
        beastDirichlet.initAndValidate();

        //TODO

        return BEASTContext.createPrior(beastDirichlet, (RealParameter) value);
    }

    @Override
    public Class<DirichletMulti> getGeneratorClass() {
        return DirichletMulti.class;
    }

    @Override
    public Class<Prior> getBEASTClass() {
        return Prior.class;
    }
}
