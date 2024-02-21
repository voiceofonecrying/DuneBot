package controller.commands;

import constants.Emojis;
import enums.GameOption;
import enums.UpdateType;
import exceptions.ChannelNotFoundException;
import controller.DiscordGame;
import model.Force;
import model.Game;
import model.Territory;
import model.factions.BGFaction;
import model.factions.Faction;
import model.factions.FremenFaction;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static controller.commands.CommandOptions.bgTerritories;
import static controller.commands.CommandOptions.territory;

public class BGCommands {
    public static List<CommandData> getCommands() {

        List<CommandData> commandData = new ArrayList<>();
        commandData.add(
                Commands.slash("bg", "Commands related to the BG Faction.").addSubcommands(
                        new SubcommandData(
                                "flip",
                                "Flip to advisor or fighter in a territory."
                        ).addOptions(bgTerritories),
                        new SubcommandData(
                                "advise",
                                "Advise to a territory or to the polar sink."
                        ).addOptions(territory)
                )
        );

        return commandData;
    }

    public static void runCommand(SlashCommandInteractionEvent event, DiscordGame discordGame, Game game) throws ChannelNotFoundException, IOException {
        String name = event.getSubcommandName();
        if (name == null) throw new IllegalArgumentException("Invalid command name: null");

        switch (name) {
            case "flip" -> flip(discordGame, game);
            case "advise" -> adviseEventHandler(discordGame, game);
        }
    }

    private static void adviseEventHandler(DiscordGame discordGame, Game game) throws ChannelNotFoundException, IOException {
        Territory territory = game.getTerritory(discordGame.required(CommandOptions.territory).getAsString());
        advise(discordGame, game, territory, 1);
    }

    public static void advise(DiscordGame discordGame, Game game, Territory territory, int amount) throws ChannelNotFoundException, IOException {
        Faction bg = game.getFaction("BG");
        if (amount == 2 && !bg.isHighThreshold()) {
            discordGame.getFactionChat(bg).queueMessage("You are at Low Threshold and cannot send 2 " + Emojis.BG_ADVISOR);
            amount = 1;
        }
        CommandManager.placeForceInTerritory(discordGame, game, territory, bg, amount, false);
        int fighters = territory.getForce("BG").getStrength();
        territory.getForces().removeIf(force -> force.getName().equals("BG"));
        territory.getForces().add(new Force("Advisor", fighters));
        discordGame.queueMessage("You sent " + amount + " " + Emojis.BG_ADVISOR + " to " + territory.getTerritoryName());
        discordGame.getTurnSummary().queueMessage(Emojis.BG + " sent " + amount + " " + Emojis.BG_ADVISOR + " to " + territory.getTerritoryName());
        discordGame.pushGame();
        game.setUpdated(UpdateType.MAP);
    }

    public static void flip(DiscordGame discordGame, Game game) throws ChannelNotFoundException, IOException {
        Territory territory = game.getTerritories().get(discordGame.required(bgTerritories).getAsString());
        flip(discordGame, game, territory);
    }

    public static void flip(DiscordGame discordGame, Game game, Territory territory) throws ChannelNotFoundException, IOException {
        int strength = 0;
        String found = "";
        for (Force force : territory.getForces()) {
            if (force.getName().equals("BG") || force.getName().equals("Advisor")) {
                strength += force.getStrength();
                found = force.getName();
            }
        }
        territory.getForces().removeIf(force -> force.getName().equals("BG") || force.getName().equals("Advisor"));
        if (found.equals("Advisor")) territory.getForces().add(new Force("BG", strength));
        else if (found.equals("BG")) territory.getForces().add(new Force("Advisor", strength));
        else {
            discordGame.getModInfo().queueMessage("No Bene Gesserit were found in that territory.");
            return;
        }
        discordGame.pushGame();
        game.setUpdated(UpdateType.MAP);
    }

    public static void presentAdvisorButtons(DiscordGame discordGame, Game game, Faction targetFaction, Territory targetTerritory) throws ChannelNotFoundException {
        if (game.hasFaction("BG")
                && !(targetFaction instanceof BGFaction || targetFaction instanceof FremenFaction)
                && !(
                game.hasGameOption(GameOption.HOMEWORLDS)
                        && !game.getFaction("BG").isHighThreshold()
                        && !game.getHomeworlds().containsValue(targetTerritory.getTerritoryName())
        )) {
            List<Button> buttons = new LinkedList<>();
            String territoryName = targetTerritory.getTerritoryName();
            buttons.add(Button.primary("bg-advise-" + territoryName, "Advise"));
            buttons.add(Button.secondary("bg-advise-Polar Sink", "Advise to Polar Sink"));

            if (game.hasGameOption(GameOption.HOMEWORLDS)) {
                buttons.add(Button.secondary("bg-ht", "Advise 2 to Polar Sink"));
            }

            buttons.add(Button.danger("bg-dont-advise-" + territoryName, "No"));
            discordGame.getBGChat().queueMessage(Emojis.BG + " Would you like to advise the shipment to " + territoryName + "? " + game.getFaction("BG").getPlayer(), buttons);
        }
    }
}
