package lphybeast;

import beast.core.Loggable;
import beast.core.Logger;

import java.io.PrintStream;

/**
 * Wrapper to handle the extra content required in beast logging,
 * such as Mascot StructuredTreeLogger
 * @author Walter Xie
 */
public abstract class ExtraLogger implements Loggable {

    protected Loggable loggable;
    private boolean multiTrees = false;

    /**
     * Use the BEAST class in the package and {@code super},
     * such as Mascot.
     * @param loggable  A specific BEAST class
     */
    public ExtraLogger(Loggable loggable) {
        this.loggable = loggable;
    }

    public Loggable getLoggable() {
        return loggable;
    }

    /**
     * Create a tree logger containing the extra contents required by a specific package,
     * such as Mascot StructuredTreeLogger.
     * @param logEvery         log every for this logger
     * @param fileNameStem     non null string to determine the output file name,
     *                         prefer xml file name stem. Because getID() is often null.
     * @return BEAST {@link Logger}
     */
    public abstract Logger createExtraLogger(int logEvery, String fileNameStem);

    /**
     * @return true, if there is more than 1 tree for this logger.
     *         For example, multi-partitions and unlink trees.
     */
    public boolean hasMultiTrees() {
        return multiTrees;
    }

    public void setMultiTrees(boolean multiTrees) {
        this.multiTrees = multiTrees;
    }

    @Override
    public void init(PrintStream printStream) {
        loggable.init(printStream);
    }

    @Override
    public void close(PrintStream printStream) {
        loggable.close(printStream);
    }

    @Override
    public void log(long l, PrintStream printStream) {
        loggable.log(l, printStream);
    }
}
