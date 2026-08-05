#!/usr/bin/env bash
set -a
if [ -f ../.env ]; then
  source ../.env
elif [ -f ./.env ]; then
  source ./.env
fi
set +a

./mvnw spring-boot:run
