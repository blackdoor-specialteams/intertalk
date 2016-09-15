#!/usr/bin/env bash

docker run --link postgres -d -p 9869:9869 \
 -e INTERTALK_DOMAIN \
 -e INTERTALK_DB_HOST=postgres \
 blackdoor/intertalk