# LPhyBeast

[![Build Status](https://github.com/LinguaPhylo/LPhyBeast/workflows/Lphy%20BEAST%20tests/badge.svg)](https://github.com/LinguaPhylo/LPhyBeast/actions?query=workflow%3A%22Lphy+BEAST+tests%22)


LPhyBEAST is a command-line program that takes an
[LPhy](http://linguaphylo.github.io/) model specification,
including a data block and produces a [BEAST 2](http://beast2.org/)
XML input file. 
It therefore enables LPHY as an alternative way to succinctly
express and communicate BEAST2 analyses.

## Setup and Commands

A simple usage of LPhyBEAST to create `RSV2.xml` given 
the LPhy script [RSV2.lphy](https://github.com/LinguaPhylo/linguaPhylo/blob/master/tutorials/RSV2.lphy).

```bash
LPhyBEAST tutorials/RSV2.lphy
```

Create 10 XMLs for simulations:

```bash
LPhyBEAST -r 10 tutorials/RSV2.lphy
```

More usage details are [here](https://linguaphylo.github.io/setup/).
More scripts are available in 
[linguaPhylo/examples](https://github.com/LinguaPhylo/linguaPhylo/tree/master/examples).

## Tutorials

LPhyBEAST [Tutorials](https://linguaphylo.github.io/tutorials/)


## Dependencies

- [linguaPhylo](https://github.com/LinguaPhylo/linguaPhylo)

BEAST 2 packages:

- [beast2](http://www.github.com/CompEvol/beast2)
- [BEASTLabs](https://github.com/BEAST2-Dev/BEASTLabs/)
- [feast](https://github.com/BEAST2-Dev/BEASTLabs/)
- [BEAST_CLASSIC](https://github.com/BEAST2-Dev/beast-classic/)
- [SMM](https://github.com/BEAST2-Dev/substmodels/)

All released BEAST 2 packages are listed in
[Package Viewer](https://compevol.github.io/CBAN/).

