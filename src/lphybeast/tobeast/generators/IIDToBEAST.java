package lphybeast.tobeast.generators;

import beast.core.BEASTInterface;
import beast.core.parameter.Parameter;
import beast.math.distributions.Prior;
import lphy.core.distributions.Bernoulli;
import lphy.core.distributions.IID;
import lphy.graphicalModel.GenerativeDistribution;
import lphy.graphicalModel.GraphicalModelNode;
import lphy.utils.LoggerUtils;
import lphybeast.BEASTContext;
import lphybeast.GeneratorToBEAST;
import outercore.util.BEASTVector;

import java.util.ArrayList;
import java.util.List;

public class IIDToBEAST implements GeneratorToBEAST<IID, BEASTInterface> {
    @Override
    public BEASTInterface generatorToBEAST(IID generator, BEASTInterface value, BEASTContext context) {
        GenerativeDistribution baseDistribution = generator.getBaseDistribution();
        GeneratorToBEAST toBEAST = context.getGeneratorToBEAST(baseDistribution);

        // IID DiscretizedGamma is ignored in Exclusion
        // SiteModel is generated by special code in PhyloCTMCToBEAST,
        // because siteRates are parallel with Q matrix
        if (toBEAST == null) {
            if (generator.getBaseDistribution() instanceof Bernoulli) {
                //TODO need BernoulliToBEAST?
                throw new UnsupportedOperationException("in dev");
            }
            LoggerUtils.log.warning("Ignoring IID distribution " + generator.getBaseDistribution().getName());
            return null;
        }

        if (value instanceof Parameter) {
            Parameter parameter = (Parameter) value;

            if (generator.size() != parameter.getDimension())
                throw new IllegalArgumentException("Expecting base distribution sizes and parameter dimension to match!");

            BEASTInterface beastGenerator = toBEAST.generatorToBEAST(baseDistribution, value, context);
            if ( !(beastGenerator instanceof Prior) )
                throw new IllegalArgumentException("Expecting Prior to be generated ! " + beastGenerator.getClass().getSimpleName());

            return (Prior) beastGenerator;

        } else if (value instanceof BEASTVector) {

            List<BEASTInterface> values = ((BEASTVector)value).getObjectList();

            if (generator.size() != values.size())
                throw new IllegalArgumentException("Expecting value and base distribution list sizes to match!");

            List<BEASTInterface> beastGenerators = new ArrayList<>();
            for (int i = 0; i < values.size(); i++)  {
                // get Prior
                BEASTInterface beastGenerator = toBEAST.generatorToBEAST(baseDistribution, values.get(i), context);
                if ( !(beastGenerator instanceof Prior) )
                    throw new IllegalArgumentException("Expecting Prior to be generated ! " + beastGenerator.getClass().getSimpleName());

                beastGenerators.add((Prior) beastGenerator);
                /** call {@link BEASTContext#addToContext(GraphicalModelNode, BEASTInterface)} **/
                context.putBEASTObject(baseDistribution, beastGenerator);
            }
            // wrap the list of Prior into BEASTVector
            return new BEASTVector(beastGenerators);

        } else {
            throw new IllegalArgumentException("Expecting Parameter value from IID , " +
                    "but getting " + value.getClass().getSimpleName());
        }
    }

    @Override
    public Class<IID> getGeneratorClass() {
        return IID.class;
    }

}
