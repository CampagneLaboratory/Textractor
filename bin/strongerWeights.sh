#!/bin/sh
model=$1
perl bin/svm2weight.pl $model | sort -k2 -t: -n -r | head -20
