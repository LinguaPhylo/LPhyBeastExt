package lphybeast.registry;

import beast.evolution.datatype.DataType;
import jebl.evolution.sequences.SequenceType;
import lphy.evolution.datatype.Binary;
import lphy.evolution.datatype.Continuous;
import lphybeast.tobeast.generators.*;
import lphybeast.tobeast.values.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Walter Xie
 */
public class LPhyBEASTRegistry implements ClassesRegistry{

    // the first matching converter is used.
    private final Class[] valuesToBEASTs = {
            DoubleArrayValueToBEAST.class,  // KeyRealParameter
            IntegerArrayValueToBEAST.class, // KeyIntegerParameter
            NumberArrayValueToBEAST.class,
            CompoundVectorToBEAST.class, // TODO handle primitive CompoundVector properly
            AlignmentToBEAST.class, // simulated alignment
            TimeTreeToBEAST.class,
            DoubleValueToBEAST.class,
            DoubleArray2DValueToBEAST.class,
            IntegerValueToBEAST.class,
            BooleanArrayValueToBEAST.class,
            BooleanValueToBEAST.class
    };

    // the first matching converter is used.
    private final Class[] generatorToBEASTs = {
            BernoulliMultiToBEAST.class, // cannot be replaced by IID
            BetaToBEAST.class,
            BirthDeathSerialSamplingToBEAST.class,
            BirthDeathSampleTreeDTToBEAST.class,
            DirichletToBEAST.class,
            ExpToBEAST.class,
            F81ToBEAST.class,
            FossilBirthDeathTreeToBEAST.class,
            GammaToBEAST.class,
            GTRToDiscretePhylogeo.class,
            GTRToBEAST.class,
            HKYToBEAST.class,
            IIDToBEAST.class,
            InverseGammaToBEAST.class,
            JukesCantorToBEAST.class,
            K80ToBEAST.class,
            LewisMKToBeast.class,
            LocalBranchRatesToBEAST.class,
            LogNormalToBEAST.class,
//                MultispeciesCoalescentToStarBEAST2.class,
            NormalToBEAST.class,
            PhyloCTMCToBEAST.class,
            PoissonToBEAST.class,
            RandomBooleanArrayToBEAST.class,
            SerialCoalescentToBEAST.class,
            SimFBDAgeToBEAST.class,
            SkylineToBSP.class,
            SliceDoubleArrayToBEAST.class,
            StructuredCoalescentToMascot.class,
            TreeLengthToBEAST.class,
            TN93ToBEAST.class,
            UniformToBEAST.class,
            VectorizedDistributionToBEAST.class,
            VectorizedFunctionToBEAST.class,
            YuleToBEAST.class,
            ExpMarkovChainToBEAST.class
    };


    @Override
    public Class[] getValuesToBEASTs() {
        return valuesToBEASTs;
    }

    @Override
    public Class[] getGeneratorToBEASTs() {
        return generatorToBEASTs;
    }

    // LPhy SequenceType => BEAST DataType
    @Override
    public Map<SequenceType, DataType> getDataTypeMap() {
        Map<SequenceType, DataType> dataTypeMap = new ConcurrentHashMap<>();
        dataTypeMap.put(SequenceType.NUCLEOTIDE, new beast.evolution.datatype.Nucleotide());
        dataTypeMap.put(SequenceType.AMINO_ACID, new beast.evolution.datatype.Aminoacid());
        dataTypeMap.put(Binary.getInstance(), new beast.evolution.datatype.Binary());
        dataTypeMap.put(Continuous.getInstance(), new beast.evolution.datatype.ContinuousDataType());
        return dataTypeMap;
    }

    private Map<SequenceType, DataType> dataTypeMap;

}
