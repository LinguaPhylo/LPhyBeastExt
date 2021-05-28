package lphybeast.tobeast.values;

import beast.core.parameter.Parameter;
import beast.core.parameter.RealParameter;
import lphy.graphicalModel.Value;
import lphybeast.BEASTContext;
import lphybeast.BEASTFactory;
import lphybeast.ValueToBEAST;

public class NumberArrayValueToBEAST implements ValueToBEAST<Number[], RealParameter> {

    @Override
    public RealParameter valueToBEAST(Value<Number[]> value, BEASTContext context) {

//        KeyRealParameter parameter = new KeyRealParameter();
//        List<Double> values = new ArrayList<>();
//        for (int i = 0; i < value.value().length; i++) {
//            values.add(value.value()[i].doubleValue());
//        }
//        parameter.setInputValue("value", values);
//        parameter.setInputValue("dimension", values.size());
        Parameter parameter = BEASTFactory.createKeyParameter(value, null, null, true);
        if (!(parameter instanceof RealParameter))
            throw new IllegalStateException("Expecting to create KeyRealParameter from " + value.getCanonicalId());

        return (RealParameter) parameter;
    }

    @Override
    public Class getValueClass() {
        return Number[].class;
    }

    @Override
    public Class<RealParameter> getBEASTClass() {
        return RealParameter.class;
    }

}
