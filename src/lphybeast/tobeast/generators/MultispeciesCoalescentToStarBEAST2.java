package lphybeast.tobeast.generators;

import beast.core.BEASTInterface;
import beast.evolution.alignment.Taxon;
import beast.evolution.alignment.TaxonSet;
import beast.evolution.tree.TraitSet;
import beast.evolution.tree.Tree;
import lphy.evolution.Taxa;
import lphy.evolution.alignment.Alignment;
import lphy.evolution.coalescent.MultispeciesCoalescent;
import lphy.evolution.tree.TimeTree;
import lphy.graphicalModel.Value;
import lphybeast.BEASTContext;
import lphybeast.GeneratorToBEAST;
import starbeast2.GeneTree;
import starbeast2.PopulationModel;
import starbeast2.SpeciesTree;
import starbeast2.StarBeastTaxonSet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static java.util.Arrays.*;

public class MultispeciesCoalescentToStarBEAST2 implements
        GeneratorToBEAST<MultispeciesCoalescent, starbeast2.MultispeciesCoalescent> {
    @Override
    public starbeast2.MultispeciesCoalescent generatorToBEAST(MultispeciesCoalescent generator, BEASTInterface value, BEASTContext context) {

        starbeast2.MultispeciesCoalescent starbeast = new starbeast2.MultispeciesCoalescent();

        Tree geneTree = (Tree) value;

        SpeciesTree speciesTree = (SpeciesTree)context.getBEASTObject(generator.getSpeciesTree());
        StarBeastTaxonSet starBeastTaxonSet = (StarBeastTaxonSet)speciesTree.getTaxonset();

        starbeast2.ConstantPopulations constantPopulations = new starbeast2.ConstantPopulations();
        constantPopulations.setInputValue("speciesTree", speciesTree);
        constantPopulations.setInputValue("populationSizes", context.getBEASTObject(generator.getPopulationSizes()));
        constantPopulations.initAndValidate();

// PASSTHROUGH MODEL ONLY NEEDED FOR BEAUTI WORKAROUND
//        starbeast2.PassthroughModel populationModel = new starbeast2.PassthroughModel();
//        populationModel.setInputValue("childModel", constantPopulations);
//        populationModel.initAndValidate();

        starbeast2.GeneTree starbeast2GeneTree = new starbeast2.GeneTree();
        starbeast2GeneTree.setInputValue("speciesTree", speciesTree);
        starbeast2GeneTree.setInputValue("tree", geneTree);
        starbeast2GeneTree.setInputValue("populationModel", constantPopulations);
        starbeast2GeneTree.initAndValidate();

        starbeast.setInputValue("distribution", starbeast2GeneTree);
        starbeast.initAndValidate();

        List<Tree> geneTrees = asList(geneTree);
        List<GeneTree> geneTreeDists = asList(starbeast2GeneTree);

        starbeast2.StarBeastInitializer starBeastInitializer = createStarBEASTInitializer(speciesTree, geneTrees, constantPopulations);
        starBeastInitializer.setID("SBI");

        context.addInit(starBeastInitializer);

        addOperators(starBeastTaxonSet, speciesTree, geneTrees, geneTreeDists, context);

        return starbeast;
    }

    public void modifyBEASTValues(MultispeciesCoalescent generator, BEASTInterface value, BEASTContext context) {
        Tree geneTree = (Tree) value;
        Tree vanillaSpeciesTree =  (Tree) context.getBEASTObject(generator.getSpeciesTree());
        starbeast2.StarBeastTaxonSet starBeastTaxonSet = createStarBeastTaxonSet(
                generator.getSpeciesTree().value().getTaxa(),
                ((TimeTree) context.getGraphicalModelNode(geneTree).value()).getTaxa(), context);

        TraitSet traitSet = createTraitSet(generator.getSpeciesTree().value(), starBeastTaxonSet);

        SpeciesTree speciesTree = convertToStarBEASTSpeciesTree(vanillaSpeciesTree, traitSet, starBeastTaxonSet);
        speciesTree.setID(vanillaSpeciesTree.getID());
        // replace species tree in context with newly converted tree so that tree operators are attached to the correct tree.
        context.removeBEASTObject(vanillaSpeciesTree);
        context.putBEASTObject(generator.getSpeciesTree(), speciesTree);
    }

    private starbeast2.StarBeastInitializer createStarBEASTInitializer(SpeciesTree tree, List<Tree> geneTree, PopulationModel populationModel) {

        starbeast2.StarBeastInitializer starBeastInitializer = new starbeast2.StarBeastInitializer();
        starBeastInitializer.setInputValue("speciesTree", tree);
        starBeastInitializer.setInputValue("geneTree", geneTree);
        starBeastInitializer.setInputValue("estimate", false);
        starBeastInitializer.setInputValue("populationModel", populationModel);
        starBeastInitializer.initAndValidate();

        return starBeastInitializer;

    }

    private void addOperators(starbeast2.StarBeastTaxonSet starBeastTaxonSet, SpeciesTree tree, List<Tree> geneTree, List<GeneTree> geneTreeDists, BEASTContext context) {

        int totalNodeCount = tree.getInternalNodeCount();
        for (Tree gTree : geneTree) {
            totalNodeCount += gTree.getInternalNodeCount();
        }

        starbeast2.NodeReheight2 nodeReheight2 = new starbeast2.NodeReheight2();
        nodeReheight2.setInputValue("taxonset", starBeastTaxonSet);
        nodeReheight2.setInputValue("tree", tree);
        nodeReheight2.setInputValue("geneTree", geneTreeDists);
        nodeReheight2.setInputValue("weight", BEASTContext.getOperatorWeight(totalNodeCount));
        nodeReheight2.initAndValidate();
        nodeReheight2.setID(tree.getID()+".nodeReheight2");

        context.addExtraOperator(nodeReheight2);

        starbeast2.CoordinatedUniform coordinatedUniform = new starbeast2.CoordinatedUniform();
        coordinatedUniform.setInputValue("speciesTree", tree);
        coordinatedUniform.setInputValue("geneTree", geneTree);
        coordinatedUniform.setInputValue("weight", BEASTContext.getOperatorWeight(totalNodeCount));
        coordinatedUniform.initAndValidate();
        coordinatedUniform.setID(tree.getID()+".coordinatedUniform");

        context.addExtraOperator(coordinatedUniform);

//        starbeast2.CoordinatedExponential coordinatedExponential = new starbeast2.CoordinatedExponential();
//        coordinatedExponential.setInputValue("speciesTree", tree);
//        coordinatedExponential.setInputValue("geneTree", geneTree);
//        coordinatedExponential.setInputValue("weight", BEASTContext.getOperatorWeight(totalNodeCount));
//        coordinatedExponential.initAndValidate();
//        coordinatedExponential.setID(tree.getID()+".coordinatedExponential");
//
//        context.addExtraOperator(coordinatedExponential);
    }

    /**
     * @param speciesTaxa a taxa object containing the species taxa
     * @param geneTaxa    a taxa object containing the individual taxa in gene tree and associated species.
     * @return a taxonsuperset using information from the species taxa and gene taxa provided.
     */
    private starbeast2.StarBeastTaxonSet createStarBeastTaxonSet(Taxa speciesTaxa, Taxa geneTaxa, BEASTContext context) {

        List<String> speciesTaxaNames = Arrays.asList(speciesTaxa.getTaxaNames());

        List<Value<Alignment>> alignments = context.getAlignments();

        Value<lphy.evolution.alignment.Alignment> a = null;
        for (Value<lphy.evolution.alignment.Alignment> candidate : alignments) {
            List<String> taxaNames = Arrays.asList(candidate.value().getTaxa().getTaxaNames());
            if (taxaNames.containsAll(speciesTaxaNames)) {
                a = candidate;
                break;
            }
        }

        // This is the mapping from gene tree taxa to species tree taxa
        starbeast2.StarBeastTaxonSet sbtaxonSuperSet = new starbeast2.StarBeastTaxonSet();
        List<Taxon> spTaxonSets = new ArrayList<>();
        for (String speciesId : speciesTaxa.getTaxaNames()) {

            System.out.println("Species " + speciesId);

            TaxonSet spTaxonSet = new TaxonSet();
            List<Taxon> geneTaxonList = new ArrayList<>();
            for (lphy.evolution.Taxon taxon : geneTaxa.getTaxonArray()) {
                System.out.println("  gene taxon: " + taxon);

                if (taxon.getSpecies().equals(speciesId)) {
                    Taxon geneTaxon = context.getTaxon(taxon.getName());
                    geneTaxonList.add(geneTaxon);
                    System.out.println("  taxon " + taxon.getName());
                }
            }
            if (geneTaxonList.size() > 0) {
                spTaxonSet.setInputValue("taxon", geneTaxonList);
                spTaxonSet.initAndValidate();
                spTaxonSet.setID(speciesId);
                spTaxonSets.add(spTaxonSet);
            }
        }

        sbtaxonSuperSet.setInputValue("taxon", spTaxonSets);
        if (a != null) {
            sbtaxonSuperSet.setInputValue("alignment", context.getBEASTObject(a));
        }
        sbtaxonSuperSet.initAndValidate();
        return sbtaxonSuperSet;
    }

    /**
     * Takes a BEAST tree and, if necessary, converts it to a starbeast2.SpeciesTree
     *
     * @param tree
     * @return
     */
    private starbeast2.SpeciesTree convertToStarBEASTSpeciesTree(Tree tree, TraitSet traitSet, TaxonSet taxonSet) {

        tree.m_taxonset.set(taxonSet);

        // TODO might need to update taxon set for each new gene tree
        if (tree instanceof SpeciesTree) return (SpeciesTree) tree;

        SpeciesTree speciesTree = new SpeciesTree();
        //speciesTree.setInputValue("initial", tree);
        speciesTree.setInputValue("trait", traitSet);
        speciesTree.setInputValue("taxonset", taxonSet);
        speciesTree.initAndValidate();

        return speciesTree;
    }

    private TraitSet createTraitSet(TimeTree tree, TaxonSet taxonSuperSet) {

        StringBuilder builder = new StringBuilder();
        lphy.evolution.Taxon[] taxonArray = tree.getTaxa().getTaxonArray();
        builder.append(taxonArray[0].getName());
        builder.append("=");
        builder.append(taxonArray[0].getAge());

        for (int i = 1; i < taxonArray.length; i++) {
            builder.append(",\n");
            builder.append(taxonArray[i].getName());
            builder.append("=");
            builder.append(taxonArray[i].getAge());
        }
        builder.append("\n");
        String traitValueString = builder.toString();

        TraitSet traitSet = new TraitSet();
        traitSet.setInputValue("traitname", "date-backward");
        traitSet.setInputValue("value", traitValueString);
        traitSet.setInputValue("taxa", taxonSuperSet);
        traitSet.initAndValidate();

        return traitSet;
    }

    @Override
    public Class<MultispeciesCoalescent> getGeneratorClass() {
        return MultispeciesCoalescent.class;
    }

    @Override
    public Class<starbeast2.MultispeciesCoalescent> getBEASTClass() {
        return starbeast2.MultispeciesCoalescent.class;
    }
}
