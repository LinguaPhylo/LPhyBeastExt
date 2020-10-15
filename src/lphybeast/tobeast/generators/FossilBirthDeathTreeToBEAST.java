package lphybeast.tobeast.generators;

import beast.core.BEASTInterface;
import beast.core.parameter.RealParameter;
import beast.evolution.speciation.SABirthDeathModel;
import lphy.evolution.birthdeath.FossilBirthDeathTree;
import lphy.evolution.tree.TimeTree;
import lphy.graphicalModel.Value;
import lphybeast.BEASTContext;
import lphybeast.GeneratorToBEAST;

public class FossilBirthDeathTreeToBEAST implements
        GeneratorToBEAST<FossilBirthDeathTree, SABirthDeathModel> {

    @Override
    public SABirthDeathModel generatorToBEAST(FossilBirthDeathTree generator, BEASTInterface tree, BEASTContext context) {

        SABirthDeathModel saBirthDeathModel = new SABirthDeathModel();
        saBirthDeathModel.setInputValue("birthRate", context.getAsRealParameter(generator.getBirthRate()));
        saBirthDeathModel.setInputValue("deathRate", context.getAsRealParameter(generator.getDeathRate()));
        saBirthDeathModel.setInputValue("rho", context.getAsRealParameter(generator.getRho()));
        saBirthDeathModel.setInputValue("samplingRate", context.getAsRealParameter(generator.getPsi()));
        saBirthDeathModel.setInputValue("tree", tree);
        saBirthDeathModel.setInputValue("conditionOnRoot", true);
        saBirthDeathModel.setInputValue("conditionOnSampling", true);
        saBirthDeathModel.initAndValidate();

        return saBirthDeathModel;
    }

    @Override
    public Class<FossilBirthDeathTree> getGeneratorClass() {
        return FossilBirthDeathTree.class;
    }

    @Override
    public Class<SABirthDeathModel> getBEASTClass() {
        return SABirthDeathModel.class;
    }
}
