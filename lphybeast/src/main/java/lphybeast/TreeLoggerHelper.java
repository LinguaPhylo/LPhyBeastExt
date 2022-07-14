package lphybeast;

import beast.core.Loggable;
import beast.evolution.tree.TreeInterface;

import java.util.List;

/**
 * Helper to create a tree logger, may contain a simple tree,
 * or customised logger e.g. {@link beast.evolution.tree.TreeWithMetaDataLogger}.
 * Usually one tree one logger, which is different convention logging parameters.
 * @author Walter Xie
 */
public interface TreeLoggerHelper extends LoggerHelper  {


    default List<Loggable> getLoggables() {
        return null; // use getTree()
    }

    /**
     * @return  the tree to log, usually one tree in one logger
     */
    TreeInterface getTree();
}
