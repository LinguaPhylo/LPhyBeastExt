package lphybeast.tobeast.values;

import lphy.graphicalModel.Value;
import lphybeast.BEASTContext;
import lphybeast.BEASTFactory;
import lphybeast.ValueToBEAST;
import outercore.parameter.KeyParameter;
import outercore.parameter.KeyRealParameter;

public class NumberArrayValueToBEAST implements ValueToBEAST<Number[], KeyRealParameter> {

    @Override
    public KeyRealParameter valueToBEAST(Value<Number[]> value, BEASTContext context) {

//        KeyRealParameter parameter = new KeyRealParameter();
//        List<Double> values = new ArrayList<>();
//        for (int i = 0; i < value.value().length; i++) {
//            values.add(value.value()[i].doubleValue());
//        }
//        parameter.setInputValue("value", values);
//        parameter.setInputValue("dimension", values.size());

        KeyParameter parameter = BEASTFactory.createKeyParameter(value, null, null, true);
        if (!(parameter instanceof KeyRealParameter))
            throw new IllegalStateException("Expecting to create KeyRealParameter from " + value.getCanonicalId());

        return (KeyRealParameter) parameter;
    }

    @Override
    public Class getValueClass() {
        return Number[].class;
    }

    @Override
    public Class<KeyRealParameter> getBEASTClass() {
        return KeyRealParameter.class;
    }

}
