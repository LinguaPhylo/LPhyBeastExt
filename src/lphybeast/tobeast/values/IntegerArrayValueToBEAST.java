package lphybeast.tobeast.values;

import lphy.core.distributions.RandomComposition;
import lphy.graphicalModel.Value;
import lphybeast.BEASTContext;
import lphybeast.BEASTFactory;
import lphybeast.ValueToBEAST;
import outercore.parameter.KeyIntegerParameter;
import outercore.parameter.KeyParameter;

public class IntegerArrayValueToBEAST implements ValueToBEAST<Integer[], KeyIntegerParameter> {

    @Override
    public KeyIntegerParameter valueToBEAST(Value<Integer[]> value, BEASTContext context) {

//        KeyIntegerParameter parameter = new KeyIntegerParameter();
//        List<Integer> values = Arrays.asList(value.value());
//        parameter.setInputValue("value", values);
//        parameter.setInputValue("dimension", values.size());
//        // set estimate="false" for IntegerArray values that are not RandomVariables.
//        if (!(value instanceof RandomVariable)) parameter.setInputValue("estimate", false);

        // check domain
        Integer lower = null;
        if (value.getGenerator() instanceof RandomComposition) {
//            parameter.setInputValue("lower", 1);
            lower = 1;
        }
        Integer upper = null;

        KeyParameter parameter = BEASTFactory.createKeyParameter(value, lower, upper, false);
        if (!(parameter instanceof KeyIntegerParameter))
            throw new IllegalStateException("Expecting to create KeyIntegerParameter from " + value.getCanonicalId());

        return (KeyIntegerParameter) parameter;
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
