package lphybeast;

import beast.evolution.datatype.DataType;
import beast.util.PackageManager;
import jebl.evolution.sequences.SequenceType;
import lphy.graphicalModel.Generator;
import lphy.graphicalModel.Value;
import lphy.util.LoggerUtils;
import lphy.util.Progress;
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

    private LPhyBEASTExtFactory(Progress progress, double startPer, double endPer) {
        // ServiceLoader cannot work with BEASTClassLoader
        registerExtensions(null, progress, startPer, endPer);
    }

    // singleton
    public static synchronized LPhyBEASTExtFactory getInstance(Progress progress, double startPer, double endPer) {
        if (factory == null)
            factory = new LPhyBEASTExtFactory(progress, startPer, endPer);
        return factory;
    }
    // singleton no Process
    public static synchronized LPhyBEASTExtFactory getInstance() {
        if (factory == null)
            factory = new LPhyBEASTExtFactory(null, 0.0, 1.0);
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
     * Use {@link PackageManager} to load the container classes from LPhyBEAST extensions,
     * which include all extended classes.
     * @return  the list of container classes (one per extension).
     */
    private List<LPhyBEASTExt> getExtClasses(Progress progress, double startPer, double endPer) {

        // loading all beast2 classes sets to half of progress
        if (progress != null)
            progress.setProgressPercentage(startPer + (endPer-startPer)*0.1);
        List<Class<?>> classList = PackageManager.find(LPhyBEASTExt.class, false);
        if (progress != null)
            progress.setProgressPercentage(startPer + (endPer-startPer)*0.5);

        // instantiating all beast2 classes sets to 1/4 of progress
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
            if (progress != null)
                progress.setProgressPercentage(progress.getCurrentPercentage() + (endPer-startPer)*0.25/classList.size());
//        catch (Throwable e) { e.printStackTrace(); }
        }
        return extensionList;
    }


    //    private void registerExtensions(ServiceLoader<LPhyBEASTExt> loader, String clsName) {
    private void registerExtensions(String clsName, Progress progress, double startPer, double endPer) {
        valueToBEASTList = new ArrayList<>();
        generatorToBEASTMap = new LinkedHashMap<>();
        dataTypeMap = new ConcurrentHashMap<>();

        excludedGeneratorClasses = new ArrayList<>();
        excludedValueClasses = new ArrayList<>();

        try {
//            while (extensions.hasNext()) { // TODO validation if add same name
            List<LPhyBEASTExt> extList = getExtClasses(progress, startPer, endPer);

            for (LPhyBEASTExt ext : extList) {
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

                    // registering all extensions sets to 1/4 of progress
                    if (progress != null)
                        progress.setProgressPercentage(progress.getCurrentPercentage() + (endPer-startPer)*0.25/extList.size());
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

}
