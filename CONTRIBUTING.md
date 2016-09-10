## Starting test server

1. Start a postgres database
2. `. set-variables.sh` or otherwise configure environment variables
3. Baseline database `mvn flyway:migrate`
4. Start server `mvn exec:java`

Server should be listening on port 4567