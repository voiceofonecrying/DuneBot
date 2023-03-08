package controller.commands;

import com.google.gson.internal.LinkedTreeMap;
import exceptions.ChannelNotFoundException;
import exceptions.InvalidGameStateException;
import model.*;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;

public class RunCommands {
    public static List<CommandData> getCommands() {
        List<CommandData> commandData = new ArrayList<>();
        commandData.add(
                Commands.slash("run", "Commands related to playing through phases and turns.").addSubcommands(
                        new SubcommandData("advance", "Continue to the next phase of the game."),
                        new SubcommandData("bidding", "Run a regular bidding for a card"),
                        new SubcommandData("richese-card-bidding", "Start bidding on a Richese Card")
                                .addOptions(CommandOptions.richeseCard, CommandOptions.richeseBidType)
                )
        );

        return commandData;
    }

    public static void runCommand(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) throws ChannelNotFoundException, InvalidGameStateException {
        String name = event.getSubcommandName();

        switch (name) {
            case "advance" -> advance(event, discordGame, gameState);
            case "bidding" -> bidding(event, discordGame, gameState);
            case "richese-card-bidding" -> richeseCardBidding(event, discordGame, gameState);
        }
    }

    public static void advance(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) throws ChannelNotFoundException {
        int phase = gameState.getPhase();
        int subPhase = gameState.getSubPhase();

        if (phase == 1 && subPhase == 1) {
            startStormPhase(event, discordGame, gameState);
            gameState.advanceSubPhase();
        } else if (phase == 1 && subPhase == 2) {
            endStormPhase(event, discordGame, gameState);
            gameState.advancePhase();
        } else if (phase == 2) {
            spiceBlow(event, discordGame, gameState);
            gameState.advancePhase();
        } else if (phase == 3) {
            choamCharity(event, discordGame, gameState);
            gameState.advancePhase();
        } else if (phase == 4 && subPhase == 1) {
            startBiddingPhase(event, discordGame, gameState);
            gameState.advanceSubPhase();
        } else if (phase == 4 && subPhase == 2) {
            cardCountsInBiddingPhase(event, discordGame, gameState);
            gameState.advancePhase();
        } else if (phase == 5) {
            startRevivalPhase(event, discordGame, gameState);
            gameState.advancePhase();
        } else if (phase == 6) {
            startShipmentPhase(event, discordGame, gameState);
            gameState.advancePhase();
        } else if (phase == 7) {
            startBattlePhase(event, discordGame, gameState);
            gameState.advancePhase();
        } else if (phase == 8) {
            startSpiceHarvest(event, discordGame, gameState);
            gameState.advancePhase();
        } else if (phase == 9) {
            startMentatPause(event, discordGame, gameState);
            gameState.advanceTurn();
        }


        discordGame.pushGameState();
    }

    public static void startStormPhase(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) throws ChannelNotFoundException {
        discordGame.sendMessage("turn-summary", "Turn " + gameState.getTurn() + " Storm Phase:");
        Map<String, Territory> territories = gameState.getTerritories();
        if (gameState.getTurn() != 1) {
            discordGame.sendMessage("turn-summary", "The storm moves " + gameState.getStormMovement() + " sectors this turn.");
            for (int i = 0; i < gameState.getStormMovement(); i++) {
                gameState.advanceStorm(1);
                for (Territory territory : territories.values()) {
                    if (territory.isRock() || territory.getSector() != gameState.getStorm()) continue;
                    List<Force> forces = territory.getForces();
                    boolean fremenSpecialCase = forces.stream()
                            .filter(force -> force.getName().startsWith("Fremen"))
                            .count() == 2;
                    //Defaults to play "optimally", destroying Fremen regular forces over Fedaykin
                    if (fremenSpecialCase) {
                        int fremenForces = 0;
                        int fremenFedaykin = 0;
                        for (Force force : forces) {
                            if (force.getName().equals("Fremen")) fremenForces = force.getStrength();
                            if (force.getName().equals("Fremen*")) fremenFedaykin = force.getStrength();
                        }
                        int lost = Math.ceilDiv(fremenForces + fremenFedaykin, 2);
                        if (lost < fremenForces) {
                            for (Force force : forces) {
                                if (force.getName().equals("Fremen")) force.setStrength(force.getStrength() - lost);
                            }
                        } else if (lost > fremenForces) {
                            forces.removeIf(force -> force.getName().equals("Fremen"));
                            for (Force force : forces) {
                                if (force.getName().equals("Fremen*"))
                                    force.setStrength(fremenFedaykin - lost + fremenForces);
                            }
                        }
                        discordGame.sendMessage("turn-summary", gameState.getFaction("Fremen").getEmoji() + " lost " + lost +
                                " forces to the storm in " + territory.getTerritoryName());
                    }
                    List<Force> toRemove = new LinkedList<>();
                    for (Force force : forces) {
                        if (force.getName().contains("Fremen") && fremenSpecialCase) continue;
                        int lost = force.getStrength();
                        if (force.getName().contains("Fremen") && lost > 1) {
                            lost /= 2;
                            force.setStrength(lost);
                            forces.add(force);
                        } else toRemove.add(force);
                        gameState.getTanks().stream().filter(force1 -> force1.getName().equals(force.getName())).findFirst().orElseThrow().addStrength(force.getStrength());
                        discordGame.sendMessage("turn-summary",
                                gameState.getFaction(force.getName().replace("*", "")).getEmoji() + " lost " +
                                        lost + " forces to the storm in " + territory.getTerritoryName());
                    }
                    forces.removeAll(toRemove);
                    territory.setSpice(0);
                }
            }

            ShowCommands.showBoard(discordGame, gameState);
        }

        gameState.setStormMovement(new Random().nextInt(6) + 1);
    }
    public static void endStormPhase(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) throws ChannelNotFoundException {
        if (gameState.hasFaction("Fremen")) {
            discordGame.sendMessage("fremen-chat", "The storm will move " + gameState.getStormMovement() + " sectors next turn.");
        }
    }

    public static void spiceBlow(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) throws ChannelNotFoundException {
        discordGame.sendMessage("turn-summary", "Turn " + gameState.getTurn() + " Spice Blow Phase:");
    }

    public static void choamCharity(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) throws ChannelNotFoundException {
        discordGame.sendMessage("turn-summary", "Turn " + gameState.getTurn() + " CHOAM Charity Phase:");
        int multiplier = 1;
        if (gameState.getResources().stream().anyMatch(resource -> resource.getName().equals("inflation token"))) {
            if (gameState.getResources().stream().filter(resource -> resource.getName().equals("inflation token")).findFirst().orElseThrow().getValue().equals("cancel")) {
                discordGame.sendMessage("turn-summary","CHOAM Charity is cancelled!");
                return;
            } else {
                multiplier = 2;
            }
        }

        int choamGiven = 0;
        List<Faction> factions = gameState.getFactions();
        if (gameState.hasFaction("CHOAM")) {
            discordGame.sendMessage("turn-summary",
                    gameState.getFaction("CHOAM").getEmoji() + " receives " +
                            gameState.getFactions().size() * 2 * multiplier + " <:spice4:991763531798167573> in dividends from their many investments."
            );
        }
        for (Faction faction : factions) {
            if (faction.getName().equals("CHOAM")) continue;
            int spice = faction.getSpice();
            if (faction.getName().equals("BG")) {
                choamGiven += 2 * multiplier;
                faction.addSpice(2 * multiplier);
                discordGame.sendMessage("turn-summary", faction.getEmoji() + " have received " + 2 * multiplier + " <:spice4:991763531798167573> in CHOAM Charity.");
                CommandManager.spiceMessage(discordGame, 2 * multiplier, faction.getName(), "CHOAM Charity", true);
            }
            else if (spice < 2) {
                int charity = (2 * multiplier) - (spice * multiplier);
                choamGiven += charity;
                faction.addSpice(charity);
                discordGame.sendMessage("turn-summary",
                        faction.getEmoji() + " have received " + charity + " <:spice4:991763531798167573> in CHOAM Charity."
                );
                CommandManager.spiceMessage(discordGame, charity, faction.getName(), "CHOAM Charity", true);
            }
            else continue;
            ShowCommands.writeFactionInfo(discordGame, faction);
        }
        if (gameState.hasFaction("CHOAM")) {
            gameState.getFaction("CHOAM").addSpice((2 * factions.size() * multiplier) - choamGiven);
            CommandManager.spiceMessage(discordGame, gameState.getFactions().size() * 2 * multiplier, "choam", "CHOAM Charity", true);
            discordGame.sendMessage("turn-summary",
                    gameState.getFaction("CHOAM").getEmoji() + " has paid " + choamGiven + " <:spice4:991763531798167573> to factions in need."
            );
            CommandManager.spiceMessage(discordGame, choamGiven, "choam", "CHOAM Charity given", false);
        }
    }

    public static void startBiddingPhase(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) throws ChannelNotFoundException {
        discordGame.sendMessage("turn-summary", "Turn " + gameState.getTurn() + " Bidding Phase:");
        gameState.setBidOrder(new ArrayList<>());
        gameState.setBidCardNumber(0);
        gameState.setBidCard(null);
        discordGame.sendMessage("mod-info", "Run black market bid (if exists), then advance the game.");
    }

    public static void cardCountsInBiddingPhase(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) throws ChannelNotFoundException {
        StringBuilder message = new StringBuilder();
        message.append("<:treachery:991763073281040518>Number of Treachery Cards<:treachery:991763073281040518>\n");
        List<Faction> factions = gameState.getFactions();

        message.append(
                factions.stream().map(
                        f -> MessageFormat.format(
                                "{0}: {1}\n", f.getEmoji(), f.getTreacheryHand().size()
                        )
                ).collect(Collectors.joining())
        );

        int numCardsForBid = factions.stream()
                .filter(f -> f.getHandLimit() > f.getTreacheryHand().size())
                .collect(Collectors.toList()).size();

        message.append(
                MessageFormat.format(
                        "There will be {0}<:treachery:991763073281040518> up for bid this round.",
                        numCardsForBid
                )
        );

        discordGame.sendMessage("turn-summary", message.toString());
        discordGame.sendMessage("mod-info", "Start running commands to bid and then advance when all the bidding is done.");
    }

    public static void bidding(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) throws ChannelNotFoundException {
        updateBidOrder(gameState);
        List<String> bidOrder = gameState.getBidOrder();

        gameState.incrementBidCardNumber();

        List<TreacheryCard> treacheryDeck = gameState.getTreacheryDeck();

        if (treacheryDeck.isEmpty()) {
            List<TreacheryCard> treacheryDiscard = gameState.getTreacheryDiscard();
            discordGame.sendMessage("turn-summary", "The Treachery Deck has been replenished from the Discard Pile");
            treacheryDeck.addAll(treacheryDiscard);
            treacheryDiscard.clear();
        }

        TreacheryCard bidCard = treacheryDeck.remove(0);

        gameState.setBidCard(bidCard);

        if (gameState.hasFaction("Atreides")) {
            discordGame.sendMessage("atreides-chat",
                    MessageFormat.format(
                            "You predict <:treachery:991763073281040518> {0} <:treachery:991763073281040518> is up for bid.",
                            bidCard.name().strip()
                    )
            );
        }

        StringBuilder message = new StringBuilder();

        Faction firstBidFaction = gameState.getFaction(bidOrder.get(0));

        message.append(
                MessageFormat.format(
                        "R{0}:C{1}\n{2} - {3}\n",
                        gameState.getTurn(), gameState.getBidCardNumber(),
                        firstBidFaction.getEmoji(), firstBidFaction.getPlayer()
                )
        );

        message.append(
                bidOrder.subList(1, bidOrder.size()).stream()
                        .map(f -> gameState.getFaction(f).getEmoji() + " - \n")
                        .collect(Collectors.joining())
        );

        discordGame.pushGameState();
        discordGame.sendMessage("bidding-phase", message.toString());
    }

    public static void richeseCardBidding(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) throws ChannelNotFoundException {
        String cardName = event.getOption(CommandOptions.richeseCard.getName()).getAsString();
        String bidType = event.getOption(CommandOptions.richeseBidType.getName()).getAsString();

        Faction faction = gameState.getFaction("Richese");

        List<LinkedTreeMap> rawList = (ArrayList<LinkedTreeMap>) faction.getResource("cache").getValue();

        for (int i = 0; i < rawList.size(); i++) {
            if (((String)rawList.get(i).get("name")).equalsIgnoreCase(cardName)) {
                gameState.setBidCard(new TreacheryCard(
                        (String)rawList.get(i).get("name"), (String)rawList.get(i).get("type")
                ));

                rawList.remove(i);
                break;
            }
        }

        gameState.incrementBidCardNumber();

        if (bidType.equalsIgnoreCase("Silent")) {
            discordGame.sendMessage("bidding-phase",
                    MessageFormat.format(
                            "We will now silently auction a brand new Richese {0}!  Please place your bid in your private channels.",
                            gameState.getBidCard().name()
                    )
            );
        } else {
            StringBuilder message = new StringBuilder();
            message.append(
                    MessageFormat.format("We are now bidding on a shiny, brand new Richese {0}!\n",
                            gameState.getBidCard().name()
                    )
            );

            List<Faction> factions = gameState.getFactions();

            List<Faction> bidOrder = new ArrayList<>();

            List<Faction> factionsInBidDirection;

            if (bidType.equalsIgnoreCase("OnceAroundCW")) {
                factionsInBidDirection = new ArrayList<>(factions);
                Collections.reverse(factionsInBidDirection);
            } else {
                factionsInBidDirection = factions;
            }

            int richeseIndex = factionsInBidDirection.indexOf(gameState.getFaction("Richese"));
            bidOrder.addAll(factionsInBidDirection.subList(richeseIndex + 1, factions.size()));
            bidOrder.addAll(factionsInBidDirection.subList(0, richeseIndex + 1));

            List<Faction> filteredBidOrder = bidOrder.stream()
                    .filter(f -> f.getHandLimit() > f.getTreacheryHand().size())
                    .toList();

            message.append(
                    MessageFormat.format(
                            "R{0}:C{1} (Once Around)\n{2} - {3}\n",
                            gameState.getTurn(), gameState.getBidCardNumber(),
                            bidOrder.get(0).getEmoji(), bidOrder.get(0).getPlayer()
                    )
            );

            message.append(
                    bidOrder.subList(1, bidOrder.size()).stream()
                            .map(f -> f.getEmoji() + " - \n")
                            .collect(Collectors.joining())
            );

            discordGame.sendMessage("bidding-phase", message.toString());
        }

        discordGame.pushGameState();
    }

    public static void updateBidOrder(Game gameState) {
        List<String> bidOrder;

        if (gameState.getBidOrder().isEmpty()) {
            List<Faction> factions = gameState.getFactions();

            int firstBid = Math.ceilDiv(gameState.getStorm(), 3) % factions.size();

            List<Faction> bidOrderFactions = new ArrayList<>();

            bidOrderFactions.addAll(factions.subList(firstBid, factions.size()));
            bidOrderFactions.addAll(factions.subList(0, firstBid));
            bidOrder = bidOrderFactions.stream().map(Faction::getName).collect(Collectors.toList());
        } else {
            bidOrder = gameState.getBidOrder();
            bidOrder.add(bidOrder.remove(0));
        }

        gameState.setBidOrder(
                bidOrder.stream()
                        .filter(f -> gameState.getFaction(f).getHandLimit() > gameState.getFaction(f).getTreacheryHand().size())
                        .collect(Collectors.toList())
        );
    }

    public static void startRevivalPhase(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) throws ChannelNotFoundException {
        discordGame.sendMessage("turn-summary", "Turn " + gameState.getTurn() + " Revival Phase:");
        List<Faction> factions = gameState.getFactions();
        StringBuilder message = new StringBuilder();
        message.append("Free Revivals:\n");
        for (Faction faction : factions) {
            int revived = 0;
            boolean revivedStar = false;
            for (int i = faction.getFreeRevival(); i > 0; i--) {
                if (gameState.getForceFromTanks(faction.getName()).getStrength() == 0
                        && gameState.getForceFromTanks(faction.getName() + "*").getStrength() == 0) continue;
                revived++;
                if (gameState.getForceFromTanks(faction.getName() + "*").getStrength() > 0 && !revivedStar) {
                    Force force = gameState.getForceFromTanks(faction.getName() + "*");
                    force.setStrength(force.getStrength() - 1);
                    revivedStar = true;
                    faction.getSpecialReserves().setStrength(faction.getSpecialReserves().getStrength() + 1);
                } else if (gameState.getForceFromTanks(faction.getName()).getStrength() > 0) {
                    Force force = gameState.getForceFromTanks(faction.getName());
                    force.setStrength(force.getStrength() - 1);
                    faction.getReserves().setStrength(faction.getReserves().getStrength() + 1);
                }
            }
            if (revived > 0) {
                message.append(gameState.getFaction(faction.getName()).getEmoji()).append(": ").append(revived).append("\n");
            }
        }
        discordGame.sendMessage("turn-summary", message.toString());
        ShowCommands.showBoard(discordGame, gameState);
    }

    public static void startShipmentPhase(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) throws ChannelNotFoundException {
        discordGame.sendMessage("turn-summary","Turn " + gameState.getTurn() + " Shipment and Movement Phase:");
        if (gameState.hasFaction("Atreides")) {
            discordGame.sendMessage("atreides-info", "You see visions of " + gameState.getSpiceDeck().peek().name() + " in your future.");
        }
        if(gameState.hasFaction("BG")) {
            for (Territory territory : gameState.getTerritories().values()) {
                if (territory.getForce("Advisor").getStrength() > 0) discordGame.sendMessage("turn-summary",gameState
                        .getFaction("BG").getEmoji() + " to decide whether to flip their advisors in " + territory.getTerritoryName());
            }
        }
        ShowCommands.showBoard(discordGame, gameState);
    }

    public static void startBattlePhase(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) throws ChannelNotFoundException {
        discordGame.sendMessage("turn-summary","Turn " + gameState.getTurn() + " Battle Phase:");

        // Get list of territories with multiple factions
        List<Pair<Territory, List<Faction>>> battles = new ArrayList<>();
        for (Territory territory : gameState.getTerritories().values()) {
            List<Force> forces = territory.getForces();
            List<Faction> factions = forces.stream()
                    .filter(force -> !(force.getName().equalsIgnoreCase("Advisor")))
                    .map(Force::getFactionName)
                    .distinct()
                    .sorted(Comparator.comparingInt(gameState::getFactionTurnIndex))
                    .map(gameState::getFaction)
                    .toList();
            ;

            if (factions.size() > 1) {
                battles.add(new ImmutablePair<>(territory, factions));
            }
        }

        if(battles.size() > 0) {
            String battleMessages = battles.stream()
                    .sorted(Comparator
                            .comparingInt(o -> gameState.getFactionTurnIndex(o.getRight().get(0).getName()))
                    ).map((battle) ->
                            MessageFormat.format("{0} in {1}",
                                    battle.getRight().stream()
                                            .map(Faction::getEmoji)
                                            .collect(Collectors.joining(" vs ")),
                                    battle.getLeft().getTerritoryName()
                            )
                    ).collect(Collectors.joining("\n"));

            discordGame.sendMessage("turn-summary",
                    "The following battles will take place this turn:\n" + battleMessages
            );
        } else {
            discordGame.sendMessage("turn-summary", "There are no battles this turn.");
        }
        ShowCommands.showBoard(discordGame, gameState);
    }

    public static void startSpiceHarvest(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) throws ChannelNotFoundException {
        discordGame.sendMessage("turn-summary", "Turn " + gameState.getTurn() + " Spice Harvest Phase:");
        Map<String, Territory> territories = gameState.getTerritories();
        for (Territory territory : territories.values()) {
            if (territory.getForces().size() != 1) continue;
            if (territory.getForces().get(0).getName().equals("Advisor")) {
                int strength = territory.getForces().get(0).getStrength();
                territory.getForces().clear();
                territory.getForces().add(new Force("BG", strength));
                discordGame.sendMessage("turn-summary", "Advisors are alone in " + territory.getTerritoryName() + " and have flipped to fighters.");
            }
        }

        Set<Faction> factionsWithChanges = new HashSet<>();

        for (Faction faction : gameState.getFactions()) {
            faction.setHasMiningEquipment(false);
            if (territories.get("Arrakeen").getForces().stream().anyMatch(force -> force.getName().contains(faction.getName()))) {
                faction.addSpice(2);
                CommandManager.spiceMessage(discordGame, 2, faction.getName(), "for Arrakeen", true);
                discordGame.sendMessage("turn-summary", gameState.getFaction(faction.getName()).getEmoji() + " collects 2 <:spice4:991763531798167573> from Arrakeen");
                faction.setHasMiningEquipment(true);
                factionsWithChanges.add(faction);
            }
            if (territories.get("Carthag").getForces().stream().anyMatch(force -> force.getName().contains(faction.getName()))) {
                faction.addSpice(2);
                CommandManager.spiceMessage(discordGame, 2, faction.getName(), "for Carthag", true);
                discordGame.sendMessage("turn-summary", gameState.getFaction(faction.getName()).getEmoji() + " collects 2 <:spice4:991763531798167573> from Carthag");
                faction.setHasMiningEquipment(true);
                factionsWithChanges.add(faction);
            }
            if (territories.get("Tuek's Sietch").getForces().stream().anyMatch(force -> force.getName().contains(faction.getName()))) {
                discordGame.sendMessage("turn-summary", gameState.getFaction(faction.getName()).getEmoji() + " collects 1 <:spice4:991763531798167573> from Tuek's Sietch");
                faction.addSpice(1);
                CommandManager.spiceMessage(discordGame, 1, faction.getName(), "for Tuek's Sietch", true);
                factionsWithChanges.add(faction);
            }
        }

        for (Territory territory: territories.values()) {
            if (territory.getSpice() == 0 || territory.getForces().size() == 0) continue;
            int totalStrength = 0;
            Faction faction = gameState.getFaction(territory.getForces().stream().filter(force -> !force.getName().equals("Advisor")).findFirst().orElseThrow().getName().replace("*", ""));
            for (Force force : territory.getForces()) {
                if (force.getName().equals("Advisor")) continue;
                totalStrength += force.getStrength();
            }
            int multiplier = faction.hasMiningEquipment() ? 3 : 2;
            int spice = Math.min(multiplier * totalStrength, territory.getSpice());
            faction.addSpice(spice);
            CommandManager.spiceMessage(discordGame, spice, faction.getName(), "for Spice Blow", true);
            factionsWithChanges.add(faction);
            territory.setSpice(territory.getSpice() - spice);
            discordGame.sendMessage("turn-summary", gameState.getFaction(faction.getName()).getEmoji() + " collects " + spice + " <:spice4:991763531798167573> from " + territory.getTerritoryName());
        }

        for (Faction faction : factionsWithChanges) {
            ShowCommands.writeFactionInfo(discordGame, faction);
        }
        ShowCommands.showBoard(discordGame, gameState);
    }

    public static void startMentatPause(SlashCommandInteractionEvent event, DiscordGame discordGame, Game gameState) throws ChannelNotFoundException {
        discordGame.sendMessage("turn-summary", "Turn " + gameState.getTurn() + " Mentat Pause Phase:");
        for (Faction faction : gameState.getFactions()) {
            if (faction.getFrontOfShieldSpice() > 0) {
                discordGame.sendMessage("turn-summary", faction.getEmoji() + " collects " +
                        faction.getFrontOfShieldSpice() + " <:spice4:991763531798167573> from front of shield.");
                CommandManager.spiceMessage(discordGame,  faction.getFrontOfShieldSpice(), faction.getName(), "front of shield", true);
                faction.addSpice(faction.getFrontOfShieldSpice());
                faction.setFrontOfShieldSpice(0);
                ShowCommands.writeFactionInfo(discordGame, faction);
            }
        }
    }
}
