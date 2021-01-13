package lphybeast.tobeast.generators;

import beast.core.BEASTInterface;
import beast.core.parameter.BooleanParameter;
import beast.core.parameter.RealParameter;
import beast.evolution.substitutionmodel.Frequencies;
import beast.evolution.substitutionmodel.SVSGeneralSubstitutionModel;
import lphy.core.functions.Select;
import lphy.evolution.substitutionmodel.GeneralTimeReversible;
import lphy.graphicalModel.Generator;
import lphy.graphicalModel.GraphicalModelNode;
import lphy.graphicalModel.Value;
import lphybeast.BEASTContext;
import lphybeast.GeneratorToBEAST;

import java.util.Map;

public class GTRToDiscretePhylogeo implements
        GeneratorToBEAST<GeneralTimeReversible, SVSGeneralSubstitutionModel> {

    @Override
    public SVSGeneralSubstitutionModel generatorToBEAST(GeneralTimeReversible gtr,
                                                        BEASTInterface value, BEASTContext context) {

        SVSGeneralSubstitutionModel svs = new SVSGeneralSubstitutionModel();

        Generator ratesGenerator = gtr.getRates().getGenerator();
        // rates=select(x=trait_rates, indicator=trait_indicators)
        Map<String, Value> selectFunParams = ratesGenerator.getParams();
        if (selectFunParams.size() != 2)
            throw new IllegalStateException("Expecting 'select' function to produce traits rates, given rates and boolean indicators");

        GraphicalModelNode<?> rateNode = (GraphicalModelNode<?>) selectFunParams.get(Select.valueParamName);
        RealParameter rates = (RealParameter) context.getBEASTObject(rateNode);
        svs.setInputValue("rates", rates);

        GraphicalModelNode<?> indicatorNode = (GraphicalModelNode<?>) selectFunParams.get(Select.indicatorParamName);
        BooleanParameter rateIndicators = (BooleanParameter) context.getBEASTObject(indicatorNode);
        svs.setInputValue("rateIndicator", rateIndicators);

//        String[] stateNames = getStateNames();
//        String names = Arrays.toString(stateNames);
//        BEASTContext.createBEASTFrequencies((RealParameter) context.getBEASTObject(gtr.getFreq()), names);
        // TODO how to map the state names to the correct dimension?

        RealParameter traitfrequencies = (RealParameter) context.getBEASTObject(gtr.getFreq());
        Frequencies traitfreqs = new Frequencies();
        traitfreqs.setInputValue("frequencies", traitfrequencies);
        traitfreqs.initAndValidate();
        svs.setInputValue("frequencies", traitfreqs);

        // only symmetric
        svs.setInputValue("symmetric", Boolean.TRUE);
        svs.initAndValidate();

//        <operator id="BSSVSoperator" spec="BitFlipBSSVSOperator" indicator="@rateIndicator" mu="@traitClockRate" weight="10.0"/>
//        context.addExtraOperator();

        return svs;
    }

    private String[] getStateNames() {
        return new String[]{};
    }


    @Override
    public Class<GeneralTimeReversible> getGeneratorClass() {
        return GeneralTimeReversible.class;
    }

    @Override
    public Class<SVSGeneralSubstitutionModel> getBEASTClass() {
        return SVSGeneralSubstitutionModel.class;
    }
}
