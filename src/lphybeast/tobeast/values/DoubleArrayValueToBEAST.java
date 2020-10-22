package lphybeast.tobeast.values;

import beast.core.parameter.CompoundRealParameter;
import beast.core.parameter.RealParameter;
import lphy.core.distributions.Dirichlet;
import lphy.core.distributions.LogNormalMulti;
import lphy.core.functions.DoubleArray;
import lphy.graphicalModel.Value;
import lphybeast.BEASTContext;
import lphybeast.ValueToBEAST;
import outercore.parameter.KeyRealParameter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DoubleArrayValueToBEAST implements ValueToBEAST<Double[], RealParameter> {

    @Override
    public RealParameter valueToBEAST(Value<Double[]> value, BEASTContext context) {

        if (value.getGenerator() != null && value.getGenerator() instanceof DoubleArray) {
            DoubleArray daGenerator = (DoubleArray)value.getGenerator();

            CompoundRealParameter compoundRealParameter = new CompoundRealParameter();
            List<RealParameter> parameters = new ArrayList<>();

            Value<Double>[] values = daGenerator.getValues();
            for (Value<Double> subValue : values) {
                parameters.add((RealParameter)context.getBEASTObject(subValue));
            }
            compoundRealParameter.setInputValue("parameter", parameters);
            compoundRealParameter.initAndValidate();

            ValueToParameter.setID(compoundRealParameter, value);

            return compoundRealParameter;
        } else {

            KeyRealParameter parameter = new KeyRealParameter();
            List<Number> values = Arrays.asList(value.value());
            parameter.setInputValue("value", values);
            parameter.setInputValue("dimension", values.size());

            // check domain
            if (value.getGenerator() instanceof Dirichlet) {
                parameter.setInputValue("lower", 0.0);
                parameter.setInputValue("upper", 1.0);
            } else if (value.getGenerator() instanceof LogNormalMulti) {
                parameter.setInputValue("lower", 0.0);
            }

            parameter.initAndValidate();
            ValueToParameter.setID(parameter, value);
            return parameter;
        }
    }

    @Override
    public Class getValueClass() {
        return Double[].class;
    }

    @Override
    public Class<RealParameter> getBEASTClass() {
        return RealParameter.class;
    }

}
