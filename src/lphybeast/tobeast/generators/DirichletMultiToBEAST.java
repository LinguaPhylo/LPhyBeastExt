package lphybeast.tobeast.generators;

import beast.core.BEASTInterface;
import beast.core.Distribution;
import beast.core.parameter.RealParameter;
import beast.core.util.CompoundDistribution;
import beast.math.distributions.Prior;
import feast.function.Slice;
import lphy.core.distributions.Dirichlet;
import lphy.core.distributions.DirichletMulti;
import lphy.graphicalModel.Value;
import lphybeast.BEASTContext;
import lphybeast.GeneratorToBEAST;
import outercore.util.BEASTVector;

import java.util.ArrayList;
import java.util.List;

public class DirichletMultiToBEAST implements GeneratorToBEAST<DirichletMulti, CompoundDistribution> {

    @Override
    public CompoundDistribution generatorToBEAST(DirichletMulti generator, BEASTInterface beastValue, BEASTContext context) {

        if (!(beastValue instanceof BEASTVector)) throw new IllegalArgumentException("Expecting a beast vector!");
        BEASTVector vector = (BEASTVector)beastValue;

        Value<Double[][]> value = (Value<Double[][]>)context.getGraphicalModelNode(beastValue);

        CompoundDistribution compoundDistribution = new CompoundDistribution();

        RealParameter concentration = context.getAsRealParameter(generator.getConcentration());
        int size = concentration.getDimension();



        List<Prior> priorList = new ArrayList<>();
        for (int i = 0; i < value.value().length; i++) {
            beast.math.distributions.Dirichlet beastDirichlet = new beast.math.distributions.Dirichlet();
            beastDirichlet.setInputValue("alpha", concentration);
            beastDirichlet.initAndValidate();

            priorList.add(BEASTContext.createPrior(beastDirichlet, (RealParameter)vector.vectorInput.get().get(i)));
        }
        compoundDistribution.setInputValue("distribution", priorList);
        compoundDistribution.initAndValidate();

        return compoundDistribution;
    }

    @Override
    public Class<DirichletMulti> getGeneratorClass() {
        return DirichletMulti.class;
    }

    @Override
    public Class<CompoundDistribution> getBEASTClass() {
        return CompoundDistribution.class;
    }
}
