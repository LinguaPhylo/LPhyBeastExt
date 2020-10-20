package lphybeast.tobeast.generators;

import beast.core.BEASTInterface;
import beast.core.parameter.RealParameter;
import lphy.evolution.substitutionmodel.HKY;
import lphybeast.BEASTContext;
import lphybeast.GeneratorToBEAST;

public class HKYToBEAST implements GeneratorToBEAST<HKY, outercore.evolution.substitutionmodel.HKY> {
    @Override
    public outercore.evolution.substitutionmodel.HKY generatorToBEAST(HKY hky, BEASTInterface value, BEASTContext context) {

        outercore.evolution.substitutionmodel.HKY beastHKY = new outercore.evolution.substitutionmodel.HKY();
        beastHKY.setInputValue("kappa", context.getBEASTObject(hky.getKappa()));
        beastHKY.setInputValue("frequencies", BEASTContext.createBEASTFrequencies((RealParameter) context.getBEASTObject(hky.getFreq()),"A C G T"));
        beastHKY.initAndValidate();
        return beastHKY;
    }

    @Override
    public Class<HKY> getGeneratorClass() {
        return HKY.class;
    }

    @Override
    public Class<outercore.evolution.substitutionmodel.HKY> getBEASTClass() {
        return outercore.evolution.substitutionmodel.HKY.class;
    }
}
