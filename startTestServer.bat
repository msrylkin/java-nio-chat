@echo off
cd target
chcp 65001
java -cp w-1.0-SNAPSHOT.jar;w-1.0-SNAPSHOT.jar/model nio.Server

pause