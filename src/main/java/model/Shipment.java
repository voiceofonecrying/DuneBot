package model;

import constants.Emojis;
import controller.commands.CommandManager;
import exceptions.ChannelNotFoundException;
import model.factions.Faction;

import java.text.MessageFormat;

import static controller.commands.CommandManager.spiceMessage;

public class Shipment {
    private int force;
    private int specialForce;
    private Territory territory;
    private int cost;

    private boolean payGuild;

    public Shipment() {}

    public void execute(DiscordGame discordGame, Game game, Faction faction) throws ChannelNotFoundException {
        CommandManager.placeForceInTerritory(this.territory, faction, force, false);
        if (specialForce > 0) CommandManager.placeForceInTerritory(this.territory, faction, specialForce, true);
        StringBuilder message = new StringBuilder();
        message.append(faction.getEmoji())
                .append(": ");

        if (force > 0) {
            message.append(MessageFormat.format("{0} {1} ", force, Emojis.getForceEmoji(faction.getName())));
        }

        if (specialForce > 0) {
            message.append(MessageFormat.format("{0} {1} ", specialForce, Emojis.getForceEmoji(faction.getName() + "*")));
        }

        message.append(
                MessageFormat.format("placed on {0}",
                        territory.getTerritoryName()
                )
        );

        if (cost > 0) {
            message.append(
                    MessageFormat.format(" for {0} {1}",
                            cost, Emojis.SPICE
                    )
            );
            faction.subtractSpice(cost);
            spiceMessage(discordGame, cost, faction.getName(),
                    "shipment to " + territory.getTerritoryName(), false);
            if (payGuild) {
                message.append(" paid to " + Emojis.GUILD);
                game.getFaction("Guild").addSpice(cost);
                spiceMessage(discordGame, cost, "guild", faction.getEmoji() + " shipment", true);

            }
        }
        discordGame.sendMessage("turn-summary", message.toString());
        this.territory = null;
        this.cost = 0;
        this.force = 0;
        this.specialForce = 0;
        this.payGuild = false;
        discordGame.pushGame();
    }
    public int getForce() {
        return force;
    }

    public void setForce(int force) {
        this.force = force;
    }

    public int getSpecialForce() {
        return specialForce;
    }

    public void setSpecialForce(int specialForce) {
        this.specialForce = specialForce;
    }

    public Territory getTerritory() {
        return territory;
    }

    public void setTerritory(Territory territory) {
        this.territory = territory;
    }

    public int getCost() {
        return cost;
    }

    public void setCost(int cost) {
        this.cost = cost;
    }

    public boolean isPayGuild() {
        return payGuild;
    }

    public void setPayGuild(boolean payGuild) {
        this.payGuild = payGuild;
    }
}
