package lphybeast.tobeast.generators;

import beast.core.BEASTInterface;
import lphy.evolution.substitutionmodel.LewisMK;
import lphybeast.BEASTContext;
import lphybeast.GeneratorToBEAST;

public class LewisMKToBeast implements GeneratorToBEAST<LewisMK, beast.evolution.substitutionmodel.LewisMK> {
    @Override
    public beast.evolution.substitutionmodel.LewisMK generatorToBEAST(LewisMK lewisMK, BEASTInterface value, BEASTContext context) {

        beast.evolution.substitutionmodel.LewisMK beastLewisMK = new beast.evolution.substitutionmodel.LewisMK();
        beastLewisMK.setInputValue("stateNumber", lewisMK.getNumStates().value());
        beastLewisMK.initAndValidate();
        return beastLewisMK;
    }

    @Override
    public Class<LewisMK> getGeneratorClass() { return LewisMK.class; }

    @Override
    public Class<beast.evolution.substitutionmodel.LewisMK> getBEASTClass() {
        return beast.evolution.substitutionmodel.LewisMK.class;
    }
}
