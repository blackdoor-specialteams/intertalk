#!/usr/bin/env bash

docker run --link postgres -d -p 6668:6668 \
 -e INTERTALK_DOMAIN \
 -e INTERTALK_DB_HOST=postgres \
 blackdoor/intertalk