#!/bin/sh
java -Xmx${jvm.max.heap.size.mb}m -Xdock:icon=${0%/*}/../Resources/logo.png -jar ${0%/*}/*.jar ExecutedFromLauncher