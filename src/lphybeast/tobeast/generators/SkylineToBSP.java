package lphybeast.tobeast.generators;

import beast.core.BEASTInterface;
import beast.core.parameter.IntegerParameter;
import beast.core.parameter.RealParameter;
import beast.evolution.tree.coalescent.TreeIntervals;
import lphy.evolution.coalescent.SkylineCoalescent;
import lphybeast.BEASTContext;
import lphybeast.GeneratorToBEAST;

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

        BEASTInterface theta = context.getBEASTObject(coalescent.getTheta());
        if ( ! (theta instanceof RealParameter) )
            throw new IllegalArgumentException("Expecting RealParameter for Skyline pop size ! ");

        RealParameter thetaParam = (RealParameter) theta;
        // TODO best way to check the index in the keys
        // keys reply on BEAST Parameter default String getKey(int i)
        // expecting keys = [1, 2, 3, ...]
        int i = Integer.parseInt(thetaParam.getKey(0));
        // https://github.com/LinguaPhylo/LPhyBeast/issues/33
        if (i < 1)
            throw new IllegalArgumentException("Tracer requires the key of pop size parameter to start from 1, which cannot pick up 0");
        // set keys explicitly to show them in XML
        String[] keys = thetaParam.getKeys();
        String keysStr = String.join(" ", keys);
        theta.setInputValue("keys", keysStr);

        bsp.setInputValue("popSizes", theta);

        // pop size index has to be same as group size, for Tracer skyline plot
        IntegerParameter groupSizeParam = null;
        if (coalescent.getGroupSizes() != null) {
            groupSizeParam = (IntegerParameter)context.getBEASTObject(coalescent.getGroupSizes());
        } else {
            // classic skyline
            groupSizeParam = new IntegerParameter();
            List<Integer> groupSizes = new ArrayList<>();
            for (int j = 0; j < thetaParam.getDimension(); j++) {
                groupSizes.add(1);
            }
            groupSizeParam.setInputValue("value", groupSizes);
            groupSizeParam.setInputValue("dimension", groupSizes.size());
            groupSizeParam.setID("groupSizes");
        }
        // expecting keys = [1, 2, 3, ...]
        i = Integer.parseInt(groupSizeParam.getKey(0));
        // https://github.com/LinguaPhylo/LPhyBeast/issues/33
        if (i < 1)
            throw new IllegalArgumentException("Tracer requires the key of group size parameter to start from 1, which cannot pick up 0");

        // set keys explicitly to show them in XML
        keys = groupSizeParam.getKeys();
        keysStr = String.join(" ", keys);
        groupSizeParam.setInputValue("keys", keysStr);

        bsp.setInputValue("groupSizes", groupSizeParam);
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
