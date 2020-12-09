package lphybeast;

import lphy.core.distributions.DiscretizedGamma;
import lphy.core.distributions.RandomComposition;
import lphy.core.distributions.WeightedDirichlet;
import lphy.core.functions.*;
import lphy.evolution.Taxa;
import lphy.evolution.alignment.Alignment;
import lphy.evolution.tree.TimeTree;
import lphy.graphicalModel.Generator;
import lphy.graphicalModel.Value;
import lphy.parser.functions.ExpressionNode;
import lphy.parser.functions.MapFunction;
import lphy.parser.functions.MethodCall;
import lphy.parser.functions.RangeList;

import java.util.HashMap;
import java.util.TreeMap;

/**
 * Utils class to exclude {@link lphy.graphicalModel.Value}
 * or {@link lphy.graphicalModel.Generator} to skip the validation
 * so not to throw UnsupportedOperationException
 * in either <code>BEASTContext#valueToBEAST(Value)<code/> or
 * <code>BEASTContext#generatorToBEAST(Value, Generator)<code/>.
 * @author Walter Xie
 */
public class Exclusion {

    public static boolean isExcludedValue(Value<?> val) {
        Object ob = val.value();
        return ob instanceof String || ob instanceof String[] || // ignore all String: d = nexus(file="Dengue4.nex");
                ob instanceof HashMap || ob instanceof TreeMap ||
                // exclude the value returned by taxa (and ages) functions
                ( ob instanceof Taxa && !(ob instanceof Alignment) ) || ob instanceof TimeTree[];
    }

    public static boolean isExcludedGenerator(Generator generator) {

        return generator instanceof WeightedDirichlet || generator instanceof IntegerArray ||
                generator instanceof ExpressionNode || generator instanceof RandomComposition ||
                generator instanceof NTaxaFunction || generator instanceof NCharFunction ||
                generator instanceof CreateTaxa || generator instanceof TaxaFunction ||
                generator instanceof Species || generator instanceof TaxaAgesFromFunction ||
                generator instanceof ReadNexus || generator instanceof ReadFasta ||
                generator instanceof ARange || generator instanceof Range ||
                generator instanceof MapFunction || generator instanceof MethodCall  ||
                generator instanceof RangeList || generator instanceof ElementsAt || generator instanceof Rep ||
                generator instanceof DiscretizedGamma || generator instanceof Length ||
                generator instanceof MigrationMatrix || generator instanceof MigrationCount ||
                generator instanceof Select;
    }
}
