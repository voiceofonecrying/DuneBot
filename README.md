# DuneBot
## Docker Build
To Build the docker image run the following command in the project directory: `docker build -t dunebot --progress=plain .`.

The following command will run the bot: `docker run -e TOKEN='REPLACE_WITH_DISCORD_TOKEN' --name dunebot -d dunebot`.

After that the bot will be running in the background, and you can stop it using docker commands!
