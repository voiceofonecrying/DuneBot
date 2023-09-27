# Game Setup

## New Game
The `/newgame` command sets up a new channel category for the game.
You should probably use a different player role and mod role for each game.

## Expansion Choices
Use the following commands to select options for the game:

`/setup show-game-options`
`/setup add-game-option`
`/setup remove-game-option`

Game options need to be selected before your un `/setup advance` for the first time.

## Setup Factions
Setup factions for users using the following command:

`/setup faction`

## Start Game Setup
Please make sure you've completed all previous steps (chosen expansions and setup factions) before continuing.

For the game setup you will run `/setup advance` multiple times until setup is finished.  `/setup advance` will print
out all the steps that will be done during setup.

### Traitor Selection
To select the traitor cards you will need to run `/setup traitor`.

If the Harkonnen can mulligan their traitors (and decide to do it) use the following command: `/setup harkonnen-mulligan`.

### Select Leader Skill Cards
To select the leader skill cards you will need to run `/setup leader-skill`.

### Place forces
Some forces will be placed automatically.
If any need to be manually places (Fremen) use the `/placeforces` command, setting `isShipment`
to `false` to avoid charging the player.

After placing the forces you can then run `/show board` to show the board in the turn summary.

### Storm Selection
To select the storm location run `/setstorm`, with the total storm movement.