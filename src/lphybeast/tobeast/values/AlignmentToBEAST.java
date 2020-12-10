package lphybeast.tobeast.values;

import beast.evolution.alignment.AlignmentFromTrait;
import beast.evolution.alignment.Sequence;
import beast.evolution.alignment.Taxon;
import beast.evolution.alignment.TaxonSet;
import beast.evolution.datatype.UserDataType;
import beast.evolution.tree.TraitSet;
import jebl.evolution.sequences.SequenceType;
import lphy.evolution.alignment.SimpleAlignment;
import lphy.evolution.sequences.Standard;
import lphy.graphicalModel.Value;
import lphybeast.BEASTContext;
import lphybeast.ValueToBEAST;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class AlignmentToBEAST implements ValueToBEAST<SimpleAlignment, beast.evolution.alignment.Alignment> {

    private static final String DISCRETE = "discrete";

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

        String datatype = getBEASTDataType(alignment);
        if (datatype.equalsIgnoreCase(Standard.NAME)) {
            beastAlignment = new beast.evolution.alignment.AlignmentFromTrait();
            // for trait alignment
            UserDataType userDataType = getUserDataType(alignment);
            beastAlignment.setInputValue("userDataType", userDataType);

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

            AlignmentFromTrait traitAlignment = new AlignmentFromTrait();
            beastAlignment.initByName("traitSet", traitSet, "userDataType", userDataType);

        } else {

            beastAlignment = new beast.evolution.alignment.Alignment();
            beastAlignment.initByName("sequence", sequences, "dataType", datatype);

        }

        // using LPhy var as ID allows multiple alignments
        if (!alignmentValue.isAnonymous()) beastAlignment.setID(alignmentValue.getCanonicalId());
        return beastAlignment;
    }

    // TODO better solution to getDataType for BEAST from Lphy Alignment
    private String getBEASTDataType(SimpleAlignment alignment) {
        return alignment.getSequenceTypeStr();
    }

    // for trait alignment
    private UserDataType getUserDataType(SimpleAlignment alignment) {
        // userDataType: non-standard, user specified data type, if specified 'dataType' is ignored
        UserDataType userDataType = new UserDataType();
        // no ambiguous
//        int stateCount = alignment.getCanonicalStateCount();

        SequenceType sequenceType = alignment.getSequenceType();
        if (! (sequenceType instanceof Standard))
            throw new IllegalArgumentException("Standard data type is required ! " + sequenceType.getName());

        List<String> stateNames = ((Standard) sequenceType).getStateNames();
        String codeMap = IntStream.range(0, stateNames.size())
                .mapToObj(i -> stateNames.get(i) + "=" + i)
                .collect(Collectors.joining(","));
        codeMap += ", ? = " + IntStream.range(0, stateNames.size()).mapToObj(String::valueOf)
                .collect(Collectors.joining(" "));

        // codeMap="Asia=0,EU=1,NZ=2,RoW=3,USA=4,? = 0 1 2 3 4" codelength="-1" states="5"
        userDataType.initByName("codeMap", codeMap,
                "codelength", -1, "states", stateNames.size());
        // this will create codeMapping in StandardData
//        userDataType.setInputValue("nrOfStates", alignment.getSequenceType().getCanonicalStateCount());
        userDataType.initAndValidate();
        return userDataType;
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
