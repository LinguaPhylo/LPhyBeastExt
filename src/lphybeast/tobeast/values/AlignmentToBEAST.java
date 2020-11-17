package lphybeast.tobeast.values;

import beast.evolution.alignment.Sequence;
import beast.evolution.datatype.StandardData;
import lphy.evolution.alignment.SimpleAlignment;
import lphy.evolution.sequences.Standard;
import lphy.graphicalModel.Value;
import lphybeast.BEASTContext;
import lphybeast.ValueToBEAST;

import java.util.ArrayList;
import java.util.List;

public class AlignmentToBEAST implements ValueToBEAST<SimpleAlignment, beast.evolution.alignment.Alignment> {

    @Override
    public beast.evolution.alignment.Alignment valueToBEAST(Value<SimpleAlignment> alignmentValue, BEASTContext context) {

        SimpleAlignment alignment = alignmentValue.value();
//        SequenceType sequenceType = alignment.getSequenceType();// binary has no datatype
        String[] taxaNames = alignment.getTaxaNames();
        beast.evolution.alignment.Alignment beastAlignment;

        List<Sequence> sequences = new ArrayList<>();

        for (int i = 0; i < taxaNames.length; i++) {
            context.addTaxon(taxaNames[i]);
            // have to convert to string, cannot use integer state
            String s = alignment.getSequence(i);
            sequences.add(createBEASTSequence(taxaNames[i], s));
        }

        beastAlignment = new beast.evolution.alignment.Alignment();

        String datatype = getBEASTDataType(alignment);
        beastAlignment.setInputValue("dataType", datatype);
        if (datatype.equals(Standard.NAME)) {
            StandardData type = new StandardData();
            // this will create codeMapping in StandardData
            type.setInputValue("nrOfStates", alignment.getSequenceType().getCanonicalStateCount());
            type.initAndValidate();
            beastAlignment.setInputValue("userDataType", type);
        }
        beastAlignment.setInputValue("sequence", sequences);
        beastAlignment.initAndValidate();

        // using LPhy var as ID allows multiple alignments
        if (!alignmentValue.isAnonymous()) beastAlignment.setID(alignmentValue.getCanonicalId());
        return beastAlignment;
    }

    // TODO better solution to getDataType
    private String getBEASTDataType(SimpleAlignment alignment) {
        return alignment.getSequenceTypeStr();
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
