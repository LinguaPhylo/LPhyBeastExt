package lphybeast;

import beast.core.Operator;
import beast.evolution.tree.Tree;

import java.util.ArrayList;
import java.util.List;

/**
 * Defines a family of tree operators.
 * Use constructor to assign <code>Multimap<BEASTInterface, GraphicalModelNode<?>> elements</code>.
 * @author Walter Xie
 */
public interface TreeOperatorStrategy {

    default List<Operator> createTreeOperators(Tree tree) {
        List<Operator> operators = new ArrayList<>();

        operators.add(createTreeScaleOperator(tree));
        operators.add(createRootHeightOperator(tree));
        operators.add(createExchangeOperator(tree, true));
        operators.add(createExchangeOperator(tree, false));
        operators.add(createTreeUniformOperator(tree));

        operators.add(createSubtreeSlideOperator(tree));
        operators.add(createWilsonBaldingOperator(tree));

        return operators;
    }

    Operator createTreeScaleOperator(Tree tree);

    Operator createRootHeightOperator(Tree tree);

    Operator createTreeUniformOperator(Tree tree);

    Operator createExchangeOperator(Tree tree, boolean isNarrow);

    Operator createSubtreeSlideOperator(Tree tree);

    Operator createWilsonBaldingOperator(Tree tree);

    String getName();

}
