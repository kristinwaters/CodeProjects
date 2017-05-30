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

# Compile C code
gcc -o matrix matrix.c

# Compile code
javac -d build src/cluster/*.java

# Create jar file
cd build
jar -cf cluster.jar cluster/*.class
cd ..

# Move jar to lib
mv build/cluster.jar lib/cluster.jar

# Clean up build files
rm -rf build
