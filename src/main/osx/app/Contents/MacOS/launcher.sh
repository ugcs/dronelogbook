#!/bin/sh
java -Xmx${jvm.max.heap.size.mb}m -jar ${0%/*}/*.jar ExecutedFromLauncher