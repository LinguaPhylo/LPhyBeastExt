package lphybeast;

import beast.core.BEASTInterface;
import beast.core.parameter.IntegerParameter;
import beast.core.parameter.Parameter;
import beast.core.parameter.RealParameter;
import feast.function.Slice;
import lphy.graphicalModel.RandomVariable;
import lphy.graphicalModel.Value;
import lphybeast.tobeast.values.ValueToParameter;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Utils class to create frequently used BEAST objects
 * @author Walter Xie
 */
public final class BEASTFactory {

    /**
     * @param slicedBEASTObj  map to "arg" input, argument to extract element from.
     * @param index           map to "index" input, index of first element to extract.
     * @param id              Slice ID
     * @return   {@link Slice}
     */
    public static Slice createSlice(BEASTInterface slicedBEASTObj, int index, String id) {
        return BEASTFactory.createSlice(slicedBEASTObj, index, 1, id);
    }

    /**
     * @param slicedBEASTObj  map to "arg" input, argument to extract element from.
     * @param index           map to "index" input, index of first element to extract.
     * @param count           map to "count" input, Number of elements to extract, default to 1 in BEAST.
     * @param id              Slice ID
     * @return   {@link Slice}
     */
    public static Slice createSlice(BEASTInterface slicedBEASTObj, int index, int count, String id) {
        Slice slice = new Slice();
        slice.setInputValue("arg", slicedBEASTObj);
        slice.setInputValue("index", index);
        if (count != 1) slice.setInputValue("count", count);
        slice.initAndValidate();
        slice.setID(id);
        return slice;
    }

    // Use RealParameter getAsRealParameter(Value value)
    @Deprecated
    public static Parameter<? extends Number> createKeyParameter(Value<? extends Number[]> value,
                                                                 Number lower, Number upper, boolean forceToDouble) {

        List<Number> values = Arrays.asList(value.value());

        // forceToDouble will ignore whether component type is Integer or not
        if ( !forceToDouble &&
                Objects.requireNonNull(value).getType().getComponentType().isAssignableFrom(Integer.class) ) {

            IntegerParameter parameter = new IntegerParameter();
            parameter.setInputValue("value", values);
            parameter.setInputValue("dimension", values.size());

            if (lower != null)
                parameter.setInputValue("lower", lower.intValue());
            if (upper != null)
                parameter.setInputValue("upper", upper.intValue());

            // set estimate="false" for IntegerArray values that are not RandomVariables.
            if (!(value instanceof RandomVariable))
                parameter.setInputValue("estimate", false);

            parameter.initAndValidate();
            ValueToParameter.setID(parameter, value);
            return parameter;

        } else { // Double and Number

            RealParameter parameter = new RealParameter();
            parameter.setInputValue("value", values);
            parameter.setInputValue("dimension", values.size());

            if (lower != null)
                parameter.setInputValue("lower", lower.doubleValue());
            if (upper != null)
                parameter.setInputValue("upper", upper.doubleValue());

            // set estimate="false" for DoubleArray values that are not RandomVariables.
            if (!(value instanceof RandomVariable))
                parameter.setInputValue("estimate", false);

            parameter.initAndValidate();
            ValueToParameter.setID(parameter, value);
            return parameter;

        }


    }


}
