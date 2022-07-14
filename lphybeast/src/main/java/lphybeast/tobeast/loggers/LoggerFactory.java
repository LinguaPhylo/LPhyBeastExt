package lphybeast.tobeast.loggers;

import beast.core.BEASTInterface;
import beast.core.Loggable;
import beast.core.Logger;
import beast.core.StateNode;
import beast.core.util.CompoundDistribution;
import beast.evolution.tree.Tree;
import beast.evolution.tree.TreeInterface;
import beast.evolution.tree.TreeStatLogger;
import beast.evolution.tree.TreeWithMetaDataLogger;
import com.google.common.collect.Multimap;
import lphy.evolution.coalescent.SkylineCoalescent;
import lphy.evolution.coalescent.StructuredCoalescent;
import lphy.graphicalModel.Generator;
import lphy.graphicalModel.GraphicalModelNode;
import lphy.graphicalModel.RandomVariable;
import lphybeast.BEASTContext;

import java.util.*;
import java.util.stream.Collectors;

import static lphybeast.BEASTContext.*;

/**
 * A class to create all operators
 * @author Walter Xie
 */
public class LoggerFactory implements LoggerHelper {
    // state nodes
    final private List<StateNode> state;
    // a map of BEASTInterface to graphical model nodes that they represent
    final private Map<BEASTInterface, GraphicalModelNode<?>> BEASTToLPHYMap;

    // a list of extra loggables in 3 default loggers: parameter logger, screen logger, tree logger.
    final private List<Loggable> extraLoggables;
    // extra loggers from extensions
    final private List<LoggerHelper> extraLoggers;

    private CompoundDistribution[] topDist = new CompoundDistribution[3];

    String fileName = null;

    public LoggerFactory(BEASTContext context) {
        this.state = context.getState();
        this.BEASTToLPHYMap = context.getBEASTToLPHYMap();
        this.extraLoggables = context.getExtraLoggables();
        this.extraLoggers = context.getExtraLoggers();
    }

    /**
     * @param logEvery  Number of the samples logged
     * @param logFileStem  null for screen logger
     * @return    3 default loggers: parameter logger, screen logger, tree logger.
     * @see Logger
     */
    public List<Logger> createLoggers(int logEvery, String logFileStem,
                                      Multimap<BEASTInterface, GraphicalModelNode<?>> elements) {
        topDist = getTopCompoundDist(elements);

        List<Logger> loggers = new ArrayList<>();
        // reduce screen logging
        setFileName(null, false);
        loggers.add(createLogger(logEvery * 100, elements));
        // parameter logger
        setFileName(logFileStem, false);
        loggers.add(createLogger(logEvery, elements));

        // tree logger
        List<TreeInterface> trees = getTrees();
        boolean multipleTrees = trees.size() > 1;
        for (TreeInterface tree : trees) {
            TreeLoggerHelper treeLogger = new TreeLoggerCreator(tree);
            treeLogger.setFileName(logFileStem, multipleTrees);

            loggers.add(treeLogger.createLogger(logEvery, elements));
        }

        // extraLoggers, create a seperated logger each time
        for (LoggerHelper loggerHelper : extraLoggers) {
            // implement to set a different file name to the default names.
            loggerHelper.setFileName(logFileStem, multipleTrees);
            Logger logger = loggerHelper.createLogger(logEvery, elements);
            loggers.add(logger);
        }

        return loggers;
    }

    //*** default parameter/screen loggers ***//

    // screen logger if fileName is null
    public Logger createLogger(int logEvery, Multimap<BEASTInterface, GraphicalModelNode<?>> elements) {
        Logger logger = new Logger();
        logger.setInputValue("logEvery", logEvery);
        logger.setInputValue("log", getLoggables());
        if (getFileName() == null) {
            logger.setID("ScreenLogger"); // only 1 screen logger
        } else {
            logger.setInputValue("fileName", getFileName());
        }
        logger.initAndValidate();
        elements.put(logger, null);
        return logger;
    }

    public List<Loggable> getLoggables() {
        List<Loggable> nonTrees = state.stream()
                .filter(stateNode -> !(stateNode instanceof Tree))
                .collect(Collectors.toList());

        // tree height, but not in screen logging
        if ( getFileName() != null ) {
            List<TreeInterface> trees = getTrees();
            for (TreeInterface tree : trees) {
                TreeStatLogger treeStatLogger = new TreeStatLogger();
                // use default, which will log the length
                treeStatLogger.initByName("tree", tree);
                nonTrees.add(treeStatLogger);
            }
        }

        // not in screen logging
        if ( getFileName() != null ) {
            nonTrees.addAll(extraLoggables);
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

        return nonTrees;
    }

    @Override
    public String getFileName() {
        return fileName;
    }

    @Override
    public void setFileName(String fileStem, boolean isMultiple) {
        if (fileStem == null) fileName = null;
        else fileName = Objects.requireNonNull(fileStem) + ".log";
    }

    /**
     * @return   a list of {@link TreeInterface}
     */
    public List<TreeInterface> getTrees() {
        //TODO get trees from CTMC?
        return state.stream()
                .filter(stateNode -> stateNode instanceof TreeInterface)
                .map(stateNode -> (TreeInterface) stateNode)
                .sorted(Comparator.comparing(TreeInterface::getID))
                .collect(Collectors.toList());
    }

    // sorted by specific order
    private CompoundDistribution[] getTopCompoundDist(Multimap<BEASTInterface, GraphicalModelNode<?>> elements) {
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
    class TreeLoggerCreator implements TreeLoggerHelper {

        final TreeInterface tree;
        String fileName;

        public TreeLoggerCreator(TreeInterface tree) {
            this.tree = tree;
        }

        public Logger createLogger(int logEvery, Multimap<BEASTInterface, GraphicalModelNode<?>> elements) {
            TreeInterface tree = getTree();
            GraphicalModelNode graphicalModelNode = BEASTToLPHYMap.get(tree);
            Generator generator = ((RandomVariable) graphicalModelNode).getGenerator();

            boolean logMetaData = generator instanceof SkylineCoalescent ||
                    generator instanceof StructuredCoalescent; // TODO more general?

            Logger logger = new Logger();
            logger.setInputValue("logEvery", logEvery);
            if (logMetaData) {
                TreeWithMetaDataLogger treeWithMetaDataLogger = new TreeWithMetaDataLogger();
                treeWithMetaDataLogger.setInputValue("tree", tree);
                logger.setInputValue("log", treeWithMetaDataLogger);
            } else
                logger.setInputValue("log", tree);

            logger.setInputValue("fileName", getFileName());
            logger.setInputValue("mode", "tree");
            logger.initAndValidate();
            logger.setID(tree.getID() + ".treeLogger");
            elements.put(logger, null);

            return logger;
        }

        @Override
        public String getFileName() {
            return fileName;
        }

        @Override
        public void setFileName(String fileStem, boolean isMultiple) {
            if (isMultiple) // multi-partitions and unlink trees
                fileName = fileStem + "." + getTree().getID() + ".trees";
            else
                fileName = fileStem + ".trees";
        }

        @Override
        public TreeInterface getTree() {
            return tree;
        }

//    public List<Logger> createTreeLoggers(int logEvery, String logFileStem) {
//
//        List<TreeInterface> trees = getTrees();
//
//        boolean multipleTrees = trees.size() > 1;
//
//        List<Logger> treeLoggers = new ArrayList<>();
//
//        // TODO: use tree-likelihood instead, and get all trees from tree-likelihood?
//
//        for (TreeInterface tree : trees) {
//            GraphicalModelNode graphicalModelNode = BEASTToLPHYMap.get(tree);
//            Generator generator = ((RandomVariable) graphicalModelNode).getGenerator();
//
//            boolean logMetaData = generator instanceof SkylineCoalescent ||
//                    generator instanceof StructuredCoalescent;
//
//            Logger logger = new Logger();
//            logger.setInputValue("logEvery", logEvery);
//            if (logMetaData) { // TODO
//                TreeWithMetaDataLogger treeWithMetaDataLogger = new TreeWithMetaDataLogger();
//                treeWithMetaDataLogger.setInputValue("tree", tree);
//                logger.setInputValue("log", treeWithMetaDataLogger);
//            } else
//                logger.setInputValue("log", tree);
//
//            String fileName = Objects.requireNonNull(logFileStem) + ".trees";
//            if (multipleTrees) // multi-partitions and unlink trees
//                fileName = logFileStem + "_" + tree.getID() + ".trees";
//
//            logger.setInputValue("fileName", fileName);
//            logger.setInputValue("mode", "tree");
//            logger.initAndValidate();
//            logger.setID(tree.getID() + ".treeLogger");
//            treeLoggers.add(logger);
//            elements.put(logger, null);
//        }
//
//        // extra tree logger
//        // extraLoggables are used to retain all tree-likelihoods
//        for (Loggable loggable : extraLoggables) {
//            // TODO add to extraLoggers, create a seperated logger instead
//            if (loggable instanceof AncestralStateTreeLikelihood ancestralStateTreeLikelihood) {
//                // DPG: TreeWithTraitLogger
//                TreeInterface tree = ancestralStateTreeLikelihood.treeInput.get();
//
//                TreeWithTraitLogger treeWithTraitLogger = new TreeWithTraitLogger();
//                treeWithTraitLogger.setInputValue("tree", tree);
//
//                List<BEASTObject> metadata = new ArrayList<>();
//                metadata.add((AncestralStateTreeLikelihood) loggable);
//                // posterior
//                metadata.add(getPosteriorDist());
//                treeWithTraitLogger.setInputValue("metadata", metadata);
//
//                Logger logger = new Logger();
//                logger.setInputValue("logEvery", logEvery);
//                logger.setInputValue("log", treeWithTraitLogger);
//
//                String treeFNSteam = Objects.requireNonNull(logFileStem) + "_with_trait";
//                if (multipleTrees) // multi-partitions and unlink trees
//                    treeFNSteam = logFileStem + "_" + tree.getID();
//                String fileName = treeFNSteam + ".trees";
//                logger.setInputValue("fileName", fileName);
//                logger.setID("TreeWithTraitLogger" + (multipleTrees ? "." + treeFNSteam : ""));
//
//                logger.setInputValue("mode", "tree");
//                logger.initAndValidate();
//
//                treeLoggers.add(logger);
//                elements.put(logger, null);
//            }
//
//        }
//
//        return treeLoggers;
//    }
    }

}
