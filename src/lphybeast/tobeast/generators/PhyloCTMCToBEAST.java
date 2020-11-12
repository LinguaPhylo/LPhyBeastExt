package lphybeast.tobeast.generators;

import beast.core.BEASTInterface;
import beast.core.parameter.RealParameter;
import beast.evolution.alignment.Alignment;
import beast.evolution.branchratemodel.StrictClockModel;
import beast.evolution.branchratemodel.UCRelaxedClockModel;
import beast.evolution.likelihood.ThreadedTreeLikelihood;
import beast.evolution.operators.UpDownOperator;
import beast.evolution.sitemodel.SiteModel;
import beast.evolution.substitutionmodel.SubstitutionModel;
import beast.evolution.tree.Tree;
import beast.math.distributions.Prior;
import consoperators.BigPulley;
import consoperators.InConstantDistanceOperator;
import consoperators.SimpleDistance;
import consoperators.SmallPulley;
import lphy.core.distributions.DiscretizedGamma;
import lphy.core.distributions.LogNormalMulti;
import lphy.evolution.branchrates.LocalBranchRates;
import lphy.evolution.likelihood.PhyloCTMC;
import lphy.evolution.substitutionmodel.RateMatrix;
import lphy.evolution.tree.TimeTree;
import lphy.graphicalModel.GenerativeDistribution;
import lphy.graphicalModel.Generator;
import lphy.graphicalModel.RandomVariable;
import lphy.graphicalModel.Value;
import lphybeast.BEASTContext;
import lphybeast.GeneratorToBEAST;

public class PhyloCTMCToBEAST implements GeneratorToBEAST<PhyloCTMC, ThreadedTreeLikelihood> {

    public ThreadedTreeLikelihood generatorToBEAST(PhyloCTMC phyloCTMC, BEASTInterface value, BEASTContext context) {

        ThreadedTreeLikelihood treeLikelihood = new ThreadedTreeLikelihood();

        assert value instanceof beast.evolution.alignment.Alignment;
        beast.evolution.alignment.Alignment alignment = (beast.evolution.alignment.Alignment)value;
        treeLikelihood.setInputValue("data", alignment);

        Value<TimeTree> timeTreeValue = phyloCTMC.getTree();
        Tree tree = (Tree) context.getBEASTObject(timeTreeValue);
        //tree.setInputValue("taxa", value);
        //tree.initAndValidate();

        treeLikelihood.setInputValue("tree", tree);

        Value<Double[]> branchRates = phyloCTMC.getBranchRates();

        if (branchRates != null) {

            if (branchRates.getGenerator() instanceof LogNormalMulti) {

                UCRelaxedClockModel relaxedClockModel = new UCRelaxedClockModel();

                Prior logNormalPrior = (Prior) context.getBEASTObject(branchRates.getGenerator());

                RealParameter beastBranchRates = (RealParameter) context.getBEASTObject(branchRates);

                relaxedClockModel.setInputValue("rates", beastBranchRates);
                relaxedClockModel.setInputValue("tree", tree);
                relaxedClockModel.setInputValue("distr", logNormalPrior.distInput.get());
                relaxedClockModel.initAndValidate();
                treeLikelihood.setInputValue("branchRateModel", relaxedClockModel);

                addRelaxedClockOperators(tree, relaxedClockModel, beastBranchRates, context);

            } else if (branchRates.getGenerator() instanceof LocalBranchRates) {
                treeLikelihood.setInputValue("branchRateModel", context.getBEASTObject(branchRates.getGenerator()));
            } else {
                throw new RuntimeException("Only localBranchRates and lognormally distributed branchRates currently supported for BEAST2 conversion");
            }

            if (branchRates instanceof RandomVariable && timeTreeValue instanceof RandomVariable) {
//TODO
            }

        } else {
            StrictClockModel clockModel = new StrictClockModel();
            Value<Number> clockRate = phyloCTMC.getClockRate();

            RealParameter clockRatePara;
            if (clockRate != null) {
                clockRatePara = (RealParameter) context.getBEASTObject(clockRate);

            } else {
                clockRatePara =  BEASTContext.createRealParameter(1.0);
            }
            clockModel.setInputValue("clock.rate", clockRatePara);
            treeLikelihood.setInputValue("branchRateModel", clockModel);

            if (clockRate instanceof RandomVariable && timeTreeValue instanceof RandomVariable) {
                addUpDownOperator(tree, clockRatePara, context);
            }
        }

        SiteModel siteModel = constructSiteModel(phyloCTMC, context);
        treeLikelihood.setInputValue("siteModel", siteModel);

        treeLikelihood.initAndValidate();
        treeLikelihood.setID(alignment.getID() + ".treeLikelihood");
        // logging
        context.addExtraLogger(treeLikelihood);

        return treeLikelihood;
    }

    /**
     * @param phyloCTMC the phyloCTMC object
     * @param context the beast context
     * @return a BEAST SiteModel representing the site model of this LPHY PhyloCTMC
     */
    private SiteModel constructSiteModel(PhyloCTMC phyloCTMC, BEASTContext context) {

        SiteModel siteModel = new SiteModel();

        Value<Double[]> siteRates = phyloCTMC.getSiteRates();

        if (siteRates != null) {
            Generator generator = siteRates.getGenerator();
            if (generator instanceof DiscretizedGamma) {
                DiscretizedGamma discretizedGamma = (DiscretizedGamma)generator;
                siteModel.setInputValue("shape", context.getAsRealParameter(discretizedGamma.getShape()));
                siteModel.setInputValue("gammaCategoryCount", discretizedGamma.getNcat().value());
            } else {
                throw new RuntimeException("Only discretized gamma site rates are supported by LPhyBEAST");
            }
        }

        Generator qGenerator = phyloCTMC.getQ().getGenerator();
        if (qGenerator == null || !(qGenerator instanceof RateMatrix)) {
            throw new RuntimeException("BEAST2 only supports Q matrices constructed by a RateMatrix function (e.g. hky, gtr, jukeCantor et cetera).");
        } else {
            RateMatrix rateMatrix = (RateMatrix)qGenerator;

            BEASTInterface mutationRate = context.getBEASTObject(rateMatrix.getMeanRate());

            SubstitutionModel substitutionModel = (SubstitutionModel) context.getBEASTObject(qGenerator);

            if (substitutionModel == null) throw new IllegalArgumentException("Substitution Model was null!");

            siteModel.setInputValue("substModel", substitutionModel);
            if (mutationRate != null) siteModel.setInputValue("mutationRate", mutationRate);
            siteModel.initAndValidate();
        }
        return siteModel;
    }

    private void addRelaxedClockOperators(Tree tree, UCRelaxedClockModel relaxedClockModel, RealParameter rates, BEASTContext context) {

        double tWindowSize = tree.getRoot().getHeight() / 10.0;

        InConstantDistanceOperator inConstantDistanceOperator = new InConstantDistanceOperator();
        inConstantDistanceOperator.setInputValue("clockModel", relaxedClockModel);
        inConstantDistanceOperator.setInputValue("tree", tree);
        inConstantDistanceOperator.setInputValue("rates", rates);
        inConstantDistanceOperator.setInputValue("twindowSize", tWindowSize);
        inConstantDistanceOperator.setInputValue("weight", BEASTContext.getOperatorWeight(tree.getNodeCount()));
        inConstantDistanceOperator.initAndValidate();
        context.addExtraOperator(inConstantDistanceOperator);

        SimpleDistance simpleDistance = new SimpleDistance();
        simpleDistance.setInputValue("clockModel", relaxedClockModel);
        simpleDistance.setInputValue("tree", tree);
        simpleDistance.setInputValue("rates", rates);
        simpleDistance.setInputValue("twindowSize", tWindowSize);
        simpleDistance.setInputValue("weight", BEASTContext.getOperatorWeight(2));
        simpleDistance.initAndValidate();
        context.addExtraOperator(simpleDistance);

        BigPulley bigPulley = new BigPulley();
        bigPulley.setInputValue("tree", tree);
        bigPulley.setInputValue("rates", rates);
        bigPulley.setInputValue("twindowSize", tWindowSize);
        bigPulley.setInputValue("dwindowSize", 0.1);
        bigPulley.setInputValue("weight", BEASTContext.getOperatorWeight(2));
        bigPulley.initAndValidate();
        context.addExtraOperator(bigPulley);

        SmallPulley smallPulley = new SmallPulley();
        smallPulley.setInputValue("clockModel", relaxedClockModel);
        smallPulley.setInputValue("tree", tree);
        smallPulley.setInputValue("rates", rates);
        smallPulley.setInputValue("dwindowSize", 0.1);
        smallPulley.setInputValue("weight", BEASTContext.getOperatorWeight(2));
        smallPulley.initAndValidate();
        context.addExtraOperator(smallPulley);
    }

    // when both mu and tree are random var
    private void addUpDownOperator(Tree tree, RealParameter clockRate, BEASTContext context) {
        String idStr = clockRate.getID() + "Up" + tree.getID() + "DownOperator";
        // avoid to duplicate updown ops from the same pair of rate and tree
        if (!context.hasExtraOperator(idStr)) {
            UpDownOperator upDownOperator = new UpDownOperator();
            upDownOperator.setID(idStr);
            upDownOperator.setInputValue("up", clockRate);
            upDownOperator.setInputValue("down", tree);
            upDownOperator.setInputValue("scaleFactor", 0.9);
            upDownOperator.setInputValue("weight", BEASTContext.getOperatorWeight(tree.getInternalNodeCount()+1));
            context.addExtraOperator(upDownOperator);
        }
    }

    @Override
    public Class<PhyloCTMC> getGeneratorClass() {
        return PhyloCTMC.class;
    }

    @Override
    public Class<ThreadedTreeLikelihood> getBEASTClass() {
        return ThreadedTreeLikelihood.class;
    }
}
