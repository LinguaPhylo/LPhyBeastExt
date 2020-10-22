package lphybeast.tobeast.generators;

import beast.core.BEASTInterface;
import outercore.parameter.CompoundRealParameter;
import beast.core.parameter.RealParameter;
import beast.math.distributions.Prior;
import lphy.core.distributions.WeightedDirichlet;
import lphy.graphicalModel.Value;
import lphybeast.BEASTContext;
import lphybeast.GeneratorToBEAST;
import lphybeast.tobeast.values.ValueToParameter;

import java.util.ArrayList;
import java.util.List;

public class WeightedDirichletToBEAST implements GeneratorToBEAST<WeightedDirichlet, Prior> {
    @Override
    public Prior generatorToBEAST(WeightedDirichlet generator, BEASTInterface value, BEASTContext context) {
        outercore.math.distributions.WeightedDirichlet beastDirichlet = new outercore.math.distributions.WeightedDirichlet();
        beastDirichlet.setInputValue("alpha", context.getAsRealParameter(generator.getConcentration()));
        beastDirichlet.setInputValue("weights", context.getBEASTObject(generator.getWeights()));
        beastDirichlet.initAndValidate();

        return BEASTContext.createPrior(beastDirichlet, (RealParameter) value);

    }

    @Override
    public void modifyBEASTValues(WeightedDirichlet generator, BEASTInterface value, BEASTContext context) {

        CompoundRealParameter compoundRealParameter = new CompoundRealParameter();
        List<RealParameter> parameters = new ArrayList<>();

        RealParameter output = (RealParameter) value;
        Double[] values = output.getValues();

        for (int i = 0; i < values.length; i++) {
            parameters.add(context.createRealParameter(output.getID() + "." + i, values[i]));
        }
        compoundRealParameter.setInputValue("parameter", parameters);
        compoundRealParameter.initAndValidate();

        Value lphyValue = (Value) context.getGraphicalModelNode(value);

        ValueToParameter.setID(compoundRealParameter, lphyValue);

        context.removeBEASTObject(value);
        context.putBEASTObject(lphyValue, compoundRealParameter);
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
