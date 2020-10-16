package lphybeast.tobeast.values;

import lphy.graphicalModel.Value;
import lphybeast.BEASTContext;
import lphybeast.ValueToBEAST;
import outercore.parameter.KeyRealParameter;

import java.util.ArrayList;
import java.util.List;

public class NumberArrayValueToBEAST implements ValueToBEAST<Number[], KeyRealParameter> {

    @Override
    public KeyRealParameter valueToBEAST(Value<Number[]> value, BEASTContext context) {

        KeyRealParameter parameter = new KeyRealParameter();
        List<Double> values = new ArrayList<>();
        for (int i = 0; i < value.value().length; i++) {
            values.add(value.value()[i].doubleValue());
        }
        parameter.setInputValue("value", values);
        parameter.setInputValue("dimension", values.size());
        
        parameter.initAndValidate();
        ValueToParameter.setID(parameter, value);
        return parameter;
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
