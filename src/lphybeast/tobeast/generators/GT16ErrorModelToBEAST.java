package lphybeast.tobeast.generators;

import beast.core.BEASTInterface;
import beast.core.parameter.RealParameter;
import beast.evolution.datatype.DataType;
import jebl.evolution.sequences.SequenceType;
import lphy.evolution.alignment.Alignment;
import lphy.evolution.alignment.GT16ErrorModel;
import lphybeast.BEASTContext;
import lphybeast.DataTypeRegistry;
import lphybeast.GeneratorToBEAST;

/**
 * @author Walter Xie
 */
public class GT16ErrorModelToBEAST implements GeneratorToBEAST<GT16ErrorModel, beast.evolution.errormodel.GT16ErrorModel>  {

    @Override
    public beast.evolution.errormodel.GT16ErrorModel generatorToBEAST(GT16ErrorModel generator, BEASTInterface value, BEASTContext context) {

        // the allelic drop out probability
        double delta = generator.getDelta();
        // the sequencing/amplification error rate
        double epsilon = generator.getEpsilon();

        Alignment origAlg = generator.getOriginalAlignment();
        SequenceType lphyDataType = origAlg.getSequenceType();
        DataType beastDataType = DataTypeRegistry.getBEASTDataType(lphyDataType);


        beast.evolution.errormodel.GT16ErrorModel gt16ErrorModel = new beast.evolution.errormodel.GT16ErrorModel();
        // Input<DataType> datatypeInput
        gt16ErrorModel.setInputValue("datatype", beastDataType);

        RealParameter deltaParam = new RealParameter(String.valueOf(delta));
        gt16ErrorModel.setInputValue("delta", deltaParam);
        RealParameter epsilonParam = new RealParameter(String.valueOf(epsilon));
        gt16ErrorModel.setInputValue("epsilon", epsilonParam);

        gt16ErrorModel.initAndValidate();

        return gt16ErrorModel;
    }

    @Override
    public Class<GT16ErrorModel> getGeneratorClass() {
        return GT16ErrorModel.class;
    }

    @Override
    public Class<beast.evolution.errormodel.GT16ErrorModel> getBEASTClass() {
        return beast.evolution.errormodel.GT16ErrorModel.class;
    }
}
