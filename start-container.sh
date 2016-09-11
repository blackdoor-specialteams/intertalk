#!/usr/bin/env bash

docker run --link postgres -d -p 4567:4567 \
 -e INTERTALK_DOMAIN \
 -e INTERTALK_DB_HOST=postgres \
 blackdoor/intertalk