package lphybeast.tobeast.values;

import lphy.core.distributions.RandomComposition;
import lphy.graphicalModel.RandomVariable;
import lphy.graphicalModel.Value;
import lphybeast.BEASTContext;
import lphybeast.ValueToBEAST;
import outercore.parameter.KeyIntegerParameter;

import java.util.Arrays;
import java.util.List;

public class IntegerArrayValueToBEAST implements ValueToBEAST<Integer[], KeyIntegerParameter> {

    @Override
    public KeyIntegerParameter valueToBEAST(Value<Integer[]> value, BEASTContext context) {

        KeyIntegerParameter parameter = new KeyIntegerParameter();
        List<Integer> values = Arrays.asList(value.value());
        parameter.setInputValue("value", values);
        parameter.setInputValue("dimension", values.size());
        if (!(value instanceof RandomVariable)) parameter.setInputValue("estimate", false);

        // check domain
        if (value.getGenerator() instanceof RandomComposition) {
            parameter.setInputValue("lower", 1);
        }

        parameter.initAndValidate();
        ValueToParameter.setID(parameter, value);
        return parameter;
    }

    @Override
    public Class getValueClass() {
        return Integer[].class;
    }

    @Override
    public Class<KeyIntegerParameter> getBEASTClass() {
        return KeyIntegerParameter.class;
    }

}
