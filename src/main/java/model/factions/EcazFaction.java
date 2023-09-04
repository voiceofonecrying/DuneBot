package model.factions;

import constants.Emojis;
import controller.commands.CommandManager;
import exceptions.ChannelNotFoundException;
import model.*;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class EcazFaction extends Faction {
    private final List<String> ambassadorPool;
    private final List<String> ambassadorSupply;

    private Leader loyalLeader;

    public EcazFaction(String player, String userName, Game game) throws IOException {
        super("Ecaz", player, userName, game);

        setSpice(12);
        this.freeRevival = 2;
        this.reserves = new Force("Ecaz", 14);
        this.emoji = Emojis.ECAZ;
        game.getTerritories().get("Imperial Basin (Center Sector)").getForces().add(new Force("Ecaz", 6));
        this.ambassadorPool = new LinkedList<>();
        this.ambassadorSupply = new LinkedList<>();
        ambassadorPool.add("Atreides");
        ambassadorPool.add("BG");
        ambassadorPool.add("CHOAM");
        ambassadorPool.add("Emperor");
        ambassadorPool.add("Fremen");
        ambassadorPool.add("Harkonnen");
        ambassadorPool.add("Ix");
        ambassadorPool.add("Richese");
        ambassadorPool.add("Guild");
        ambassadorPool.add("BT");
        drawNewSupply();
    }

    public void drawNewSupply() {
        this.ambassadorSupply.clear();
        Collections.shuffle(ambassadorPool);

        for (int i = 0; i < 5; i++) {
            ambassadorSupply.add(ambassadorPool.get(i));
        }
        ambassadorSupply.add("Ecaz");
    }

    public void triggerAmbassador(Game game, DiscordGame discordGame, Faction triggeringFaction, String ambassador) throws ChannelNotFoundException {

        discordGame.queueMessage("The " + ambassador + " ambassador has been triggered!");

        switch (ambassador) {
            case "Ecaz" -> {
                Button getVidal = Button.primary("ecaz-get-vidal", "Get Duke Vidal");
                Button offerAlliance = Button.primary("ecaz-offer-alliance-" + triggeringFaction.getName(), "Offer Alliance");
                if (game.getLeaderTanks().stream().anyMatch(leader -> leader.name().equals("Duke Vidal"))
                        || (game.hasFaction("Harkonnen") && game.getFaction("Harkonnen").getLeaders().stream().anyMatch(leader -> leader.name().equals("Duke Vidal")))
                        || (game.hasFaction("BT") && game.getFaction("BT").getLeaders().stream().anyMatch(leader -> leader.name().equals("Duke Vidal")))) getVidal = getVidal.asDisabled();
                if (game.getFaction("Ecaz").hasAlly() || triggeringFaction.hasAlly()) offerAlliance = offerAlliance.asDisabled();
                discordGame.prepareMessage("ecaz-chat", "Your Ecaz Ambassador has been triggered by " + triggeringFaction.getEmoji() + "! Which would you like to do?")
                        .addActionRow(getVidal, offerAlliance).queue();
                ambassadorPool.add("Ecaz");
            }
            case "Atreides" -> discordGame.queueMessage("mod-info", "Atreides ambassador token was triggered, please show Ecaz player the " + triggeringFaction.getEmoji() + " hand.");
            case "BG" -> {
                List<String> triggerOptions = ambassadorPool.subList(5, 9);
                List<Button> buttons = new LinkedList<>();
                for (String option : triggerOptions) {
                    buttons.add(Button.primary("ecaz-bg-trigger-" + option + "-" + triggeringFaction.getName(), option));
                }
                discordGame.prepareMessage("ecaz-chat", "Your Bene Gesserit Ambassador has been triggered by " + triggeringFaction.getEmoji() + "! Which ambassador token not from your supply would you like to trigger?")
                        .addActionRow(buttons).queue();
                ambassadorPool.removeIf(s -> s.equals("Bene Gesserit"));
            }
            case "CHOAM" -> discordGame.queueMessage("mod-info", "CHOAM ambassador token was triggered, please discard Ecaz treachery cards for 3 spice each");
            case "Emperor" -> {
                CommandManager.spiceMessage(discordGame, 5, "ecaz", Emojis.EMPEROR + " ambassador token", true);
                addSpice(5);
            }
            case "Fremen" -> discordGame.queueMessage("mod-info", "Fremen ambassador token was triggered, Ecaz player may move a group of forces on the board to any territory.");
            case "Harkonnen" -> discordGame.queueMessage("mod-info", "Harkonnen ambassador token was triggered by " + triggeringFaction.getEmoji() + ", please show Ecaz player a random traitor card that " + triggeringFaction.getEmoji() + " holds.");
            case "Ix" -> discordGame.queueMessage("mod-info", "Ixian ambassador token was triggered, Ecaz may discard a treachery card and draw a new one.");
            case "Richese" -> discordGame.queueMessage("mod-info", "Richese ambassador token was triggered, Ecaz may draw a treachery card for 3 spice.");
            case "Guild" -> discordGame.queueMessage("mod-info", "Guild ambassador token was triggered, Ecaz may place 4 forces to any territory from reserves for free.");
            case "BT" -> discordGame.queueMessage("mod-info", "BT ambassador token was triggered, Ecaz may revive a leader or up to 4 forces for free.");
        }

        for (Territory territory : game.getTerritories().values()) {
            if (territory.getEcazAmbassador() == null) continue;
            if (territory.getEcazAmbassador().equals(ambassador)) territory.setEcazAmbassador(null);
        }

        long nonEcazAmbassadorsCount = game.getTerritories().values().stream()
                .map(Territory::getEcazAmbassador)
                .filter(Objects::nonNull)
                .filter(a -> !a.equals("Ecaz"))
                .count();
        nonEcazAmbassadorsCount += ambassadorSupply.stream().filter(a -> !a.equals("Ecaz")).count();

        if (nonEcazAmbassadorsCount == 0) drawNewSupply();
        setBackOfShieldModified();
    }

    public void sendAmbassadorLocationMessage(Game game, DiscordGame discordGame, int cost) throws ChannelNotFoundException {
        List<Button> buttons = new LinkedList<>();
        for (Territory territory : game.getTerritories().values()) {
            if (!territory.isStronghold()) continue;
            if (territory.getTerritoryName().equals("Hidden Mobile Stronghold") && !game.hasFaction("Ix")) continue;
            Button stronghold = Button.primary("ecaz-place-ambassador-" + territory.getTerritoryName() + "-" + cost, "Place Ambassador in " + territory.getTerritoryName());
            if (territory.getEcazAmbassador() != null || game.getStorm() == territory.getSector()) stronghold = stronghold.asDisabled();
            buttons.add(stronghold);
        }
        buttons.add(Button.secondary("ecaz-no-more-ambassadors", "No more ambassadors."));
        discordGame.prepareMessage("ecaz-chat", "Use these buttons to place Ambassador tokens from your supply for " + cost + " " + Emojis.SPICE + "." + getPlayer())
                .addActionRow(buttons.subList(0, 5))
                .addActionRow(buttons.subList(5, buttons.size())).queue();
    }

    public void sendAmbassadorMessage(DiscordGame discordGame, String territory, int cost) throws ChannelNotFoundException {
        List<Button> buttons = new LinkedList<>();
        for (String ambassador : ambassadorSupply) {
            buttons.add(Button.primary("ecaz-ambassador-selected-" + ambassador + "-" + territory + "-" + cost, ambassador));
        }
        if (buttons.size() > 5) {
            discordGame.prepareMessage("ecaz-chat", "Which ambassador would you like to send?")
                    .addActionRow(buttons.subList(0, 5))
                    .addActionRow(buttons.get(5)).queue();
        }
        else {
            discordGame.prepareMessage("ecaz-chat", "Which ambassador would you like to send?")
                    .addActionRow(buttons).queue();
        }
    }

    public void placeAmbassador(Territory territory, String ambassador) {
        ambassadorSupply.removeIf(a -> a.equals(ambassador));
        territory.setEcazAmbassador(ambassador);
        setBackOfShieldModified();
    }

    public Leader getLoyalLeader() {
        return loyalLeader;
    }

    public void setLoyalLeader(Leader loyalLeader) {
        this.loyalLeader = loyalLeader;
    }

    public String getAmbassadorSupply() {
        StringBuilder supply = new StringBuilder();
        supply.append("\nAmbassador Supply:\n");

        for (String ambassador : ambassadorSupply) {
            supply.append(ambassador + "\n");
        }
        return supply.toString();
    }
}
