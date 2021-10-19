package lphybeast.tobeast;

import beast.evolution.datatype.DataType;
import beast.evolution.datatype.UserDataType;
import jebl.evolution.sequences.SequenceType;
import jebl.evolution.sequences.State;
import lphy.evolution.datatype.Standard;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Utils
 * @author Walter Xie
 */
public class DataTypeUtils {

    public static DataType getBEASTDataType(SequenceType lphyDataType, Map<SequenceType, DataType> dataTypeMap) {
        if (lphyDataType instanceof Standard) {
            return getUserDataType((Standard) lphyDataType);
        }
        return dataTypeMap.get(lphyDataType);
    }

    // for trait alignment
    private static UserDataType getUserDataType(Standard lphyDataType) {
        // userDataType: non-standard, user specified data type, if specified 'dataType' is ignored
        UserDataType userDataType = new UserDataType();

        List<State> states = lphyDataType.getStates();
        // State toString is stateCode
        String codeMap = IntStream.range(0, states.size())
                .mapToObj(i -> states.get(i) + "=" + i)
                .collect(Collectors.joining(","));
        // create ambiguous state
        codeMap += ", ? = " + IntStream.range(0, states.size()).mapToObj(String::valueOf)
                .collect(Collectors.joining(" "));

        // codeMap="Asia=0,EU=1,NZ=2,RoW=3,USA=4,? = 0 1 2 3 4" codelength="-1" states="5"
        userDataType.initByName("codeMap", codeMap,
                "codelength", -1, "states", states.size());
        // this will create codeMapping in StandardData
//        userDataType.setInputValue("nrOfStates", alignment.getSequenceType().getCanonicalStateCount());
        userDataType.initAndValidate();
        return userDataType;
    }

}
