package lphybeast.tobeast.values;

import beast.evolution.alignment.Sequence;
import beast.evolution.alignment.Taxon;
import beast.evolution.alignment.TaxonSet;
import beast.evolution.datatype.DataType;
import beast.evolution.datatype.UserDataType;
import beast.evolution.tree.TraitSet;
import jebl.evolution.sequences.SequenceType;
import lphy.evolution.alignment.Alignment;
import lphy.evolution.alignment.SimpleAlignment;
import lphy.evolution.sequences.Standard;
import lphy.graphicalModel.Generator;
import lphy.graphicalModel.GraphicalModelNode;
import lphy.graphicalModel.Value;
import lphybeast.BEASTContext;
import lphybeast.DataTypeRegistry;
import lphybeast.ValueToBEAST;

import java.util.ArrayList;
import java.util.List;

public class AlignmentToBEAST implements ValueToBEAST<SimpleAlignment, beast.evolution.alignment.Alignment> {

    private static final String DISCRETE = "discrete";

    @Override
    public beast.evolution.alignment.Alignment valueToBEAST(Value<SimpleAlignment> alignmentValue, BEASTContext context) {

        SimpleAlignment alignment = alignmentValue.value();
        SequenceType lphyDataType = alignment.getSequenceType();
        String[] taxaNames = alignment.getTaxaNames();

        DataType beastDataType = DataTypeRegistry.getBEASTDataType(lphyDataType);

        beast.evolution.alignment.Alignment beastAlignment;
        // TODO BEAST special data types: StandardData, UserDataType, IntegerData
        if (lphyDataType instanceof Standard) {
            // UserDataType for trait alignment
            if ( ! (beastDataType instanceof UserDataType))
                throw new IllegalArgumentException("Require BEAST 'user defined' ! But find " +
                        beastDataType.getTypeDescription());

            // AlignmentFromTrait
            beastAlignment = new beast.evolution.alignment.AlignmentFromTrait();
            // Input<DataType.Base> userDataTypeInput
            beastAlignment.setInputValue("userDataType", beastDataType);

            List<Taxon> taxonList = context.createTaxonList(List.of(taxaNames));
            String traitStr = createTraitString(alignment);

            // TODO morphological data
            TraitSet traitSet = new TraitSet();
            traitSet.setInputValue("traitname", DISCRETE);
            traitSet.setInputValue("value", traitStr);

            TaxonSet taxa = new TaxonSet();
            taxa.setInputValue("taxon", taxonList);
            taxa.initAndValidate();

            traitSet.setInputValue("taxa", taxa);
            traitSet.initAndValidate();

            beastAlignment.setInputValue("traitSet", traitSet);
            beastAlignment.initAndValidate();

        } else {

            // sequences
            List<Sequence> sequences = new ArrayList<>();
            for (int i = 0; i < taxaNames.length; i++) {
                context.addTaxon(taxaNames[i]);
                // have to convert to string, cannot use integer state
                // state = sequenceType.getState(alignment[taxonIndex][j]);
                String s = alignment.getSequence(i);
                sequences.add(createBEASTSequence(taxaNames[i], s));
            }

            // normal Alignment
            beastAlignment = new beast.evolution.alignment.Alignment();
            // Input<String> dataTypeInput
            beastAlignment.setInputValue("dataType", beastDataType.getTypeDescription());
            beastAlignment.setInputValue("sequence", sequences);
            beastAlignment.initAndValidate();

        }

        // using LPhy var as ID allows multiple alignments
        if (!alignmentValue.isAnonymous()) beastAlignment.setID(alignmentValue.getCanonicalId());

        // TODO temp solution to rm parent alignment if there is a child alignment created from it,
        // e.g. original alignment creates err alignment

        Generator<?> alignmentGenerator = alignmentValue.getGenerator();
        boolean hasParentAlignment = false;
        for (GraphicalModelNode<?> input : alignmentGenerator.getInputs()) {
            if (input.value() instanceof Alignment) {
                hasParentAlignment = true;
                break;
            }
        }

        if (hasParentAlignment) {

        }

        return beastAlignment;
    }

    // taxa names = traits, ...
    private String createTraitString(SimpleAlignment alignment) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < alignment.ntaxa(); i++) {
            if (i > 0) builder.append(", ");
            builder.append(alignment.getTaxonName(i));
            builder.append("=");
            builder.append(alignment.getSequence(i));
        }
        return builder.toString();
    }

    private Sequence createBEASTSequence(String taxon, String sequence) {
        Sequence seq = new Sequence();
        seq.setInputValue("taxon", taxon);
        seq.setInputValue("value", sequence);
        seq.initAndValidate();
        return seq;
    }

    @Override
    public Class getValueClass() {
        return SimpleAlignment.class;
    }

    @Override
    public Class<beast.evolution.alignment.Alignment> getBEASTClass() {
        return beast.evolution.alignment.Alignment.class;
    }
}
