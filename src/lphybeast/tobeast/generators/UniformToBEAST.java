package lphybeast.tobeast.generators;

import beast.core.BEASTInterface;
import beast.core.BEASTObject;
import beast.core.parameter.IntegerParameter;
import beast.core.parameter.RealParameter;
import beast.math.distributions.Prior;
import lphy.core.distributions.Normal;
import lphy.core.distributions.Uniform;
import lphybeast.BEASTContext;
import lphybeast.GeneratorToBEAST;

public class UniformToBEAST implements GeneratorToBEAST<Uniform, Prior> {
    @Override

    public Prior generatorToBEAST(Uniform generator, BEASTInterface value, BEASTContext context) {

        beast.math.distributions.Uniform uniform = new beast.math.distributions.Uniform();

        BEASTInterface lowerB = context.getBEASTObject(generator.getLower());
        BEASTInterface upperB = context.getBEASTObject(generator.getUpper());

        Double lower = Double.NEGATIVE_INFINITY;
        Double upper = Double.POSITIVE_INFINITY;

        if (lowerB instanceof RealParameter) {
            lower = ((RealParameter)lowerB).getValue();
        } else if (lowerB instanceof IntegerParameter) {
            lower = ((IntegerParameter)lowerB).getValue().doubleValue();
        } else {
            throw new IllegalArgumentException("BEAST2 can only have constants for lower and upper of Uniform distribution.");
        }

        if (upperB instanceof RealParameter) {
            upper = ((RealParameter)upperB).getValue();
        } else if (upperB instanceof IntegerParameter) {
            upper = ((IntegerParameter)upperB).getValue().doubleValue();
        } else {
            throw new IllegalArgumentException("BEAST2 can only have constants for lower and upper of Uniform distribution.");
        }

        uniform.setInputValue("lower", lower);
        uniform.setInputValue("upper", upper);
        uniform.initAndValidate();

        return BEASTContext.createPrior(uniform, (RealParameter)value);
    }

    @Override
    public Class<Uniform> getGeneratorClass() {
        return Uniform.class;
    }

    @Override
    public Class<Prior> getBEASTClass() {
        return Prior.class;
    }
}
