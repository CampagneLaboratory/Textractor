#!/bin/sh
featureNumber=$1

term=`head -${featureNumber} index/JBC-1995-basename.terms | tail -1 `
echo feature: $featureNumber term: $term
