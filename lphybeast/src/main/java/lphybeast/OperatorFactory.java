package lphybeast;

import beast.core.BEASTInterface;
import beast.core.BEASTObject;
import beast.core.Operator;
import beast.core.StateNode;
import beast.core.parameter.BooleanParameter;
import beast.core.parameter.IntegerParameter;
import beast.core.parameter.RealParameter;
import beast.evolution.operators.*;
import beast.evolution.tree.Tree;
import com.google.common.collect.Multimap;
import lphy.core.distributions.Dirichlet;
import lphy.core.distributions.IID;
import lphy.core.distributions.RandomComposition;
import lphy.evolution.birthdeath.SimFBDAge;
import lphy.evolution.tree.TimeTree;
import lphy.graphicalModel.GenerativeDistribution;
import lphy.graphicalModel.GraphicalModelNode;
import lphy.graphicalModel.RandomVariable;
import lphy.graphicalModel.Value;
import lphy.util.LoggerUtils;

import java.util.*;

import static lphybeast.BEASTContext.getOperatorWeight;

/**
 * A class to create all operators
 * @author Walter Xie
 */
public class OperatorFactory {
    // state nodes
    final private List<StateNode> state;
    // a list of extra beast elements in the keys,
    // with a pointer to the graphical model node that caused their production
    final private Multimap<BEASTInterface, GraphicalModelNode<?>> elements;
    // a map of BEASTInterface to graphical model nodes that they represent
    final private Map<BEASTInterface, GraphicalModelNode<?>> BEASTToLPHYMap;

    // a list of beast state nodes to skip the automatic operator creation for.
    final private Set<StateNode> skipOperators;
    // extra operators either for default or from extensions
    final private List<Operator> extraOperators;

    public OperatorFactory(BEASTContext context) {
        this.state = context.getState();
        this.elements = context.getElements();
        this.BEASTToLPHYMap = context.getBEASTToLPHYMap();
        this.skipOperators = context.getSkipOperators();
        this.extraOperators = context.getExtraOperators();
    }

    /**
     * @return  a list of {@link Operator}.
     */
    public List<Operator> createOperators() {

        List<Operator> operators = new ArrayList<>();

        for (StateNode stateNode : state) {
            if (!skipOperators.contains(stateNode)) {
                if (stateNode instanceof RealParameter realParameter) {
                    Operator operator = createBEASTOperator(realParameter);
                    if (operator != null) operators.add(operator);
                } else if (stateNode instanceof IntegerParameter integerParameter) {
                    operators.add(createBEASTOperator(integerParameter));
                } else if (stateNode instanceof BooleanParameter booleanParameter) {
                    operators.add(createBitFlipOperator(booleanParameter));
                } else if (stateNode instanceof Tree tree) {
                    operators.add(createTreeScaleOperator(tree));
                    operators.add(createRootHeightOperator(tree));
                    operators.add(createExchangeOperator(tree, true));
                    operators.add(createExchangeOperator(tree, false));
                    if (!isSampledAncestor(tree)) operators.add(createSubtreeSlideOperator((Tree) stateNode));
                    operators.add(createTreeUniformOperator(tree));
                    if (!isSampledAncestor(tree)) operators.add(createWilsonBaldingOperator(tree));
                }
            }
        }

        operators.addAll(extraOperators);
        operators.sort(Comparator.comparing(BEASTObject::getID));

        return operators;
    }

    //*** parameter operators ***//

    private Operator createBEASTOperator(RealParameter parameter) {

        Collection<GraphicalModelNode<?>> nodes = elements.get(parameter);

        if (nodes.stream().anyMatch(node -> node instanceof RandomVariable)) {

            GraphicalModelNode graphicalModelNode = (GraphicalModelNode)nodes.stream().filter(node -> node instanceof RandomVariable).toArray()[0];

            RandomVariable<?> variable = (RandomVariable<?>) graphicalModelNode;

            Operator operator;
            GenerativeDistribution generativeDistribution = variable.getGenerativeDistribution();

            if (generativeDistribution instanceof Dirichlet ||
                    (generativeDistribution instanceof IID &&
                            ((IID<?>) generativeDistribution).getBaseDistribution() instanceof Dirichlet) ) {
                Double[] value = (Double[]) variable.value();
                operator = new DeltaExchangeOperator();
                operator.setInputValue("parameter", parameter);
                operator.setInputValue("weight", getOperatorWeight(parameter.getDimension() - 1));
                operator.setInputValue("delta", 1.0 / value.length);
                operator.initAndValidate();
                operator.setID(parameter.getID() + ".deltaExchange");
            } else {
                operator = new ScaleOperator();
                operator.setInputValue("parameter", parameter);
                operator.setInputValue("weight", getOperatorWeight(parameter.getDimension()));
                operator.setInputValue("scaleFactor", 0.75);
                operator.initAndValidate();
                operator.setID(parameter.getID() + ".scale");
            }
            elements.put(operator, null);
            return operator;
        } else {
            LoggerUtils.log.severe("No LPhy random variable associated with beast state node " + parameter.getID());
            return null;
        }
    }

    private Operator createBEASTOperator(IntegerParameter parameter) {
        RandomVariable<?> variable = (RandomVariable<?>) BEASTToLPHYMap.get(parameter);

        Operator operator;
        if (variable.getGenerativeDistribution() instanceof RandomComposition) {
            System.out.println("Constructing operator for randomComposition");
            operator = new DeltaExchangeOperator();
            operator.setInputValue("intparameter", parameter);
            operator.setInputValue("weight", getOperatorWeight(parameter.getDimension() - 1));
            operator.setInputValue("delta", 2.0);
            operator.setInputValue("integer", true);
            operator.initAndValidate();
            operator.setID(parameter.getID() + ".deltaExchange");
        } else {
            operator = new IntRandomWalkOperator();
            operator.setInputValue("parameter", parameter);
            operator.setInputValue("weight", getOperatorWeight(parameter.getDimension()));

            // TODO implement an optimizable int random walk that uses a reflected Poisson distribution for the jump size with the mean of the Poisson being the optimizable parameter
            operator.setInputValue("windowSize", 1);
            operator.initAndValidate();
            operator.setID(parameter.getID() + ".randomWalk");
        }
        elements.put(operator, null);
        return operator;
    }

    private Operator createBitFlipOperator(BooleanParameter parameter) {
        Operator operator = new BitFlipOperator();
        operator.setInputValue("parameter", parameter);
        operator.setInputValue("weight", getOperatorWeight(parameter.getDimension()));
        operator.initAndValidate();
        operator.setID(parameter.getID() + ".bitFlip");

        return operator;
    }

    //*** tree operators ***//
    //TODO rm this to make operator generation extensible
    private boolean isSampledAncestor(Tree tree) {
        return (((Value<TimeTree>)BEASTToLPHYMap.get(tree)).getGenerator() instanceof SimFBDAge);
    }

    private Operator createTreeScaleOperator(Tree tree) {

        ScaleOperator operator = isSampledAncestor(tree) ? new SAScaleOperator() : new ScaleOperator();

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

    private Operator createRootHeightOperator(Tree tree) {
        ScaleOperator operator = isSampledAncestor(tree) ? new SAScaleOperator() : new ScaleOperator();
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

    private Operator createTreeUniformOperator(Tree tree) {
        Operator uniform = isSampledAncestor(tree) ? new SAUniform() : new Uniform();
        uniform.setInputValue("tree", tree);
        uniform.setInputValue("weight", getOperatorWeight(tree.getInternalNodeCount()));
        uniform.initAndValidate();
        uniform.setID(tree.getID() + "." + "uniform");
        elements.put(uniform, null);

        return uniform;
    }

    private Operator createSubtreeSlideOperator(Tree tree) {
        SubtreeSlide subtreeSlide = new SubtreeSlide();
        subtreeSlide.setInputValue("tree", tree);
        subtreeSlide.setInputValue("weight", getOperatorWeight(tree.getInternalNodeCount()));
        subtreeSlide.setInputValue("size", tree.getRoot().getHeight() / 10.0);
        subtreeSlide.initAndValidate();
        subtreeSlide.setID(tree.getID() + "." + "subtreeSlide");
        elements.put(subtreeSlide, null);

        return subtreeSlide;
    }

    private Operator createWilsonBaldingOperator(Tree tree) {
        Operator wilsonBalding = isSampledAncestor(tree) ? new SAWilsonBalding() : new WilsonBalding();
        wilsonBalding.setInputValue("tree", tree);
        wilsonBalding.setInputValue("weight", getOperatorWeight(tree.getInternalNodeCount()));
        wilsonBalding.initAndValidate();
        wilsonBalding.setID(tree.getID() + "." + "wilsonBalding");
        elements.put(wilsonBalding, null);

        return wilsonBalding;
    }

    private Operator createExchangeOperator(Tree tree, boolean isNarrow) {
        Exchange exchange = isSampledAncestor(tree) ? new SAExchange() : new Exchange();
        exchange.setInputValue("tree", tree);
        exchange.setInputValue("weight", getOperatorWeight(tree.getInternalNodeCount()));
        exchange.setInputValue("isNarrow", isNarrow);
        exchange.initAndValidate();
        exchange.setID(tree.getID() + "." + ((isNarrow) ? "narrow" : "wide") + "Exchange");
        elements.put(exchange, null);

        return exchange;
    }

}
