package lphybeast;

import beast.core.BEASTInterface;
import beast.core.Operator;
import beast.evolution.operators.*;
import beast.evolution.tree.Tree;
import com.google.common.collect.Multimap;
import lphy.graphicalModel.GraphicalModelNode;

import static lphybeast.BEASTContext.getOperatorWeight;

/**
 * @author Walter Xie
 * @author Alexei Drommand
 */
public class DefaultTreeOperatorStrategy implements TreeOperatorStrategy {

    final Multimap<BEASTInterface, GraphicalModelNode<?>> elements;

    public DefaultTreeOperatorStrategy(Multimap<BEASTInterface, GraphicalModelNode<?>> elements) {
        this.elements = elements;
    }

    //    @Override
//    public List<Operator> createTreeOperators(Tree tree) {
//        List<Operator> operators = new ArrayList<>();
//        operators.add(createTreeScaleOperator(tree));
//        operators.add(createRootHeightOperator(tree));
//        operators.add(createExchangeOperator(tree, true));
//        operators.add(createExchangeOperator(tree, false));
//        operators.add(createTreeUniformOperator(tree));
//        if (!isSampledAncestor(tree)) operators.add(createWilsonBaldingOperator(tree));
//        if (!isSampledAncestor(tree)) operators.add(createSubtreeSlideOperator(tree));
//        return operators;
//    }

    @Override
    public Operator createTreeScaleOperator(Tree tree) {
        ScaleOperator operator = new ScaleOperator();//isSampledAncestor(tree) ? new SAScaleOperator() : new ScaleOperator();

        operator.setInputValue("tree", tree);
        operator.setInputValue("scaleFactor", 0.75);
        // set the upper of the scale factor
        operator.setInputValue("upper", 0.975);
        operator.setInputValue("weight", getOperatorWeight(tree.getInternalNodeCount()));
        operator.initAndValidate();
        operator.setID(tree.getID() + "." + "scale");
        elements.put(operator, null);

        return operator;
    }

    @Override
    public Operator createRootHeightOperator(Tree tree) {
        ScaleOperator operator = new ScaleOperator();//isSampledAncestor(tree) ? new SAScaleOperator() : new ScaleOperator();
        operator.setInputValue("tree", tree);
        operator.setInputValue("rootOnly", true);
        operator.setInputValue("scaleFactor", 0.75);
        // set the upper of the scale factor
        operator.setInputValue("upper", 0.975);
        operator.setInputValue("weight", getOperatorWeight(1));
        operator.initAndValidate();
        operator.setID(tree.getID() + "." + "rootAgeScale");
        elements.put(operator, null);

        return operator;
    }

    @Override
    public Operator createTreeUniformOperator(Tree tree) {
        Operator uniform = new Uniform();//isSampledAncestor(tree) ? new SAUniform() : new Uniform();
        uniform.setInputValue("tree", tree);
        uniform.setInputValue("weight", getOperatorWeight(tree.getInternalNodeCount()));
        uniform.initAndValidate();
        uniform.setID(tree.getID() + "." + "uniform");
        elements.put(uniform, null);

        return uniform;
    }

    @Override
    public Operator createExchangeOperator(Tree tree, boolean isNarrow) {
        Exchange exchange = new Exchange();//isSampledAncestor(tree) ? new SAExchange() : new Exchange();
        exchange.setInputValue("tree", tree);
        exchange.setInputValue("weight", getOperatorWeight(tree.getInternalNodeCount()));
        exchange.setInputValue("isNarrow", isNarrow);
        exchange.initAndValidate();
        exchange.setID(tree.getID() + "." + ((isNarrow) ? "narrow" : "wide") + "Exchange");
        elements.put(exchange, null);

        return exchange;
    }

    @Override
    public Operator createSubtreeSlideOperator(Tree tree) {
        SubtreeSlide subtreeSlide = new SubtreeSlide();
        subtreeSlide.setInputValue("tree", tree);
        subtreeSlide.setInputValue("weight", getOperatorWeight(tree.getInternalNodeCount()));
        subtreeSlide.setInputValue("size", tree.getRoot().getHeight() / 10.0);
        subtreeSlide.initAndValidate();
        subtreeSlide.setID(tree.getID() + "." + "subtreeSlide");
        elements.put(subtreeSlide, null);

        return subtreeSlide;
    }

    @Override
    public Operator createWilsonBaldingOperator(Tree tree) {
        Operator wilsonBalding = new WilsonBalding();//isSampledAncestor(tree) ? new SAWilsonBalding() : new WilsonBalding();
        wilsonBalding.setInputValue("tree", tree);
        wilsonBalding.setInputValue("weight", getOperatorWeight(tree.getInternalNodeCount()));
        wilsonBalding.initAndValidate();
        wilsonBalding.setID(tree.getID() + "." + "wilsonBalding");
        elements.put(wilsonBalding, null);

        return wilsonBalding;
    }

    @Override
    public String getName() {
        return "default tree operator strategy";
    }
}
