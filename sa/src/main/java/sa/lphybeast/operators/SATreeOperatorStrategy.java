package sa.lphybeast.operators;

import beast.core.BEASTInterface;
import beast.core.Operator;
import beast.evolution.operators.*;
import beast.evolution.tree.Tree;
import com.google.common.collect.Multimap;
import lphy.evolution.birthdeath.SimFBDAge;
import lphy.evolution.tree.TimeTree;
import lphy.graphicalModel.GraphicalModelNode;
import lphy.graphicalModel.Value;
import lphybeast.BEASTContext;
import lphybeast.TreeOperatorStrategy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Walter Xie
 * @author Alexei Drommand
 */
public class SATreeOperatorStrategy implements TreeOperatorStrategy {

    public SATreeOperatorStrategy() {
    }

    @Override
    public boolean applyStrategyToTree(Tree tree, BEASTContext context) {
        Map<BEASTInterface, GraphicalModelNode<?>> BEASTToLPHYMap = context.getBEASTToLPHYMap();
        GraphicalModelNode<TimeTree> graphicalModelNode = (GraphicalModelNode<TimeTree>) BEASTToLPHYMap.get(tree);
        if (graphicalModelNode instanceof Value<TimeTree> timeTreeValue)
            return timeTreeValue.getGenerator() instanceof SimFBDAge;
        else
            throw new IllegalArgumentException("BEAST tree " + tree + " must map to Value<TimeTree> !");
    }

    @Override
    public List<Operator> createTreeOperators(Tree tree, BEASTContext context) {
        List<Operator> operators = new ArrayList<>();

        Multimap<BEASTInterface, GraphicalModelNode<?>> elements = context.getElements();
        operators.add(createTreeScaleOperator(tree, elements));
        operators.add(createRootHeightOperator(tree, elements));
        operators.add(createExchangeOperator(tree, true, elements));
        operators.add(createExchangeOperator(tree, false, elements));
        operators.add(createTreeUniformOperator(tree, elements));

//        if (!isSampledAncestor(tree)) operators.add(createWilsonBaldingOperator(tree));
//        if (!isSampledAncestor(tree)) operators.add(createSubtreeSlideOperator(tree));
        return operators;
    }

    @Override
    public Operator getScaleOperator() {
        return new SAScaleOperator();
    }

    @Override
    public Operator getUniformOperator() {
        return new SAUniform();
    }

    @Override
    public Operator getExchangeOperator() {
        return new SAExchange();
    }

    @Override
    public Operator getSubtreeSlideOperator() {
        return new SubtreeSlide();
    }

    @Override
    public Operator getWilsonBaldingOperator() {
        return new SAWilsonBalding();
    }

    @Override
    public String getName() {
        return "sampled-ancestor tree operator strategy";
    }

}
