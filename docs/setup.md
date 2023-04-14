# Game Setup

## New Game
The `/newgame` command sets up a new channel category for the game.
You should probably use a different player rold and mod role for each game.

## Expansion Choices
Use the following command to select options for the game:

`/setup expansion-choices`

## Setup Factions
Setup factions for users using the following command:

`/setup faction`

## Start Game Setup
Please make sure you've completed all previous steps (chosen expansions and setup factions) before continuing.

For the game setup you will run `/setup advance` multiple times until setup is finished.
This command will perform the following steps

If Leader Skills are enabled:
1. Setup faction positions and BG prediction
2. Treachery Cards
3. Select Leader Skill Cards
4. Show Leader Skill Cards
5. Traitor Selection
6. Face Dancers (step will still run even if BT is not in the game, it just won't do anything).
7. Storm Selection
8. Finish Setup mode and start the game.

If Leader Skills are not enabled:
1. Setup faction positions and BG prediction
2. Traitor Selection
3. Face Dancers (step will still run even if BT is not in the game, it just won't do anything).
4. Treachery Cards
5. Storm Selection
6. Finish Setup mode and start the game.

### Traitor Selection
To select the traitor cards you will need to run `/setup traitor`.

### Select Leader Skill Cards
To select the leader skill cards you will need to run `/setup faction-leader-skill`.

### Place forces
Some forces will be placed automatically.
If any need to be manually places (Fremen) use the `/placeforces` command, setting `isShipment`
to `false` to avoid charging the player.

After placing the forces you can then run `/show board` to show the board in the turn summary.