@echo off
if exist out rmdir /s /q out
mkdir out
dir /s /b src\*.java > sources.txt
javac -d out @sources.txt || exit /b 1
del sources.txt
java -cp out com.example.orderfacade.client.Main
