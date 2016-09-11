## Starting test server

1. Start a postgres database (`docker run --name postgres -p 5432:5432 -d postgres`)
2. `. set-variables.sh` or otherwise configure environment variables
3. Baseline database `mvn flyway:migrate`
4. Start server `mvn exec:java`

Server should be listening on port 4567

## Starting server in containers

1. Start database `./start-database.sh`
2. Set domain `export INTERTALK_DOMAIN='mysite.com'`
3. Start server `./start-container.sh`
3. Set variables for migration `. set-variables.sh`
4. Migrate db `mvn flyway:migrate`

> note: 3 and 4 only need to be done once