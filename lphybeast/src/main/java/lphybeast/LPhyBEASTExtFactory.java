package lphybeast;

import beast.evolution.datatype.DataType;
import beast.util.PackageManager;
import jebl.evolution.sequences.SequenceType;
import lphy.graphicalModel.Generator;
import lphy.graphicalModel.Value;
import lphy.util.LoggerUtils;
import lphybeast.spi.LPhyBEASTExt;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The implementation to load LPhyBEAST extensions using {@link ServiceLoader}.
 * All distributions, functions and data types will be collected
 * in this class for later use.
 *
 * @author Walter Xie
 */
public class LPhyBEASTExtFactory {
    private static LPhyBEASTExtFactory factory;
//    final private ServiceLoader<LPhyBEASTExt> loader;

    private LPhyBEASTExtFactory() {
//        loader = ServiceLoader.load(LPhyBEASTExt.class);
//         register all ext
//        registerExtensions(loader, null);

        // ServiceLoader cannot work with BEASTClassLoader
        registerExtensions(null);
    }

    // singleton
    public static synchronized LPhyBEASTExtFactory getInstance() {
        if (factory == null)
            factory = new LPhyBEASTExtFactory();
        return factory;
    }

    //*** registry ***//

    /**
     * {@link ValueToBEAST}
     */
    public List<ValueToBEAST> valueToBEASTList;
    /**
     * Use LinkedHashMap to keep inserted ordering, so the first matching converter is used.
     * @see  GeneratorToBEAST
     */
    public Map<Class, GeneratorToBEAST> generatorToBEASTMap;
    /**
     * LPhy sequence types {@link SequenceType} maps to BEAST {@link DataType}
     */
    public Map<SequenceType, DataType> dataTypeMap;
    /**
     * {@link Generator}
     */
    public List<Class<? extends Generator>> excludedGeneratorClasses;
    /**
     * {@link Value}
     */
    public List<Class<? extends Value>> excludedValueClasses;

    /**
     * for creating doc only.
     * @param fullClsName  the full name with package of the class
     *                 to implement {@link LPhyBEASTExt},
     *                 such as lphy.spi.LPhyExtImpl

    public void loadExtension(String fullClsName) {
        loader.reload();
        registerExtensions(loader, fullClsName);
    }

    public List<LPhyBEASTExt> getExtensions() {
        loader.reload();
        Iterator<LPhyBEASTExt> extensions = loader.iterator();
        List<LPhyBEASTExt> extList = new ArrayList<>();
        extensions.forEachRemaining(extList::add);
        return extList;
    }*/

    /**
     * Use {@link PackageManager} to load the container classes from LPhyBEAST extensions,
     * which include all extended classes.
     * @return  the list of container classes (one per extension).
     */
    public List<LPhyBEASTExt> getExtClasses() {

        List<Class<?>> classList = PackageManager.find(LPhyBEASTExt.class, false);

        List<LPhyBEASTExt> extensionList = new ArrayList<>();
        for (Class<?> cls : classList) {
            // https://docs.oracle.com/javase/9/docs/api/java/lang/Class.html#newInstance--
            try {
                Object obj = cls.getDeclaredConstructor().newInstance();
                extensionList.add((LPhyBEASTExt) obj);
            } catch (InvocationTargetException | InstantiationException |
                    IllegalAccessException | NoSuchMethodException e) {
                // do nothing
            }
//        catch (Throwable e) { e.printStackTrace(); }
        }
        return extensionList;
    }


//    private void registerExtensions(ServiceLoader<LPhyBEASTExt> loader, String clsName) {
    private void registerExtensions(String clsName) {
        valueToBEASTList = new ArrayList<>();
        generatorToBEASTMap = new LinkedHashMap<>();
        dataTypeMap = new ConcurrentHashMap<>();

        excludedGeneratorClasses = new ArrayList<>();
        excludedValueClasses = new ArrayList<>();

        try {
//            Iterator<LPhyBEASTExt> extensions = loader.iterator();
//            while (extensions.hasNext()) { // TODO validation if add same name

            for (LPhyBEASTExt ext : getExtClasses()) {
                //*** LPhyBEASTExtImpl must have a public no-args constructor ***//
//                LPhyBEASTExt ext = extensions.next();
                // clsName == null then register all
                if (clsName == null || ext.getClass().getName().equalsIgnoreCase(clsName)) {
                    System.out.println("Registering extension from " + ext.getClass().getName());

                    final List<Class<? extends ValueToBEAST>> valuesToBEASTs = ext.getValuesToBEASTs();
                    final List<Class<? extends GeneratorToBEAST>> generatorToBEASTs = ext.getGeneratorToBEASTs();
                    final Map<SequenceType, DataType> dataTypeMap = ext.getDataTypeMap();

                    registerValueToBEAST(valuesToBEASTs);
                    registerGeneratorToBEAST(generatorToBEASTs);
                    registerDataTypes(dataTypeMap);

                    excludedGeneratorClasses.addAll(ext.getExcludedGenerator());
                    excludedValueClasses.addAll(ext.getExcludedValue());
                }
            }

            System.out.println(valueToBEASTList.size() + " ValuesToBEAST = " + valueToBEASTList);
            System.out.println(generatorToBEASTMap.size() + " GeneratorToBEAST = " + generatorToBEASTMap);
            System.out.println(dataTypeMap.size() + " Data Type = " + dataTypeMap);
            System.out.println(excludedGeneratorClasses.size() + " extra Generator(s) excluded = " + excludedGeneratorClasses);
            System.out.println(excludedValueClasses.size() + " extra Value(s) excluded = " + excludedValueClasses);

        } catch (ServiceConfigurationError serviceError) {
            System.err.println(serviceError);
            serviceError.printStackTrace();
        }

    }

    private void registerValueToBEAST(final List<Class<? extends ValueToBEAST>> valuesToBEASTs) {
        for (Class<? extends ValueToBEAST> c : valuesToBEASTs) {
            try {
                // https://docs.oracle.com/javase/9/docs/api/java/lang/Class.html#newInstance--
                ValueToBEAST<?,?> valueToBEAST = (ValueToBEAST<?,?>) c.getDeclaredConstructor().newInstance();
                if (this.valueToBEASTList.contains(valueToBEAST))
                    LoggerUtils.log.severe(valueToBEAST + " exists in the valueToBEASTList !");
                this.valueToBEASTList.add(valueToBEAST);
            } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
                e.printStackTrace();
            }
        }
    }

    private void registerGeneratorToBEAST(final List<Class<? extends GeneratorToBEAST>> generatorToBEASTs) {
        for (Class<? extends GeneratorToBEAST> c : generatorToBEASTs) {
            try {
                // https://docs.oracle.com/javase/9/docs/api/java/lang/Class.html#newInstance--
                GeneratorToBEAST<?,?> generatorToBEAST = (GeneratorToBEAST<?,?>) c.getDeclaredConstructor().newInstance();
                if (this.generatorToBEASTMap.containsKey(generatorToBEAST))
                    LoggerUtils.log.severe(generatorToBEAST + " exists in the generatorToBEASTMap !");
                this.generatorToBEASTMap.put(generatorToBEAST.getGeneratorClass(), generatorToBEAST);
            } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
                e.printStackTrace();
            }
        }
    }

    private void registerDataTypes(final Map<SequenceType, DataType> dataTypeMap) {
        for (Map.Entry<SequenceType, DataType> entry : dataTypeMap.entrySet()) {
            if (this.dataTypeMap.containsKey(entry.getKey()))
                LoggerUtils.log.severe(entry.getKey() + " exists in the dataTypeMap !");
            this.dataTypeMap.put(entry.getKey(), entry.getValue());
        }
    }

}
