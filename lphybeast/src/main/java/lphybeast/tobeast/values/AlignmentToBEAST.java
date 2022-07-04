package lphybeast.tobeast.values;

import beast.evolution.alignment.Sequence;
import beast.evolution.alignment.Taxon;
import beast.evolution.alignment.TaxonSet;
import beast.evolution.datatype.DataType;
import beast.evolution.datatype.UserDataType;
import beast.evolution.tree.TraitSet;
import jebl.evolution.sequences.SequenceType;
import lphy.evolution.alignment.SimpleAlignment;
import lphy.evolution.datatype.Standard;
import lphy.graphicalModel.Value;
import lphybeast.BEASTContext;
import lphybeast.ValueToBEAST;
import lphybeast.tobeast.DataTypeUtils;

import java.util.ArrayList;
import java.util.List;

public class AlignmentToBEAST implements ValueToBEAST<SimpleAlignment, beast.evolution.alignment.Alignment> {

    private static final String DISCRETE = "discrete";

    @Override
    public beast.evolution.alignment.Alignment valueToBEAST(Value<SimpleAlignment> alignmentValue, BEASTContext context) {

        SimpleAlignment alignment = alignmentValue.value();
        SequenceType lphyDataType = alignment.getSequenceType();
        String[] taxaNames = alignment.getTaxaNames();

        beast.evolution.alignment.Alignment beastAlignment;
        // TODO BEAST special data types: StandardData, UserDataType, IntegerData
        // 1. Trait Alignment, always 1 site
        if (lphyDataType instanceof Standard standard && alignment.nchar()==1) {
            DataType beastDataType = DataTypeUtils.getUserDataType(standard, true);
            // AlignmentFromTrait
            beastAlignment = new beast.evolution.alignment.AlignmentFromTrait();
            // Input<DataType.Base> userDataTypeInput
            beastAlignment.setInputValue("userDataType", beastDataType);

            List<Taxon> taxonList = context.createTaxonList(List.of(taxaNames));
            String traitStr = createTraitString(alignment);

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
            DataType beastDataType = DataTypeUtils.getBEASTDataType(lphyDataType, context.getDataTypeMap());

            // 2. nucleotide, protein, ...
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
            beastAlignment.setInputValue("sequence", sequences);

            // 3. morphological data, needs extra <userDataType section
            if (beastDataType instanceof UserDataType) {
                // StandardData.getTypeDescription()
                beastAlignment.setInputValue("dataType", "standard");
                //TODO add FilteredAlignment ?
                beastAlignment.setInputValue("userDataType", beastDataType);

            } else {
                // Input<String> dataTypeInput
                beastAlignment.setInputValue("dataType", beastDataType.getTypeDescription());
            }

            beastAlignment.initAndValidate();

        }

        // using LPhy var as ID allows multiple alignments
        if (!alignmentValue.isAnonymous()) beastAlignment.setID(alignmentValue.getCanonicalId());

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
