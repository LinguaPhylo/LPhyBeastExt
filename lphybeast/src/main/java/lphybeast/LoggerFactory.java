package lphybeast;

import beast.core.*;
import beast.core.util.CompoundDistribution;
import beast.evolution.likelihood.AncestralStateTreeLikelihood;
import beast.evolution.tree.*;
import com.google.common.collect.Multimap;
import lphy.evolution.coalescent.SkylineCoalescent;
import lphy.evolution.coalescent.StructuredCoalescent;
import lphy.graphicalModel.Generator;
import lphy.graphicalModel.GraphicalModelNode;
import lphy.graphicalModel.RandomVariable;

import java.util.*;
import java.util.stream.Collectors;

import static lphybeast.BEASTContext.*;

/**
 * A class to create all operators
 * @author Walter Xie
 */
public class LoggerFactory {
    // state nodes
    final private List<StateNode> state;
    // a list of extra beast elements in the keys,
    // with a pointer to the graphical model node that caused their production
    final private Multimap<BEASTInterface, GraphicalModelNode<?>> elements;
    // a map of BEASTInterface to graphical model nodes that they represent
    final private Map<BEASTInterface, GraphicalModelNode<?>> BEASTToLPHYMap;

    // a list of extra loggables in 3 default loggers: parameter logger, screen logger, tree logger.
    final private List<Loggable> extraLoggables;
    // extra loggers from extensions
    final private List<Logger> extraLoggers;

    private CompoundDistribution[] topDist = new CompoundDistribution[3];

    public LoggerFactory(BEASTContext context) {
        this.state = context.getState();
        this.elements = context.getElements();
        this.BEASTToLPHYMap = context.getBEASTToLPHYMap();
        this.extraLoggables = context.getExtraLoggables();
        this.extraLoggers = context.getExtraLoggers();
    }

    /**
     * @param logEvery  Number of the samples logged
     * @param fileName  null for screen logger
     * @return    3 default loggers: parameter logger, screen logger, tree logger.
     * @see Logger
     */
    public List<Logger> createLoggers(int logEvery, String fileName) {
        topDist = getTopCompoundDist();

        List<Logger> loggers = new ArrayList<>();
        // reduce screen logging
        loggers.add(createLogger(logEvery * 100, null));
        // parameter logger
        loggers.add(createLogger(logEvery, fileName + ".log"));
        // tree logger
        loggers.addAll(createTreeLoggers(logEvery, fileName));

        return loggers;
    }

    /**
     * @return   a list of {@link TreeInterface}
     */
    public List<TreeInterface> getTrees() {
        return state.stream()
                .filter(stateNode -> stateNode instanceof TreeInterface)
                .map(stateNode -> (TreeInterface) stateNode)
                .sorted(Comparator.comparing(TreeInterface::getID))
                .collect(Collectors.toList());
    }

    //*** default parameter/screen loggers ***//

    // screen logger if fileName is null
    private Logger createLogger(int logEvery, String fileName) {

        List<Loggable> nonTrees = state.stream()
                .filter(stateNode -> !(stateNode instanceof Tree))
                .collect(Collectors.toList());

        // tree height, but not in screen logging
        if (fileName != null) {
            List<TreeInterface> trees = getTrees();
            for (TreeInterface tree : trees) {
                TreeStatLogger treeStatLogger = new TreeStatLogger();
                // use default, which will log the length
                treeStatLogger.initByName("tree", tree);
                nonTrees.add(treeStatLogger);
            }
        }

        // not in screen logging
        if (fileName != null) {
//            nonTrees.addAll(extraLoggables);
            for (Loggable loggable : extraLoggables) {
                if (loggable instanceof ExtraLogger extraLogger) {
                    // if extra logger, then get the original beast2 loggable class
                    // otherwise logger.setInputValue("log", nonTrees) will fail.
                    nonTrees.add(extraLogger.getLoggable()); //TODO not all models have 1 Loggable and 1 Tree
                } else {
                    nonTrees.add(loggable);
                }
            }
        }

//        for (Loggable loggable : extraLoggables) {
//            // fix StructuredCoalescent log
//            if (loggable instanceof Constant) {
//                // Constant includes Ne and m
//                RealParameter ne = ((Constant) loggable).NeInput.get();
//                nonTrees.remove(ne);
//                RealParameter m = ((Constant) loggable).b_mInput.get();
//                nonTrees.remove(m);
//            }
//        }

        // add them in the end to avoid sorting
        nonTrees.addAll(0, Arrays.asList(topDist));

        Logger logger = new Logger();
        logger.setInputValue("logEvery", logEvery);
        logger.setInputValue("log", nonTrees);
        if (fileName != null) logger.setInputValue("fileName", fileName);
        logger.initAndValidate();
        elements.put(logger, null);
        return logger;
    }

    // sorted by specific order
    private CompoundDistribution[] getTopCompoundDist() {
        for (BEASTInterface bI : elements.keySet()) {
            if (bI instanceof CompoundDistribution && bI.getID() != null) {
                if (bI.getID().equals(POSTERIOR_ID))
                    topDist[0] = (CompoundDistribution) bI;
                else if (bI.getID().equals(LIKELIHOOD_ID))
                    topDist[1] = (CompoundDistribution) bI;
                else if (bI.getID().equals(PRIOR_ID))
                    topDist[2] = (CompoundDistribution) bI;
            }
        }
        return topDist;
    }

    private CompoundDistribution getPosteriorDist() {
        return topDist[0];
    }

    //*** default tree loggers ***//

    private List<Logger> createTreeLoggers(int logEvery, String fileNameStem) {

        List<TreeInterface> trees = getTrees();

        boolean multipleTrees = trees.size() > 1;

        List<Logger> treeLoggers = new ArrayList<>();

        // TODO: use tree-likelihood instead, and get all trees from tree-likelihood?

        for (TreeInterface tree : trees) {
            GraphicalModelNode graphicalModelNode = BEASTToLPHYMap.get(tree);
            Generator generator = ((RandomVariable) graphicalModelNode).getGenerator();

            boolean logMetaData = generator instanceof SkylineCoalescent ||
                    generator instanceof StructuredCoalescent;

            Logger logger = new Logger();
            logger.setInputValue("logEvery", logEvery);
            if (logMetaData) { // TODO
                TreeWithMetaDataLogger treeWithMetaDataLogger = new TreeWithMetaDataLogger();
                treeWithMetaDataLogger.setInputValue("tree", tree);
                logger.setInputValue("log", treeWithMetaDataLogger);
            } else
                logger.setInputValue("log", tree);

            String fileName = Objects.requireNonNull(fileNameStem) + ".trees";
            if (multipleTrees) // multi-partitions and unlink trees
                fileName = fileNameStem + "_" + tree.getID() + ".trees";

            logger.setInputValue("fileName", fileName);
            logger.setInputValue("mode", "tree");
            logger.initAndValidate();
            logger.setID(tree.getID() + ".treeLogger");
            treeLoggers.add(logger);
            elements.put(logger, null);
        }

        // extra tree logger
        // extraLoggables are used to retain all tree-likelihoods
        for (Loggable loggable : extraLoggables) {

            if (loggable instanceof ExtraLogger extraLogger) { // TODO
                // Mascot StructuredTreeLogger
                extraLogger.setMultiTrees(multipleTrees);
                Logger logger = extraLogger.createExtraLogger(logEvery, fileNameStem);

                treeLoggers.add(logger);
                elements.put(logger, null);

            } else if (loggable instanceof AncestralStateTreeLikelihood ancestralStateTreeLikelihood) { // TODO
                // DPG: TreeWithTraitLogger
                TreeInterface tree = ancestralStateTreeLikelihood.treeInput.get();

                TreeWithTraitLogger treeWithTraitLogger = new TreeWithTraitLogger();
                treeWithTraitLogger.setInputValue("tree", tree);

                List<BEASTObject> metadata = new ArrayList<>();
                metadata.add((AncestralStateTreeLikelihood) loggable);
                // posterior
                metadata.add(getPosteriorDist());
                treeWithTraitLogger.setInputValue("metadata", metadata);

                Logger logger = new Logger();
                logger.setInputValue("logEvery", logEvery);
                logger.setInputValue("log", treeWithTraitLogger);

                String treeFNSteam = Objects.requireNonNull(fileNameStem) + "_with_trait";
                if (multipleTrees) // multi-partitions and unlink trees
                    treeFNSteam = fileNameStem + "_" + tree.getID();
                String fileName = treeFNSteam + ".trees";
                logger.setInputValue("fileName", fileName);
                logger.setID("TreeWithTraitLogger" + (multipleTrees ? "." + treeFNSteam : ""));

                logger.setInputValue("mode", "tree");
                logger.initAndValidate();

                treeLoggers.add(logger);
                elements.put(logger, null);
            }

        }

        return treeLoggers;
    }

}
