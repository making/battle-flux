#!/bin/bash
set -e

names="$names BattleRSocketClient"
names="$names BattleRSocketServer"

mvn clean
for name in $names;do
  mvn package -DskipTests=true -Dstart-class=com.example.${name}
  mv target/battle-flux-1.0-SNAPSHOT.jar target/${name}.jar
done
