# DuneBot

## Docker Build
To Build the docker image run the following command in the project directory: `docker build -t dunebot --progress=plain .`.

The following command will run the bot: `docker run -e TOKEN='REPLACE_WITH_DISCORD_TOKEN' --name dunebot -d dunebot`.

After that the bot will be running in the background, and you can stop it using docker commands!

## Logging

DuneBot uses SLF4J with Logback for structured logging.

### Viewing Logs

To view logs from a running Docker container:

```bash
# View all logs
docker logs dunebot

# Follow logs in real-time
docker logs -f dunebot

# View last 100 lines
docker logs --tail 100 dunebot
```

### Running with Debug Logging

To enable DEBUG level logging for troubleshooting:

```bash
# Local run with debug logging
java -Dlogback.configurationFile=logback-debug.xml -jar target/DuneBot-*-SNAPSHOT.jar

# Docker with debug logging
docker run -e JAVA_OPTS="-Dlogback.configurationFile=logback-debug.xml" \
           -e TOKEN='YOUR_DISCORD_TOKEN' \
           --name dunebot -d dunebot
```
