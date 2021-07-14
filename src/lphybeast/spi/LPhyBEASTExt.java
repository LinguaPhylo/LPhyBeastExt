package lphybeast.spi;

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
 * The service interface defined for SPI.
 * Implement this interface to create one "Container" provider class
 * for each module of LPhyBEAST or its extensions,
 * which should include {@link lphybeast.ValueToBEAST}, {@link lphybeast.GeneratorToBEAST},
 * and {@link DataType}.
 *
 * @author Walter Xie
 */
public interface LPhyBEASTExt {

    List<Class<? extends ValueToBEAST>> getValuesToBEASTs();

    List<Class<? extends GeneratorToBEAST>> getGeneratorToBEASTs();

    Map<SequenceType, DataType> getDataTypeMap();

    List<Class<? extends Generator>> getExcludedGenerator();

    List<Class<? extends Value>> getExcludedValue();

    /**
     * Use {@link PackageManager} to load the container classes from LPhyBEAST extensions,
     * which include all extended classes.
     * This mechanism will be replaced by Java module system and SPI in future.
     * @return  the list of container classes (one per extension).
     */
    static List<LPhyBEASTExt> getExtClasses() {

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

}
