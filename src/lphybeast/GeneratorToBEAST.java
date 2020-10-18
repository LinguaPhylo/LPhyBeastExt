package lphybeast;

import beast.core.BEASTInterface;
import lphy.graphicalModel.Generator;

public interface GeneratorToBEAST<T extends Generator,S extends BEASTInterface> {

    /**
     * converts a generator to an equivalent BEAST object
     * @param generator the generator to be converted
     * @param value the already-converted value that this generator produced for the conversion
     * @param context the BEASTContext object holding other Beast objects already converted
     * @return a new BEAST object representing this generator
     */
    S generatorToBEAST(T generator, BEASTInterface value, BEASTContext context);

    /**
     * provides a hook to allow generators that need to, to modify/replace the values that represent their
     * inputs or outputs. This is called after all the automatic beast value creation, but before
     * generatorToBEAST is called on any of the generators.
     * @param generator the generator
     * @param value the already-converted value that this generator produced for the conversion
     * @param context the BEASTContext object holding other Beast objects already converted
     */
    default void modifyBEASTValues(T generator, BEASTInterface value, BEASTContext context) {
        // default do nothing
    }

    /**
     * The class of value that can be converted to BEAST.
     * @return
     */
    Class<T> getGeneratorClass();

    /**
     * The BEAST class to be converted. It is only used for summarising at the moment.
     *
     * @return
     */
    default Class<S> getBEASTClass() {
        return (Class<S>)BEASTInterface.class;
    }

}
