package lphybeast.tobeast.generators;

import beast.core.BEASTInterface;
import beast.core.parameter.IntegerParameter;
import beast.core.parameter.RealParameter;
import beast.evolution.tree.coalescent.TreeIntervals;
import lphy.evolution.coalescent.SkylineCoalescent;
import lphybeast.BEASTContext;
import lphybeast.GeneratorToBEAST;
import outercore.parameter.KeyIntegerParameter;
import outercore.parameter.KeyRealParameter;

import java.util.ArrayList;
import java.util.List;

public class SkylineToBSP implements
        GeneratorToBEAST<SkylineCoalescent, beast.evolution.tree.coalescent.BayesianSkyline> {
    @Override
    public beast.evolution.tree.coalescent.BayesianSkyline generatorToBEAST(SkylineCoalescent coalescent, BEASTInterface value, BEASTContext context) {

        beast.evolution.tree.coalescent.BayesianSkyline bsp = new beast.evolution.tree.coalescent.BayesianSkyline();

        TreeIntervals treeIntervals = new TreeIntervals();
        treeIntervals.setInputValue("tree", value);
        treeIntervals.initAndValidate();

        bsp.setInputValue("treeIntervals", treeIntervals);

        // https://github.com/LinguaPhylo/LPhyBeast/issues/33
        BEASTInterface theta = context.getBEASTObject(coalescent.getTheta());
        if ( ! (theta instanceof RealParameter) )
            throw new IllegalArgumentException("Expecting RealParameter for Skyline pop size ! ");

        KeyRealParameter popSizes = KeyRealParameter.createKeyRealParameter((RealParameter) theta);
        // TODO For Tracer, which requires index starting from 1
        popSizes.setInputValue("idStart1", true);
        popSizes.initAndValidate();

        bsp.setInputValue("popSizes", popSizes);

        // pop size index has to be same as group size, for Tracer skyline plot
        IntegerParameter groupSizeParam = null;
        if (coalescent.getGroupSizes() != null) {
            groupSizeParam = (IntegerParameter)context.getBEASTObject(coalescent.getGroupSizes());
        } else {
            // classic skyline
            groupSizeParam = new IntegerParameter();
            List<Integer> groupSizes = new ArrayList<>();
            for (int i = 0; i < coalescent.getTheta().value().length; i++) {
                groupSizes.add(1);
            }
            groupSizeParam.setInputValue("value", groupSizes);
            groupSizeParam.setInputValue("dimension", groupSizes.size());
            groupSizeParam.setID("groupSizes");
        }

        KeyIntegerParameter groupSizesKeyIntParam = KeyIntegerParameter.createKeyIntegerParameter((IntegerParameter) groupSizeParam);
        // TODO For Tracer, which requires index starting from 1
        groupSizesKeyIntParam.setInputValue("idStart1", true);
        groupSizesKeyIntParam.initAndValidate();

        bsp.setInputValue("groupSizes", groupSizesKeyIntParam);
        bsp.initAndValidate();

        return bsp;
    }

    @Override
    public Class<SkylineCoalescent> getGeneratorClass() {
        return SkylineCoalescent.class;
    }

    @Override
    public Class<beast.evolution.tree.coalescent.BayesianSkyline> getBEASTClass() {
        return beast.evolution.tree.coalescent.BayesianSkyline.class;
    }
}
