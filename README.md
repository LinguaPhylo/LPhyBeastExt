# LPhyBeast

Convert [LPhy](http://linguaphylo.github.io/) scripts to [BEAST 2](http://beast2.org/) XML 

## Dependencies

- [linguaPhylo](https://github.com/LinguaPhylo/linguaPhylo)

- [beast-outercore](https://github.com/LinguaPhylo/beast-outercore)

## Examples

The scripts and data are available in 
[linguaPhylo/examples](https://github.com/LinguaPhylo/linguaPhylo/examples).

A Kingman coalescent tree generative distribution for serially sampled data imported from _Dengue4.nex_.

```bash
LPhyBEAST simpleSerialCoalescentWithTaxaNex.lphy
```

Two partitions imported from _primate.nex_.

```bash
LPhyBEAST twoPartitionCoalescentNex.lphy
```


