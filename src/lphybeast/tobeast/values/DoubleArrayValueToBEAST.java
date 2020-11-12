package lphybeast.tobeast.values;

import beast.core.BEASTInterface;
import beast.core.Function;
import beast.core.Operator;
import beast.core.parameter.RealParameter;
import beast.evolution.operators.DeltaExchangeOperator;
import feast.function.Concatenate;
import lphy.core.distributions.Dirichlet;
import lphy.core.distributions.LogNormalMulti;
import lphy.core.distributions.WeightedDirichlet;
import lphy.graphicalModel.Value;
import lphybeast.BEASTContext;
import lphybeast.ValueToBEAST;
import outercore.parameter.KeyRealParameter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static lphybeast.BEASTContext.getOperatorWeight;

public class DoubleArrayValueToBEAST implements ValueToBEAST<Double[], BEASTInterface> {

    @Override
    public BEASTInterface valueToBEAST(Value<Double[]> value, BEASTContext context) {

        if (value.getGenerator() instanceof WeightedDirichlet) {

            WeightedDirichlet weightedDirichlet = (WeightedDirichlet) value.getGenerator();

            Concatenate concatenatedParameters = new Concatenate();
            List<Function> args = new ArrayList<>();

            Double[] values = value.value();

            for (int i = 0; i < values.length; i++) {
                RealParameter parameter = context.createRealParameter(value.getCanonicalId() + "." + i, values[i]);
                context.addStateNode(parameter, value, false);
                args.add(parameter);
            }
            concatenatedParameters.setInputValue("arg", args);
            concatenatedParameters.initAndValidate();

            ValueToParameter.setID(concatenatedParameters, value);

            Operator operator = new DeltaExchangeOperator();
            operator.setInputValue("parameter", args);
            operator.setInputValue("weight", getOperatorWeight(args.size() - 1));
            operator.setInputValue("weightvector", context.getAsIntegerParameter(weightedDirichlet.getWeights()));
            operator.setInputValue("delta", 1.0 / value.value().length);
            operator.initAndValidate();
            operator.setID(value.getCanonicalId() + ".deltaExchange");
            context.addExtraOperator(operator);

            return concatenatedParameters;
        }

        KeyRealParameter parameter = new KeyRealParameter();
        List<Number> values = Arrays.asList(value.value());
        parameter.setInputValue("value", values);
        parameter.setInputValue("dimension", values.size());

        // check domain
        if (value.getGenerator() instanceof Dirichlet) {
            parameter.setInputValue("lower", 0.0);
            parameter.setInputValue("upper", 1.0);
        } else if (value.getGenerator() instanceof WeightedDirichlet) {
            parameter.setInputValue("lower", 0.0);
        } else if (value.getGenerator() instanceof LogNormalMulti) {
            parameter.setInputValue("lower", 0.0);
        }

        parameter.initAndValidate();
        ValueToParameter.setID(parameter, value);
        return parameter;
//        }
    }

    @Override
    public Class getValueClass() {
        return Double[].class;
    }

    @Override
    public Class<BEASTInterface> getBEASTClass() {
        return BEASTInterface.class;
    }
}
