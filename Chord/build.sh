#!/bin/bash

# Create directories
if [ -d build ]; then
  rm -rf build
fi

if [ -d lib ]; then
  rm -rf lib
fi

mkdir build
mkdir lib

# Compile code
javac -d build src/chord/*.java

# Create jar file
cd build
jar -cfe chord.jar chord/NodeSetup chord/*.class
jar -cfe SingleNodeSetup.jar chord/SingleNodeSetup chord/*.class
cd ..

# Move jar to lib
mv build/chord.jar lib/chord.jar
mv build/SingleNodeSetup.jar lib/chord.SingleNodeSetup

# Clean up build files
rm -rf build
