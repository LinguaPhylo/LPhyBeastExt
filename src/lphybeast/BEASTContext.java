package lphybeast;

import beast.core.Loggable;
import beast.core.*;
import beast.core.parameter.BooleanParameter;
import beast.core.parameter.IntegerParameter;
import beast.core.parameter.Parameter;
import beast.core.parameter.RealParameter;
import beast.core.util.CompoundDistribution;
import beast.evolution.alignment.Alignment;
import beast.evolution.alignment.Taxon;
import beast.evolution.datatype.DataType;
import beast.evolution.likelihood.AncestralStateTreeLikelihood;
import beast.evolution.operators.*;
import beast.evolution.substitutionmodel.Frequencies;
import beast.evolution.tree.*;
import beast.mascot.distribution.Mascot;
import beast.mascot.logger.StructuredTreeLogger;
import beast.math.distributions.ParametricDistribution;
import beast.math.distributions.Prior;
import beast.util.XMLProducer;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import feast.function.Concatenate;
import feast.function.Slice;
import jebl.evolution.sequences.SequenceType;
import lphy.app.Symbols;
import lphy.core.LPhyParser;
import lphy.core.distributions.Dirichlet;
import lphy.core.distributions.IID;
import lphy.core.distributions.RandomComposition;
import lphy.core.functions.ElementsAt;
import lphy.evolution.birthdeath.SimFBDAge;
import lphy.evolution.coalescent.SkylineCoalescent;
import lphy.evolution.coalescent.StructuredCoalescent;
import lphy.evolution.tree.TimeTree;
import lphy.graphicalModel.*;
import lphy.utils.LoggerUtils;
import lphybeast.registry.ClassesRegistry;
import lphybeast.tobeast.values.ValueToParameter;
import org.xml.sax.SAXException;
import outercore.util.BEASTVector;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static java.lang.Math.toIntExact;

public class BEASTContext {

    public static final String POSTERIOR_ID = "posterior";
    public static final String PRIOR_ID = "prior";
    public static final String LIKELIHOOD_ID = "likelihood";

    //*** registry ***//

    List<ValueToBEAST> valueToBEASTList = new ArrayList<>();
    //use LinkedHashMap to keep inserted ordering, so the first matching converter is used.
    Map<Class, GeneratorToBEAST> generatorToBEASTMap = new LinkedHashMap<>();
    // LPhy SequenceType => BEAST DataType
    Map<SequenceType, DataType> dataTypeMap = new ConcurrentHashMap<>();

    //*** to BEAST ***//

    List<StateNode> state = new ArrayList<>();

    // a list of extra beast elements in the keys,
    // with a pointer to the graphical model node that caused their production
    private Multimap<BEASTInterface, GraphicalModelNode<?>> elements = HashMultimap.create();
    List<StateNodeInitialiser> inits = new ArrayList<>();

    // a map of graphical model nodes to a list of equivalent BEASTInterface objects
    private Map<GraphicalModelNode<?>, BEASTInterface> beastObjects = new HashMap<>();

    // a map of BEASTInterface to graphical model nodes that they represent
    private Map<BEASTInterface, GraphicalModelNode<?>> BEASTToLPHYMap = new HashMap<>();

    // a list of beast state nodes to skip the automatic operator creation for.
    private Set<StateNode> skipOperators = new HashSet<>();

    private List<Operator> extraOperators = new ArrayList<>();
    private List<Loggable> extraLoggables = new ArrayList<>();

    SortedMap<String, Taxon> allTaxa = new TreeMap<>();

    LPhyParser parser;


    public BEASTContext(LPhyParser phyParser) {
        parser = phyParser;

        List<ClassesRegistry> registryList = ClassesRegistry.getRegistryClasses();

        for (ClassesRegistry registry : registryList) {
            final Class<?>[] valuesToBEASTs = registry.getValuesToBEASTs();
            final Class<?>[] generatorToBEASTs = registry.getGeneratorToBEASTs();
            final Map<SequenceType, DataType> dataTypeMap = registry.getDataTypeMap();

            registerValues(valuesToBEASTs);
            registerGenerators(generatorToBEASTs);
            registerDataTypes(dataTypeMap);
        }

        System.out.println(valueToBEASTList.size() + " ValuesToBEAST = " + valueToBEASTList);
        System.out.println(generatorToBEASTMap.size() + " GeneratorToBEAST = " + generatorToBEASTMap);
        System.out.println(dataTypeMap.size() + " Data Type = " + dataTypeMap);
    }

    private void registerValues(final Class<?>[] valuesToBEASTs) {
        for (Class<?> c : valuesToBEASTs) {
            try {
                // https://docs.oracle.com/javase/9/docs/api/java/lang/Class.html#newInstance--
                ValueToBEAST<?,?> valueToBEAST = (ValueToBEAST<?,?>) c.getDeclaredConstructor().newInstance();
                if (valueToBEASTList.contains(valueToBEAST))
                    LoggerUtils.log.severe(valueToBEAST + " exists in the valueToBEASTList !");
                valueToBEASTList.add(valueToBEAST);
            } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
                e.printStackTrace();
            }
        }
    }

    private void registerGenerators(final Class<?>[] generatorToBEASTs) {

        for (Class<?> c : generatorToBEASTs) {
            try {
                // https://docs.oracle.com/javase/9/docs/api/java/lang/Class.html#newInstance--
                GeneratorToBEAST<?,?> generatorToBEAST = (GeneratorToBEAST<?,?>) c.getDeclaredConstructor().newInstance();
                if (generatorToBEASTMap.containsKey(generatorToBEAST))
                    LoggerUtils.log.severe(generatorToBEAST + " exists in the generatorToBEASTMap !");
                generatorToBEASTMap.put(generatorToBEAST.getGeneratorClass(), generatorToBEAST);
            } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
                e.printStackTrace();
            }
        }
    }

    private void registerDataTypes(final Map<SequenceType, DataType> dataTypeMap) {
        for (Map.Entry<SequenceType, DataType> entry : dataTypeMap.entrySet()) {
            if (dataTypeMap.containsKey(entry.getKey()))
                LoggerUtils.log.severe(entry.getKey() + " exists in the dataTypeMap !");
            this.dataTypeMap.put(entry.getKey(), entry.getValue());
        }
    }

    public Map<SequenceType, DataType> getDataTypeMap() {
        return this.dataTypeMap;
    }

    public BEASTInterface getBEASTObject(GraphicalModelNode<?> node) {

        // Q=jukesCantor(), rateMatrix.getMeanRate() is null
        if (node == null) return null;

        if (node instanceof Value) {
            Value value = (Value)node;
            if (!value.isAnonymous()) {
                BEASTInterface beastInterface = getBEASTObject(value.getId());
                // cannot be Alignment, otherwise getBEASTObject(value.getId()) makes data clamping not working;
                // it will get simulated Alignment even though data is clamped.
                if (beastInterface != null) {
                    if (beastInterface instanceof BEASTVector) {
                        List<BEASTInterface> beastInterfaceList = ((BEASTVector)beastInterface).getObjectList();

                        if ( !(beastInterfaceList.get(0) instanceof Alignment) )
                            return beastInterface;

                    } else if ( !(beastInterface instanceof Alignment) ) {
                        return beastInterface;
                    }
                }
            }
        }

        // have to use this for data clamping
        BEASTInterface beastInterface = beastObjects.get(node);

        if (beastInterface != null) {
            return beastInterface;
        } else {
            String id = node.getUniqueId();
            String[] parts = id.split(VectorUtils.INDEX_SEPARATOR);
            if (parts.length == 2) {
                int index = Integer.parseInt(parts[1]);
                Slice slice = createSliceFromVector(node, parts[0], index);
                beastObjects.put(node, slice);
                return slice;
            }
        }
        
        if (node instanceof SliceValue) {
            SliceValue sliceValue = (SliceValue) node;
            return handleSliceRequest(sliceValue);
        }
        return null;
    }

    public Slice createSliceFromVector(GraphicalModelNode node, String id, int index) {

        BEASTInterface parentNode = getBEASTObject(Symbols.getCanonical(id));

        Slice slice = SliceFactory.createSlice(parentNode, index,
                Symbols.getCanonical(id) + VectorUtils.INDEX_SEPARATOR + index);
        addToContext(node, slice);
        return slice;

    }

    boolean byslice = false;

    /**
     * returns a logical slice based on the given slice value, as long as the value to be sliced is already available.
     *
     * @param sliceValue the slice value that needs a beast equivalent
     * @return
     */
    public BEASTInterface handleSliceRequest(SliceValue sliceValue) {

        BEASTInterface slicedBEASTValue = beastObjects.get(sliceValue.getSlicedValue());


        if (slicedBEASTValue != null) {
            if (!(slicedBEASTValue instanceof Concatenate)) {
                Slice slice = SliceFactory.createSlice(slicedBEASTValue, sliceValue.getIndex(), sliceValue.getId());
                addToContext(sliceValue, slice);
                return slice;
            } else {
                // handle by concatenating
                List<Function> parts = ((Concatenate) slicedBEASTValue).functionsInput.get();
                Function slice = parts.get(sliceValue.getIndex());
                addToContext(sliceValue, (BEASTInterface) slice);
                return (BEASTInterface) slice;
            }
        } else return null;
    }


    public BEASTInterface getBEASTObject(String id) {
        for (BEASTInterface beastInterface : elements.keySet()) {
            if (id.equals(beastInterface.getID())) return beastInterface;
        }

        for (BEASTInterface beastInterface : beastObjects.values()) {
            if (beastInterface.getID() !=  null && id.equals(beastInterface.getID().equals(id))) return beastInterface;
        }
        return null;
    }

    /**
     * This function will retrieve the beast object for this value and return it if it is a RealParameter,
     * or convert it to a RealParameter if it is an IntegerParameter and replace the original integer parameter in the relevant stores.
     *
     * @param value
     * @return the RealParameter associated with this value if it exists, or can be coerced. Has a side-effect if coercion occurs.
     */
    public RealParameter getAsRealParameter(Value value) {
        Parameter param = (Parameter) beastObjects.get(value);
        if (param instanceof RealParameter) return (RealParameter) param;
        if (param instanceof IntegerParameter) {
            if (param.getDimension() == 1) {

                RealParameter newParam = createRealParameter(param.getID(), ((IntegerParameter) param).getValue());
                removeBEASTObject((BEASTInterface) param);
                addToContext(value, newParam);
                return newParam;
            } else {
                Double[] values = new Double[param.getDimension()];
                for (int i = 0; i < values.length; i++) {
                    values[i] = ((IntegerParameter) param).getValue(i).doubleValue();
                }

                RealParameter newParam = createRealParameter(param.getID(), values);
                removeBEASTObject((BEASTInterface) param);
                addToContext(value, newParam);
                return newParam;

            }
        }
        throw new RuntimeException("No coercable parameter found.");
    }

    public IntegerParameter getAsIntegerParameter(Value value) {
        Parameter param = (Parameter) beastObjects.get(value);
        if (param instanceof IntegerParameter) return (IntegerParameter) param;
        if (param instanceof RealParameter) {
            if (param.getDimension() == 1) {

                IntegerParameter newParam = createIntegerParameter(param.getID(), (int) Math.round(((RealParameter) param).getValue()));
                removeBEASTObject((BEASTInterface) param);
                addToContext(value, newParam);
                return newParam;
            } else {
                Integer[] values = new Integer[param.getDimension()];
                for (int i = 0; i < values.length; i++) {
                    values[i] = ((RealParameter) param).getValue(i).intValue();
                }

                IntegerParameter newParam = createIntegerParameter(param.getID(), values);
                removeBEASTObject((BEASTInterface) param);
                addToContext(value, newParam);
                return newParam;

            }
        }
        throw new RuntimeException("No coercable parameter found.");
    }


    public GraphicalModelNode getGraphicalModelNode(BEASTInterface beastInterface) {
        return BEASTToLPHYMap.get(beastInterface);
    }

    public void addBEASTObject(BEASTInterface newBEASTObject, GraphicalModelNode graphicalModelNode) {
        elements.put(newBEASTObject, graphicalModelNode);
    }

    /**
     * @param stateNode the state node to be added
     * @param graphicalModelNode the graphical model node that this state node corresponds to, or represents a part of
     */
    public void addStateNode(StateNode stateNode, GraphicalModelNode graphicalModelNode, boolean createOperators) {
        if (!state.contains(stateNode)) {
            elements.put(stateNode, graphicalModelNode);
            state.add(stateNode);
        }
        if (!createOperators) skipOperators.add(stateNode);
    }

    public void removeBEASTObject(BEASTInterface beastObject) {
        elements.removeAll(beastObject);
        BEASTToLPHYMap.remove(beastObject);
        if (beastObject instanceof StateNode) state.remove(beastObject);
        if (beastObject instanceof StateNode) skipOperators.remove(beastObject);

        GraphicalModelNode matchingKey = null;
        for (GraphicalModelNode key : beastObjects.keySet()) {
            if (getBEASTObject(key) == beastObject) {
                matchingKey = key;
                break;
            }
        }
        if (matchingKey != null) beastObjects.remove(matchingKey);

        // it may be in extraLoggables
        extraLoggables.remove(beastObject);
    }

    public static RealParameter createRealParameter(Double[] value) {
        return new RealParameter(value);
    }

    public static RealParameter createRealParameter(double value) {
        return createRealParameter(null, value);
    }

    public static IntegerParameter createIntegerParameter(String id, int value) {
        IntegerParameter parameter = new IntegerParameter();
        parameter.setInputValue("value", value);
        parameter.initAndValidate();
        if (id != null) parameter.setID(id);

        return parameter;
    }

    public static IntegerParameter createIntegerParameter(String id, Integer[] value) {
        IntegerParameter parameter = new IntegerParameter();
        parameter.setInputValue("value", Arrays.asList(value));
        parameter.initAndValidate();
        if (id != null) parameter.setID(id);

        return parameter;
    }


    public static RealParameter createRealParameter(String id, double value) {
        RealParameter parameter = new RealParameter();
        parameter.setInputValue("value", value);
        parameter.initAndValidate();
        if (id != null) parameter.setID(id);

        return parameter;
    }

    public static RealParameter createRealParameter(String id, Double[] value) {
        RealParameter parameter = new RealParameter();
        parameter.setInputValue("value", Arrays.asList(value));
        parameter.initAndValidate();
        if (id != null) parameter.setID(id);

        return parameter;
    }

    public static Parameter<? extends Number> createParameterWithBound(
            Value<? extends Number[]> value, Number lower, Number upper, boolean forceToDouble) {

        List<Number> values = Arrays.asList(value.value());

        // forceToDouble will ignore whether component type is Integer or not
        if ( !forceToDouble &&
                Objects.requireNonNull(value).getType().getComponentType().isAssignableFrom(Integer.class) ) {

            IntegerParameter parameter = new IntegerParameter();
            parameter.setInputValue("value", values);
            parameter.setInputValue("dimension", values.size());

            if (lower != null)
                parameter.setInputValue("lower", lower.intValue());
            if (upper != null)
                parameter.setInputValue("upper", upper.intValue());

            // set estimate="false" for IntegerArray values that are not RandomVariables.
            if (!(value instanceof RandomVariable))
                parameter.setInputValue("estimate", false);

            parameter.initAndValidate();
            ValueToParameter.setID(parameter, value);

            return parameter;

        } else { // Double and Number

            RealParameter parameter = new RealParameter();
            parameter.setInputValue("value", values);
            parameter.setInputValue("dimension", values.size());

            if (lower != null)
                parameter.setInputValue("lower", lower.doubleValue());
            if (upper != null)
                parameter.setInputValue("upper", upper.doubleValue());

            // set estimate="false" for DoubleArray values that are not RandomVariables.
            if (!(value instanceof RandomVariable))
                parameter.setInputValue("estimate", false);

            parameter.initAndValidate();
            ValueToParameter.setID(parameter, value);

            return parameter;
        }
    }


    /**
     * Make a BEAST2 model from the current model in parser.
     */
    public void createBEASTObjects() {

        List<Value<?>> sinks = parser.getModelSinks();

        for (Value<?> value : sinks) {
            createBEASTValueObjects(value);
        }

        Set<Generator> visited = new HashSet<>();
        for (Value<?> value : sinks) {
            traverseBEASTGeneratorObjects(value, true, false, visited);
        }

        visited.clear();
        for (Value<?> value : sinks) {
            traverseBEASTGeneratorObjects(value, false, true, visited);
        }
    }

    /**
     * @param id
     * @return true if the given id has a value in the data block and random variable in the model block
     */
    public boolean isClamped(String id) {
        if (id != null) {
            Value dataValue = parser.getValue(id, LPhyParser.Context.data);
            Value modelValue = parser.getModelDictionary().get(id);
            return (dataValue != null && modelValue != null && modelValue instanceof RandomVariable);
        }
        return false;
    }

    /**
     * @param id the id of the value
     * @return the value with this id from the data context if it exits, or if not, then the value from the model context if exists, or if neither exist, then returns null.
     */
    public Value getClampedValue(String id) {
        if (id != null) {
            Value clampedValue = parser.getValue(id, LPhyParser.Context.data);
            if (clampedValue != null) {
                return clampedValue;
            }
            return parser.getValue(id, LPhyParser.Context.model);
        }
        return null;
    }

    public GeneratorToBEAST getGeneratorToBEAST(Generator generator) {
        GeneratorToBEAST toBEAST = generatorToBEASTMap.get(generator.getClass());

        if (toBEAST == null) {
            // else see if there is a compatible to beast
            for (Class c : generatorToBEASTMap.keySet()) {
                // if *ToBEAST exists
                if (c.isAssignableFrom(generator.getClass())) {
                    toBEAST = generatorToBEASTMap.get(c);
                }
            }
        }
        return toBEAST;
    }

    public ValueToBEAST getMatchingValueToBEAST(Value value) {

        for (ValueToBEAST possibleToBEAST : valueToBEASTList) {
            if (possibleToBEAST.match(value)) {
                return possibleToBEAST;
            }
        }
        return null;
    }

    public ValueToBEAST getValueToBEAST(Object rawValue) {
        for (ValueToBEAST possibleToBEAST : valueToBEASTList) {
            // if *ToBEAST exists
            if (possibleToBEAST.match(rawValue)) {
                return possibleToBEAST;
            }
        }
        return null;
    }

    /**
     * Creates the beast value objects in a post-order traversal, so that inputs are always created before outputs.
     *
     * @param value the value to convert to a beast value (after doing so for the inputs of its generator, recursively)
     */
    private void createBEASTValueObjects(Value<?> value) {

        // do values of inputs recursively first
        Generator<?> generator = value.getGenerator();
        if (generator != null) {

            for (Object inputObject : generator.getParams().values()) {
                Value<?> input = (Value<?>) inputObject;
                createBEASTValueObjects(input);
            }
        }

        // now that the inputs are done we can do this one.
        if (beastObjects.get(value) == null) {
            valueToBEAST(value);
        }

    }


    private void traverseBEASTGeneratorObjects(Value<?> value, boolean modifyValues, boolean createGenerators, Set<Generator> visited) {

        Generator<?> generator = value.getGenerator();
        if (generator != null) {

            for (Object inputObject : generator.getParams().values()) {
                Value<?> input = (Value<?>) inputObject;
                traverseBEASTGeneratorObjects(input, modifyValues, createGenerators, visited);
            }

            if (!visited.contains(generator)) {
                generatorToBEAST(value, generator, modifyValues, createGenerators);
                visited.add(generator);
            }
        }
    }

    /**
     * This is called after valueToBEAST has been called on both the generated value and the input values.
     * Side-effect of this method is to create an equivalent BEAST object of the generator and put it in the beastObjects map of this BEASTContext.
     *
     * @param value
     * @param generator
     */
    private void generatorToBEAST(Value value, Generator generator, boolean modifyValues, boolean createGenerators) {

        if (getBEASTObject(generator) == null) {

            BEASTInterface beastGenerator = null;

            GeneratorToBEAST toBEAST = getGeneratorToBEAST(generator);

            if (toBEAST != null) {
                BEASTInterface beastValue = beastObjects.get(value);
                // If this is a generative distribution then swap to the clamped value if it exists
                if (generator instanceof GenerativeDistribution && isClamped(value.getId())) {
                    Value clampedValue = getClampedValue(value.getId());
                    beastValue = getBEASTObject(clampedValue);
                }

                if (beastValue == null) {
                    LoggerUtils.log.severe("Cannot find beast object given " + value);
                    return;
                }

                if (modifyValues) {
                    toBEAST.modifyBEASTValues(generator, beastValue, this);
                }
                if (createGenerators) {
                    beastGenerator = toBEAST.generatorToBEAST(generator, beastValue, this);
                }
            }

            if (createGenerators) {
                if (beastGenerator == null) {
                    if (!Exclusion.isExcludedGenerator(generator)) {
                        throw new UnsupportedOperationException("Unhandled generator in generatorToBEAST(): " + generator.getClass());
                    }
                } else {
                    addToContext(generator, beastGenerator);
                }
            }
        }
    }

    private BEASTInterface valueToBEAST(Value<?> val) {

        BEASTInterface beastValue = null;

        ValueToBEAST toBEAST = getMatchingValueToBEAST(val);

        if (toBEAST != null) {
            beastValue = toBEAST.valueToBEAST(val, this);
        }
        if (beastValue == null) {
            if (!Exclusion.isExcludedValue(val))
                throw new UnsupportedOperationException("Unhandled value" + (!val.isAnonymous() ? " named " + val.getId() : "") + " in valueToBEAST(): \"" +
                        val + "\" of type " + val.value().getClass());
        } else {
            // here is the common way to fill in context,
            // but there is another special method to do this
            /** {@link #putBEASTObject(GraphicalModelNode, BEASTInterface)} **/
            addToContext(val, beastValue);
        }
        return beastValue;
    }

    // fill in beastObjects, BEASTToLPHYMap, elements, and state
    private void addToContext(GraphicalModelNode node, BEASTInterface beastInterface) {
        beastObjects.put(node, beastInterface);
        BEASTToLPHYMap.put(beastInterface, node);
        elements.put(beastInterface, node);

        if (isState(node)) {
            Value var = (Value) node;

            if (var.getOutputs().size() > 0 && beastInterface != null && !state.contains(beastInterface)) {
                if (beastInterface instanceof StateNode) {
                    state.add((StateNode) beastInterface);
                } else if (beastInterface instanceof Concatenate) {
                    Concatenate concatenate = (Concatenate) beastInterface;
                    for (Function function : concatenate.functionsInput.get()) {
                        if (function instanceof StateNode && !state.contains(function)) {
                            state.add((StateNode) function);
                        }
                    }
                } else if (beastInterface instanceof BEASTVector) {
                    for (BEASTInterface beastElement : ((BEASTVector) beastInterface).getObjectList()) {
                        // BI obj is wrapped inside BEASTVector, so check existence again
                        if (beastElement instanceof StateNode && !state.contains(beastElement)) {
                            state.add((StateNode) beastElement);
                        }
                    }
                } else if (beastInterface instanceof Slice) {
                    BEASTInterface parent = (BEASTInterface)((Slice)beastInterface).functionInput.get();
                    if (parent instanceof StateNode) {
                        if (!state.contains(parent)) {
                            state.add((StateNode) parent);
                        } else {
                            // parent already in state
                        }
                    } else {
                        throw new RuntimeException("Slice representing random value, but the sliced beast interface is not a state node!");
                    }
                } else if (beastInterface instanceof Alignment) {

                } else {
                    throw new RuntimeException("Unexpected beastInterface returned true for isState() but can't be added to state");
                }
            }
        }
    }

    public boolean isState(GraphicalModelNode node) {
        if (node instanceof RandomVariable) return true;
        if (node instanceof Value) {
            Value value = (Value) node;
            if (value.isRandom() && (value.getGenerator() instanceof ElementsAt)) {
                ElementsAt elementsAt = (ElementsAt) value.getGenerator();
                if (elementsAt.array() instanceof RandomVariable) {
                    BEASTInterface beastInterface = getBEASTObject(elementsAt.array());
                    if (beastInterface == null) return true;
                }
            }
        }
        return false;
    }

    /**
     * @param freqParameter
     * @param stateNames    the names of the states in a space-delimited string
     * @return
     */
    public static Frequencies createBEASTFrequencies(RealParameter freqParameter, String stateNames) {
        Frequencies frequencies = new Frequencies();
        frequencies.setInputValue("frequencies", freqParameter);
        freqParameter.setInputValue("keys", stateNames);
        freqParameter.initAndValidate();
        frequencies.initAndValidate();
        return frequencies;
    }

    public static Prior createPrior(ParametricDistribution distr, Function function) {
        Prior prior = new Prior();
        prior.setInputValue("distr", distr);
        prior.setInputValue("x", function);
        prior.initAndValidate();
        if (function instanceof BEASTInterface) prior.setID(((BEASTInterface) function).getID() + ".prior");
        return prior;
    }


    public List<Operator> createOperators() {

        List<Operator> operators = new ArrayList<>();

        for (StateNode stateNode : state) {
            if (!skipOperators.contains(stateNode)) {
                if (stateNode instanceof RealParameter) {
                    Operator operator = createBEASTOperator((RealParameter) stateNode);
                    if (operator != null) operators.add(operator);
                } else if (stateNode instanceof IntegerParameter) {
                    operators.add(createBEASTOperator((IntegerParameter) stateNode));
                } else if (stateNode instanceof BooleanParameter) {
                    operators.add(createBEASTOperator((BooleanParameter) stateNode));
                } else if (stateNode instanceof Tree) {
                    Tree tree = (Tree) stateNode;
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

    private List<Logger> createLoggers(int logEvery, String fileName) {
        topDist = getTopCompoundDist();

        List<Logger> loggers = new ArrayList<>();
        // reduce screen logging
        loggers.add(createScreenLogger(logEvery * 100));
        loggers.add(createLogger(logEvery, fileName + ".log"));
        loggers.addAll(createTreeLoggers(logEvery, fileName));

        return loggers;
    }

    private Logger createLogger(int logEvery, String fileName) {

        List<Loggable> nonTrees = state.stream()
                .filter(stateNode -> !(stateNode instanceof Tree))
                .collect(Collectors.toList());

        // tree height, but not in screen logging
        if (fileName != null) {
            List<TreeInterface> trees = getTrees();
            for (TreeInterface tree : trees) {
// <log id="TreeHeight" spec="beast.evolution.tree.TreeStatLogger" tree="@Tree"/>
                TreeStatLogger treeStatLogger = new TreeStatLogger();
                treeStatLogger.initByName("tree", tree, "logLength", false);
                nonTrees.add(treeStatLogger);
            }
        }

        // not in screen logging
        if (fileName != null)
            nonTrees.addAll(extraLoggables);

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

    private CompoundDistribution[] topDist = new CompoundDistribution[3];
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

    CompoundDistribution getPosteriorDist() {
        return topDist[0];
    }

    public List<TreeInterface> getTrees() {
        return state.stream()
                .filter(stateNode -> stateNode instanceof TreeInterface)
                .map(stateNode -> (TreeInterface) stateNode)
                .sorted(Comparator.comparing(TreeInterface::getID))
                .collect(Collectors.toList());
    }

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

            if (loggable instanceof beast.mascot.distribution.Mascot) { // TODO
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
                if (multipleTrees) // multi-partitions and unlink trees
                    treeFNSteam = fileNameStem + "_" + tree.getID();
                String fileName = treeFNSteam + ".mascot.trees";
                logger.setInputValue("fileName", fileName);
                logger.setID("StructuredTreeLogger" + (multipleTrees ? "." + treeFNSteam : ""));

                logger.setInputValue("mode", "tree");
                logger.initAndValidate();

                treeLoggers.add(logger);
                elements.put(logger, null);

            } else if (loggable instanceof AncestralStateTreeLikelihood) { // TODO
                // DPG: TreeWithTraitLogger
                TreeInterface tree = ((AncestralStateTreeLikelihood) loggable).treeInput.get();

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

    private Logger createScreenLogger(int logEvery) {
        return createLogger(logEvery, null);
    }

    public static double getOperatorWeight(int size) {
        return Math.pow(size, 0.7);
    }

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

    private Operator createBEASTOperator(BooleanParameter parameter) {
        Operator operator = new BitFlipOperator();
        operator.setInputValue("parameter", parameter);
        operator.setInputValue("weight", getOperatorWeight(parameter.getDimension()));
        operator.initAndValidate();
        operator.setID(parameter.getID() + ".bitFlip");

        return operator;
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

    private CompoundDistribution createBEASTPosterior() {

        List<Distribution> priorList = new ArrayList<>();

        List<Distribution> likelihoodList = new ArrayList<>();

        for (Map.Entry<GraphicalModelNode<?>, BEASTInterface> entry : beastObjects.entrySet()) {
            if (entry.getValue() instanceof Distribution) {
                if ( !(entry.getKey() instanceof Generator) )
                    throw new IllegalArgumentException("Require likelihood or prior to be Generator !");

                // Now allow function in the key, e.g. GTUnphaseToBEAST
                Generator g = (Generator) entry.getKey();

                Distribution dist = (Distribution) entry.getValue();
                if (generatorOfSink(g))
                    likelihoodList.add(dist);
                else
                    priorList.add(dist);

            }
        }

        for (BEASTInterface beastInterface : elements.keySet()) {
            if (beastInterface instanceof Distribution && !likelihoodList.contains(beastInterface) && !priorList.contains(beastInterface)) {
                priorList.add((Distribution) beastInterface);
            }
        }

        System.out.println("Found " + likelihoodList.size() + " likelihoods.");
        System.out.println("Found " + priorList.size() + " priors.");

        CompoundDistribution priors = new CompoundDistribution();
        priors.setInputValue("distribution", priorList);
        priors.initAndValidate();
        priors.setID(PRIOR_ID);
        elements.put(priors, null);

        CompoundDistribution likelihoods = new CompoundDistribution();
        likelihoods.setInputValue("distribution", likelihoodList);
        likelihoods.initAndValidate();
        likelihoods.setID(LIKELIHOOD_ID);
        elements.put(likelihoods, null);

        List<Distribution> posteriorList = new ArrayList<>();
        posteriorList.add(priors);
        posteriorList.add(likelihoods);

        CompoundDistribution posterior = new CompoundDistribution();
        posterior.setInputValue("distribution", posteriorList);
        posterior.initAndValidate();
        posterior.setID(POSTERIOR_ID);
        elements.put(posterior, null);

        return posterior;
    }

    private boolean generatorOfSink(Generator g) {
        for (Value<?> var : parser.getModelSinks()) {
            if (var.getGenerator() == g) {
                return true;
            }
            if (var instanceof VectorizedRandomVariable) {
                VectorizedRandomVariable vv = (VectorizedRandomVariable) var;
                for (int i = 0; i < vv.size(); i++) {
                    RandomVariable rv = vv.getComponentValue(i);
                    if (rv.getGenerator() == g) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public MCMC createMCMC(long chainLength, int logEvery, String fileName, int preBurnin) {

        createBEASTObjects();

        CompoundDistribution posterior = createBEASTPosterior();

        MCMC mcmc = new MCMC();
        mcmc.setInputValue("distribution", posterior);
        mcmc.setInputValue("chainLength", chainLength);

        List<Operator> operators = createOperators();
        for (int i = 0; i < operators.size(); i++) {
            System.out.println(operators.get(i));
        }

        mcmc.setInputValue("operator", operators);
        mcmc.setInputValue("logger", createLoggers(logEvery, fileName));

        State state = new State();
        state.setInputValue("stateNode", this.state);
        state.initAndValidate();
        elements.put(state, null);

        // TODO make sure the stateNode list is being correctly populated
        mcmc.setInputValue("state", state);

        if (inits.size() > 0) mcmc.setInputValue("init", inits);

        // if not given, preBurnin == 0, then will be defined by all state nodes size
        if (preBurnin < 1)
            preBurnin = getAllStatesSize(this.state) * 10;
        mcmc.setInputValue("preBurnin", preBurnin);

        mcmc.initAndValidate();
        return mcmc;
    }

    protected int getAllStatesSize(List<StateNode> stateNodes) {
        int size = 0;
        for (StateNode stateNode : stateNodes) {
            if (stateNode instanceof TreeInterface)
                size += ((TreeInterface) stateNode).getInternalNodeCount();
            else
                size += stateNode.getDimension();
        }
        return size;
    }

    public void clear() {
        state.clear();
        elements.clear();
        beastObjects.clear();
        extraOperators.clear();
        skipOperators.clear();
    }

    public void runBEAST(String fileNameStem) {

        MCMC mcmc = createMCMC(1000000, 1000, fileNameStem, 0);

        try {
            mcmc.run();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
    }

    /**
     * Create BEAST 2 XML from LPhy objects.
     *
     * @param fileNameStem
     * @param chainLength  if <=0, then use default 1,000,000.
     *                     logEvery = chainLength / numOfSamples,
     *                     where numOfSamples = 2000 as default.
     * @param preBurnin    preBurnin for BEAST MCMC, default to 0.
     * @return BEAST 2 XML in String
     */
    public String toBEASTXML(final String fileNameStem, long chainLength, int preBurnin) {

        final int numOfSamples = 2000;
        // default to 1M if not specified
        if (chainLength <= 0)
            chainLength = 1000000;
        // Will throw an ArithmeticException in case of overflow.
        int logEvery = toIntExact(chainLength / numOfSamples);

        LoggerUtils.log.info("MCMC total chain length = " + chainLength +
                ", log every = " + logEvery + ", samples = " + numOfSamples);

        MCMC mcmc = createMCMC(chainLength, logEvery, fileNameStem, preBurnin);

        String xml = new XMLProducer().toXML(mcmc, elements.keySet());

        return xml;
    }

    public void addExtraOperator(Operator operator) {
        extraOperators.add(operator);
    }

    public boolean hasExtraOperator(String opID) {
        return extraOperators.stream().anyMatch(op -> op.getID().equals(opID));
    }

    public void addTaxon(String taxonID) {
        if (!allTaxa.containsKey(taxonID)) {
            allTaxa.put(taxonID, new Taxon(taxonID));
        }
    }

    /**
     * @param id
     * @return the taxon with this id.
     */
    public Taxon getTaxon(String id) {
        addTaxon(id);
        return allTaxa.get(id);
    }

    public List<Taxon> createTaxonList(List<String> ids) {
        List<Taxon> taxonList = new ArrayList<>();
        for (String id : ids) {
            Taxon taxon = allTaxa.get(id);
            if (taxon == null) {
                addTaxon(id);
                taxonList.add(allTaxa.get(id));
            } else {
                taxonList.add(taxon);
            }
        }
        return taxonList;
    }

    /**
     * The special method to to fill in context,
     * use it as a caution.
     * @param node
     * @param beastInterface
     * @see #valueToBEAST(Value)
     */
    public void putBEASTObject(GraphicalModelNode node, BEASTInterface beastInterface) {
        addToContext(node, beastInterface);
    }

    public void addExtraLogger(Loggable loggable) {
        extraLoggables.add(loggable);
    }

    public void addInit(StateNodeInitialiser beastInitializer) {
        inits.add(beastInitializer);
    }

    public List<Value<lphy.evolution.alignment.Alignment>> getAlignments() {
        ArrayList<Value<lphy.evolution.alignment.Alignment>> alignments = new ArrayList<>();
        for (GraphicalModelNode node : beastObjects.keySet()) {
            if (node instanceof Value && node.value() instanceof lphy.evolution.alignment.Alignment) {
                alignments.add((Value<lphy.evolution.alignment.Alignment>) node);
            }
        }
        return alignments;
    }

    public Value getOutput(Generator generator) {

        final Value[] outputValue = new Value[1];
        for (Value value : parser.getModelSinks()) {

            Value.traverseGraphicalModel(value, new GraphicalModelNodeVisitor() {
                @Override
                public void visitValue(Value value) {
                    if (value.getGenerator() == generator) {
                        outputValue[0] = value;
                    }
                }

                @Override
                public void visitGenerator(Generator g) {

                }
            }, true);
        }
        return outputValue[0];
    }


}