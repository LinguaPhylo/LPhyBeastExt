package lphybeast.tobeast.generators;

import beast.core.BEASTInterface;
import beast.evolution.alignment.Taxon;
import beast.evolution.alignment.TaxonSet;
import beast.evolution.tree.Tree;
import lphy.evolution.Taxa;
import lphy.evolution.coalescent.MultispeciesCoalescent;
import lphy.evolution.tree.TimeTree;
import lphybeast.BEASTContext;
import lphybeast.GeneratorToBEAST;
import starbeast2.SpeciesTree;

import java.util.ArrayList;
import java.util.List;

public class MultispeciesCoalescentToStarBEAST2 implements
        GeneratorToBEAST<MultispeciesCoalescent, starbeast2.MultispeciesCoalescent> {
    @Override
    public starbeast2.MultispeciesCoalescent generatorToBEAST(MultispeciesCoalescent generator, BEASTInterface value, BEASTContext context) {

        starbeast2.MultispeciesCoalescent starbeast = new starbeast2.MultispeciesCoalescent();

        Tree geneTree = (Tree) value;

        starbeast2.StarBeastTaxonSet starBeastTaxonSet = createStarBeastTaxonSet(
                generator.getSpeciesTree().value().getTaxa(),
                ((TimeTree)context.getGraphicalModelNode(geneTree).value()).getTaxa());

        Tree speciesTree = convertToStarBEASTSpeciesTree((Tree)context.getBEASTObject(generator.getSpeciesTree()), starBeastTaxonSet);
        // replace species tree in context with newly converted tree so that tree operators are attached to the correct tree.
        context.putBEASTObject(generator.getSpeciesTree(), speciesTree);

        starbeast2.ConstantPopulations populationModel = new starbeast2.ConstantPopulations();
        populationModel.setInputValue("speciesTree", speciesTree);
        populationModel.setInputValue("populationSizes", context.getBEASTObject(generator.getPopulationSizes()));
        populationModel.initAndValidate();

        starbeast2.GeneTree starbeast2GeneTree = new starbeast2.GeneTree();
        starbeast2GeneTree.setInputValue("speciesTree", speciesTree);
        starbeast2GeneTree.setInputValue("tree", geneTree);
        starbeast2GeneTree.setInputValue("populationModel", populationModel);
        starbeast2GeneTree.initAndValidate();

        starbeast.setInputValue("distribution", starbeast2GeneTree);
        starbeast.initAndValidate();

        return starbeast;
    }

    /**
     * @param speciesTaxa a taxa object containing the species taxa
     * @param geneTaxa a taxa object containing the individual taxa in gene tree and associated species.
     * @return a taxonsuperset using information from the species taxa and gene taxa provided.
     */
    private starbeast2.StarBeastTaxonSet createStarBeastTaxonSet(Taxa speciesTaxa, Taxa geneTaxa) {

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
                    Taxon geneTaxon = new Taxon();
                    geneTaxon.initAndValidate();
                    geneTaxon.setID(taxon.getName());
                    geneTaxonList.add(geneTaxon);
                    System.out.println("  taxon " + taxon.getName());

                }
            }
            spTaxonSet.setInputValue("taxon", geneTaxonList);
            spTaxonSet.initAndValidate();
            spTaxonSet.setID(speciesId);
            spTaxonSets.add(spTaxonSet);
        }

        sbtaxonSuperSet.setInputValue("taxon", spTaxonSets);
        sbtaxonSuperSet.initAndValidate();
        return sbtaxonSuperSet;
    }

    /**
     * Takes a BEAST tree and, if necessary, converts it to a starbeast2.SpeciesTree
     * @param tree
     * @return
     */
    private starbeast2.SpeciesTree convertToStarBEASTSpeciesTree(Tree tree, TaxonSet taxonSet) {

        // TODO might need to update taxon set for each new gene tree
        if (tree instanceof SpeciesTree) return (SpeciesTree)tree;

        SpeciesTree speciesTree = new SpeciesTree();
        speciesTree.setInputValue("initial", tree);
        speciesTree.setInputValue("taxonset", taxonSet);
        speciesTree.initAndValidate();
        return speciesTree;
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
