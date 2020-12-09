package lphybeast.tobeast.generators;

import beast.core.BEASTInterface;
import beast.core.BEASTObject;
import beast.core.parameter.RealParameter;
import lphy.core.functions.VectorizedFunction;
import lphy.graphicalModel.DeterministicFunction;
import lphybeast.BEASTContext;
import lphybeast.GeneratorToBEAST;
import outercore.util.BEASTVector;

import java.util.ArrayList;
import java.util.List;

public class VectorizedFunctionToBEAST implements GeneratorToBEAST<VectorizedFunction, BEASTInterface> {
    @Override
    public BEASTInterface generatorToBEAST(VectorizedFunction generator, BEASTInterface value, BEASTContext context) {

        List<BEASTInterface> values = null;
        if (value instanceof BEASTVector) {
            values = ((BEASTVector)value).getObjectList();
        } else if (value instanceof RealParameter) {
            String idStr = value.getID() != null ? value.getID() : generator.getName();
            BEASTObject beastDummyObj = new BEASTObject() {
                @Override
                public void initAndValidate() {setID(idStr); }
            };
            return beastDummyObj;
        } else {
            throw new IllegalArgumentException("Expecting BEASTVector value from VectorizedDistribution");
        }

        List<DeterministicFunction> functionList = generator.getComponentFunctions();

        if (functionList.size() != values.size()) throw new IllegalArgumentException("Expecting value and component function list sizes to match!");

        List<BEASTInterface> beastGenerators = new ArrayList<>();
        for (int i = 0; i < functionList.size(); i++)  {
            DeterministicFunction function = functionList.get(i);
            GeneratorToBEAST toBEAST = context.getGeneratorToBEAST(function);

            BEASTInterface beastGenerator = toBEAST.generatorToBEAST(function, values.get(i), context);
            beastGenerators.add(beastGenerator);
            context.putBEASTObject(function, beastGenerator);
        }

        return new BEASTVector(beastGenerators);
    }

    @Override
    public Class<VectorizedFunction> getGeneratorClass() {
        return VectorizedFunction.class;
    }
}
