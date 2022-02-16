package mascot.lphybeast.tobeast.loggers;

import beast.core.Logger;
import beast.evolution.tree.TreeInterface;
import beast.mascot.distribution.Mascot;
import beast.mascot.logger.StructuredTreeLogger;
import lphybeast.ExtraLogger;

import java.util.Objects;

/**
 * @author Walter Xie
 */
public class MascotExtraTreeLogger extends ExtraLogger {

    public MascotExtraTreeLogger(Mascot mascot) {
        super(mascot);
    }

    @Override
    public Logger createExtraLogger(int logEvery, String fileNameStem) {
        // Mascot StructuredTreeLogger
        TreeInterface tree = ((Mascot) loggable).treeInput.get();

        StructuredTreeLogger structuredTreeLogger = new StructuredTreeLogger();
        // not logging tree directly
        structuredTreeLogger.setInputValue("mascot", loggable);

        Logger logger = new Logger();
        logger.setInputValue("logEvery", logEvery);
        logger.setInputValue("log", structuredTreeLogger);

        String treeFNSteam = Objects.requireNonNull(fileNameStem);
        // ((Mascot) loggable).getID() == null
        if (hasMultiTrees()) // multi-partitions and unlink trees
            treeFNSteam = fileNameStem + "_" + tree.getID();
        String fileName = treeFNSteam + ".mascot.trees";
        logger.setInputValue("fileName", fileName);
        logger.setID("StructuredTreeLogger" + (hasMultiTrees() ? "." + treeFNSteam : ""));

        logger.setInputValue("mode", "tree");
        logger.initAndValidate();

        return logger;
    }
}
