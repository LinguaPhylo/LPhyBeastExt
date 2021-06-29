package lphybeast.registry;

import beast.evolution.datatype.DataType;
import beast.util.PackageManager;
import jebl.evolution.sequences.SequenceType;
import lphy.graphicalModel.Generator;
import lphy.graphicalModel.Value;
import lphybeast.GeneratorToBEAST;
import lphybeast.ValueToBEAST;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Implement this interface to register classes converting LPhy into BEAST,
 * which should include {@link lphybeast.ValueToBEAST}, {@link lphybeast.GeneratorToBEAST},
 * and {@link DataType}.
 *
 * @author Walter Xie
 */
public interface ClassesRegistry {

    List<Class<? extends ValueToBEAST>> getValuesToBEASTs();

    List<Class<? extends GeneratorToBEAST>> getGeneratorToBEASTs();

    Map<SequenceType, DataType> getDataTypeMap();

    List<Class<? extends Generator>> getExcludedGenerator();

    List<Class<? extends Value>> getExcludedValue();

    static List<ClassesRegistry> getRegistryClasses() {
        //TODO check if PackageManager handling same class from jar and development
        List<Class<?>> classList = PackageManager.find(ClassesRegistry.class, false);

        List<ClassesRegistry> registryList = new ArrayList<>();
        for (Class<?> cls : classList) {
            // https://docs.oracle.com/javase/9/docs/api/java/lang/Class.html#newInstance--
            try {
                Object obj = cls.getDeclaredConstructor().newInstance();
                registryList.add((ClassesRegistry) obj);
            } catch (InvocationTargetException | InstantiationException |
                    IllegalAccessException | NoSuchMethodException e) {
                // do nothing
            }
//        catch (Throwable e) { e.printStackTrace(); }
        }
        return registryList;
    }

}
