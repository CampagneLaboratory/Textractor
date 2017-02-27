#!/bin/sh
model=$1
bin/strongerWeights.sh tnt/svm_model | awk -F: '{if ($2>0.001) print $1}' | xargs -n1 bin/feature2term.sh
