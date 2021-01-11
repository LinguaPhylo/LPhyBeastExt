# LPhyBeast

[![Build Status](https://github.com/LinguaPhylo/LPhyBeast/workflows/Lphy%20BEAST%20tests/badge.svg)](https://github.com/LinguaPhylo/LPhyBeast/actions?query=workflow%3A%22Lphy+BEAST+tests%22)


LPhyBEAST is a command-line program that takes an [LPhy](http://linguaphylo.github.io/) model specification, including a data block and produces a [BEAST 2](http://beast2.org/) XML input file. It therefore enables LPHY as an alternative way to succinctly express and communicate BEAST2 analyses.

## Dependencies

- [linguaPhylo](https://github.com/LinguaPhylo/linguaPhylo)

- [beast-outercore](https://github.com/LinguaPhylo/beast-outercore)

## Examples

The scripts and data are available in 
[linguaPhylo/examples](https://github.com/LinguaPhylo/linguaPhylo/tree/master/examples).

A Kingman coalescent tree generative distribution for serially sampled data imported from _Dengue4.nex_.

```bash
LPhyBEAST simpleSerialCoalescentWithTaxaNex.lphy
```

Two partitions imported from _primate.nex_.

```bash
LPhyBEAST twoPartitionCoalescentNex.lphy
```


