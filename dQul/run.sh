#!/usr/bin/env bash
set -a
source ../.env
set +a

ADD_OPENS="--add-opens=java.base/sun.nio.ch=ALL-UNNAMED \
           --add-opens=java.base/java.lang=ALL-UNNAMED \
           --add-opens=java.base/java.lang.invoke=ALL-UNNAMED \
           --add-opens=java.base/java.lang.reflect=ALL-UNNAMED \
           --add-opens=java.base/java.io=ALL-UNNAMED \
           --add-opens=java.base/java.nio=ALL-UNNAMED \
           --add-opens=java.base/java.util=ALL-UNNAMED"

export MAVEN_OPTS="$ADD_OPENS $MAVEN_OPTS"

./mvnw spring-boot:run
