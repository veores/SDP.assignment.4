#!/bin/sh
set -e
rm -rf out && mkdir out
javac -d out $(find src -name '*.java')
java -cp out com.example.orderfacade.OrderFacadeTests
