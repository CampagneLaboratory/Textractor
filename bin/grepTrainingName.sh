#grep the name list for SVM training: protein, cell, interaction, process
#
#

rm Names2.*

../../../bin/getRestTrainingName.pl
grep domain rest.term >Names2.subprotein.domain

grep -P 'peptides?$' rest.term >Names2.subprotein.peptide

grep -P '(motif)|(subunit)|(loop)|(((helix)|(chains?)|(sites?)|(residues?))$)' rest.term >Names2.subprotein.othersubprotein


../../../bin/getRestTrainingName.pl
grep binding$ rest.term >Names2.interaction.binding

grep -P phosphorylation$ rest.term >Names2.interaction.phosphorylation

grep -P '(activation)s?$' rest.term >Names2.interaction.activation

grep -P '(inhibition)s?$' rest.term >Names2.interaction.inhibition

grep -P '(interaction)|(merization)|(recognition)|( action)s?$' rest.term >Names2.interaction.otherinteraction

grep -P hydrolysis$ rest.term >Names2.interaction.hydrolysis



grep -P '(activit(y|(ies)))$' rest.term >Names2.process.activity

grep -P release$ rest.term >Names2.process.release

grep -P transfer$ rest.term >Names2.process.transfer

grep -P exchange$ rest.term >Names2.process.exchange

grep -P 'expressions?$' rest.term >Names2.process.expression

grep -P 'growth$' rest.term> Names2.process.growth

../../../bin/getRestTrainingName.pl
grep -P '((repair)|(damage)|(apoptosis)|(sion)|(tion)|(synthesis)|(influx)|(efflux)|(import)|(shock)|(transit)|(transport)|(uptake))$' rest.term | grep -vP ' cation$' >Names2.process.otherprocess

../../../bin/getRestTrainingName.pl
grep -P '\t((ca2)|(calcium))' rest.term |grep -vP '(cyt)|(calmodulin)|(CaM)' >Names2.process.otherca




../../../bin/getRestTrainingName.pl
grep cell rest.term | grep -P '((lines?)|(cells?))$' > Names2.cell

grep -P '((neuron)|(blast)|(cyte))s?$' rest.term >>Names2.cell



../../../bin/getRestTrainingName.pl
grep -P '(channels?)$' rest.term > Names2.protein.channel

grep -P '(receptors?)$' rest.term > Names2.protein.receptor

grep -P '(transporters?)$' rest.term > Names2.protein.transporter

grep -P '((enzymes?)|(ases?))( (.|I{1,3}|(IV)))?(-\d)?$' rest.term | grep -vP '(coenzyme)|((\t| )(base|phase)s?( |$))|(release$)' > Names2.protein.enzyme

grep -P '\tprotein|proteins?$' rest.term | grep -v domain | grep -v receptor$ | grep -v receptors$ | grep -vP 'ase( .)?$' | grep -v ases$ > Names2.protein.protein

grep -P '(mutant )|(mutants?$)' rest.term >Names2.protein.mutant

grep -P 'complex(es)?$' rest.term >Names2.protein.complex

grep -P 'CaM|hsp|(14-3-3|p53|p38|AChR|DnaK|EGFR|eNOS|eIF4H|ERK|GIRK|GRK|JNK|MAPK|SAPK|STAT|TFIIF)\d?$' rest.term >Names2.protein.otherprotein
grep -P 'G(i|o|s) ?(alpha|beta|gamma)' rest.term>>Names2.protein.otherprotein



../../../bin/getRestTrainingName.pl
grep DNA$ rest.term >Names2.gene.DNA

grep RNA$ rest.term >Names2.gene.RNA

grep promoter rest.term >Names2.gene.promoter

grep -P '(genes?)$' rest.term >Names2.gene.gene

grep -P '(intron)|(exon)|((oligonucleotide)|(primer)|(sequence)|(plasmid)|(vector)|(nucleosome)|(box))s?$' rest.term >Names2.gene.othergene


../../../bin/getRestTrainingName.pl
grep -P '(membranes?)$' rest.term >Names2.subcell.membrane

grep -P '(extracts?)$' rest.term >Names2.subcell.extract

grep -P '(lysates?)$' rest.term >Names2.subcell.lysate

grep -P '((vesicle)|(LDL)|(proteasome)|(assembly)|(mitochondria)|(nucleus))s?$' rest.term >Names2.subcell.othersubcell

../../../bin/getRestTrainingName.pl
grep -P '(\t|\s)cell' rest.term> Names2.subcell.othersubcell

grep -P 'muscle$' rest.term> Names2.tissue.muscle



../../../bin/getRestTrainingName.pl
grep ose$ rest.term >Names2.smallmolecule.sugar

grep -P '((acids?)|(lipids?))$' rest.term >Names2.smallmolecule.othersmallmolecule

grep -P '(( (an|cat)?ions?)|(Na)|(sodium)|(ca2)|(calcium)|(ca2 cyt)|(coppers?)|Zn|Cr|Cu)( (I{1,3}|(IV)|(VI)))?$' rest.term >Names2.smallmolecule.metal



grep -P 'assay|analysis|blot' rest.term >Names2.method
../../../bin/getRestTrainingName.pl

awk '{print $NF,"|",$6,$7,$8,$9,$10}' rest.term | sort >temp.rest

#more temp.rest