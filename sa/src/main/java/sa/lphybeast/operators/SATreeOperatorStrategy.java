package sa.lphybeast.operators;

import beast.base.core.BEASTInterface;
import beast.base.evolution.operator.SubtreeSlide;
import beast.base.evolution.tree.Tree;
import beast.base.inference.Operator;
import lphy.evolution.birthdeath.FossilBirthDeathTree;
import lphy.evolution.birthdeath.SimFBDAge;
import lphy.evolution.birthdeath.SimFossilsPoisson;
import lphy.evolution.tree.TimeTree;
import lphy.graphicalModel.GraphicalModelNode;
import lphy.graphicalModel.Value;
import lphybeast.BEASTContext;
import lphybeast.tobeast.operators.OperatorFactory;
import lphybeast.tobeast.operators.TreeOperatorStrategy;
import sa.evolution.operators.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static lphybeast.BEASTContext.getOperatorWeight;

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
            return timeTreeValue.getGenerator() instanceof SimFBDAge ||  // simFBDAge.lphy
                    timeTreeValue.getGenerator() instanceof FossilBirthDeathTree ||  // simFossilsCompact.lphy
                    timeTreeValue.getGenerator() instanceof SimFossilsPoisson;   // simFossils.lphy
        else
            throw new IllegalArgumentException("BEAST tree " + tree + " must map to Value<TimeTree> !");
    }

    @Override
    public List<Operator> createTreeOperators(Tree tree, BEASTContext context) {
        List<Operator> operators = new ArrayList<>();

        operators.add(OperatorFactory.createTreeScaleOperator(tree, context));
        operators.add(OperatorFactory.createRootHeightOperator(tree, context));
        operators.add(OperatorFactory.createExchangeOperator(tree, context, true));
        operators.add(OperatorFactory.createExchangeOperator(tree, context, false));
        operators.add(OperatorFactory.createTreeUniformOperator(tree, context));
        //https://github.com/CompEvol/sampled-ancestors/blob/master/examples/fossil.xml
        operators.add(OperatorFactory.createWilsonBaldingOperator(tree, context));
        operators.add(createLeafToSampledAncestorJumpOperator(tree, context));
        return operators;
    }

    private Operator createLeafToSampledAncestorJumpOperator(Tree tree, BEASTContext context) {
        Operator leafToSA = new LeafToSampledAncestorJump();
        leafToSA.setInputValue("tree", tree);
        leafToSA.setInputValue("weight", getOperatorWeight(tree.getInternalNodeCount()));
        leafToSA.initAndValidate();
        leafToSA.setID(tree.getID() + "." + "leafToSA");
        context.getElements().put(leafToSA, null);
        return leafToSA;
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
