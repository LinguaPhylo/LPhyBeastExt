package lphybeast.tobeast.generators;

import beast.core.BEASTInterface;
import beast.core.parameter.CompoundRealParameter;
import feast.function.Slice;
import lphy.core.functions.SliceDoubleArray;
import lphy.graphicalModel.Value;
import lphybeast.BEASTContext;
import lphybeast.GeneratorToBEAST;

public class SliceDoubleArrayToBEAST implements GeneratorToBEAST<SliceDoubleArray, Slice> {
    @Override
    public Slice generatorToBEAST(SliceDoubleArray slice, BEASTInterface value, BEASTContext context) {

        Integer start = slice.start().value();
        Integer end =  slice.end().value();
        Integer count = end - start + 1;

        Slice feastSlice = new Slice();
        feastSlice.setInputValue("arg", context.getBEASTObject(slice.array()));
        feastSlice.setInputValue("index", start);
        if (count != 1) feastSlice.setInputValue("count", count);
        feastSlice.initAndValidate();
        return feastSlice;
    }

    @Override
    public void modifyBEASTValues(SliceDoubleArray generator, BEASTInterface value, BEASTContext context) {

        Value lphyValue = (Value)context.getGraphicalModelNode(value);
        BEASTInterface slicedParameter = context.getBEASTObject(generator.array());

        if (slicedParameter instanceof CompoundRealParameter && generator.size() == 1) {
            CompoundRealParameter compoundRealParameter = (CompoundRealParameter)slicedParameter;

            context.removeBEASTObject(value);
            context.putBEASTObject(lphyValue, compoundRealParameter.parameterListInput.get().get(generator.start().value()));
        } else {

            context.removeBEASTObject(value);
            context.putBEASTObject(lphyValue, generatorToBEAST(generator, value, context));
        }
    }

    @Override
    public Class<SliceDoubleArray> getGeneratorClass() { return SliceDoubleArray.class; }

    @Override
    public Class<Slice> getBEASTClass() {
        return Slice.class;
    }
}
