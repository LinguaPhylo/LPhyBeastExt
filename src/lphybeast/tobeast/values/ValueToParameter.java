package lphybeast.tobeast.values;

import beast.core.BEASTInterface;
import beast.core.Function;
import beast.core.parameter.Parameter;
import lphy.graphicalModel.Value;

public class ValueToParameter {

    public static void setID(BEASTInterface parameter, Value value) {
        if (!value.isAnonymous()) parameter.setID(value.getCanonicalId());
    }
}
