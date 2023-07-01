package controller.commands;

import com.google.gson.internal.LinkedTreeMap;
import exceptions.ChannelNotFoundException;
import model.*;
import model.factions.Faction;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class RicheseCommands {
    public static List<CommandData> getCommands() {
        List<CommandData> commandData = new ArrayList<>();
        commandData.add(
                Commands.slash("richese", "Commands related to the Richese Faction.").addSubcommands(
                        new SubcommandData(
                                "no-fields-to-front-of-shield",
                                "Move the Richese No-Fields token to the Front of Shield."
                        ).addOptions(CommandOptions.richeseNoFields),
                        new SubcommandData(
                                "place-no-fields-token",
                                "Place a No-Fields token on the map."
                        ).addOptions(CommandOptions.richeseNoFields, CommandOptions.territory),
                        new SubcommandData(
                                "remove-no-field",
                                "Remove the No-Field token from the board"
                        ),
                        new SubcommandData("card-bid", "Start bidding on a Richese card")
                                .addOptions(CommandOptions.richeseCard, CommandOptions.richeseBidType),
                        new SubcommandData("black-market-bid", "Start bidding on a black market card")
                                .addOptions(CommandOptions.richeseBlackMarketCard, CommandOptions.richeseBlackMarketBidType)
                )
        );

        return commandData;
    }

    public static void runCommand(SlashCommandInteractionEvent event, DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        String name = event.getSubcommandName();

        switch (name) {
            case "no-fields-to-front-of-shield" -> moveNoFieldsToFrontOfShield(event, discordGame, game);
            case "card-bid" -> cardBid(event, discordGame, game);
            case "black-market-bid" -> blackMarketBid(event, discordGame, game);
            case "place-no-fields-token" -> placeNoFieldToken(event, discordGame, game);
            case "remove-no-field" -> removeNoFieldToken(event, discordGame, game);
        }
    }

    public static void moveNoFieldsToFrontOfShield(SlashCommandInteractionEvent event, DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        int noFieldValue = event.getOption(CommandOptions.richeseNoFields.getName()).getAsInt();

        if (game.hasFaction("Richese")) {
            Faction faction = game.getFaction("Richese");

            if (!faction.hasResource("frontOfShieldNoField")) {
                faction.addResource(new IntegerResource("frontOfShieldNoField", noFieldValue, 0, 5));
            } else {
                ((Resource<Integer>)faction.getResource("frontOfShieldNoField")).setValue(noFieldValue);
            }

            ShowCommands.refreshFrontOfShieldInfo(event, discordGame, game);
            discordGame.pushGame();
        }
    }

    public static void cardBid(SlashCommandInteractionEvent event, DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        String cardName = event.getOption(CommandOptions.richeseCard.getName()).getAsString();
        String bidType = event.getOption(CommandOptions.richeseBidType.getName()).getAsString();

        Faction faction = game.getFaction("Richese");

        List<LinkedTreeMap> rawList = (ArrayList<LinkedTreeMap>) faction.getResource("cache").getValue();

        for (int i = 0; i < rawList.size(); i++) {
            if (((String)rawList.get(i).get("name")).equalsIgnoreCase(cardName)) {
                game.setBidCard(new TreacheryCard(
                        (String)rawList.get(i).get("name"), (String)rawList.get(i).get("type")
                ));

                rawList.remove(i);
                break;
            }
        }

        game.incrementBidCardNumber();

        runRicheseBid(discordGame, game, bidType, false);

        discordGame.pushGame();
    }

    public static void blackMarketBid(SlashCommandInteractionEvent event, DiscordGame discordGame, Game game) throws ChannelNotFoundException {
        String cardName = event.getOption(CommandOptions.richeseBlackMarketCard.getName()).getAsString();
        String bidType = event.getOption(CommandOptions.richeseBlackMarketBidType.getName()).getAsString();

        Faction faction = game.getFaction("Richese");
        List<TreacheryCard> cards = faction.getTreacheryHand();

        TreacheryCard card = cards.stream()
                .filter(c -> c.name().equalsIgnoreCase(cardName))
                .findFirst()
                .orElseThrow();

        cards.remove(card);
        game.setBidCard(card);
        game.incrementBidCardNumber();

        if (bidType.equalsIgnoreCase("Normal")) {
            RunCommands.updateBidOrder(game);
            List<String> bidOrder = game.getBidOrder();

            RunCommands.createBidMessage(discordGame, game, bidOrder, game.getFaction(bidOrder.get(bidOrder.size() - 1)));
        } else {
            runRicheseBid(discordGame, game, bidType, true);
        }

        AtreidesCommands.sendAtreidesCardPrescience(discordGame, game, card);

        discordGame.pushGame();
    }

    public static void placeNoFieldToken(SlashCommandInteractionEvent event, DiscordGame discordGame, Game game) {
        Integer noField = event.getOption(CommandOptions.richeseNoFields.getName()).getAsInt();
        String territoryName = event.getOption(CommandOptions.territory.getName()).getAsString();

        Territory territory = game.getTerritories().get(territoryName);
        territory.setRicheseNoField(noField);

        discordGame.pushGame();
    }

    public static void removeNoFieldToken(SlashCommandInteractionEvent event, DiscordGame discordGame, Game game) {
        Optional<Territory> territory = game.getTerritories().values().stream()
                .filter(Territory::hasRicheseNoField)
                .findFirst();

        if (territory.isPresent()) {
            territory.get().setRicheseNoField(null);
        }

        discordGame.pushGame();
    }

    public static void runRicheseBid(DiscordGame discordGame, Game game, String bidType, boolean blackMarket) throws ChannelNotFoundException {
        if (bidType.equalsIgnoreCase("Silent")) {
            if (blackMarket) {
                discordGame.sendMessage("bidding-phase", "We will now silently auction a card from Richese's " +
                        "hand on the black market! Please place your bid in your private channels.");
            } else {
                discordGame.sendMessage("bidding-phase",
                        MessageFormat.format(
                                "We will now silently auction a brand new Richese {0}!  Please place your bid in your private channels.",
                                game.getBidCard().name()
                        )
                );
            }
        } else {
            StringBuilder message = new StringBuilder();
            if (blackMarket) {
                message.append("We are now bidding on a card from Richese's hand on the black market!\n");
            } else {
                message.append(
                        MessageFormat.format("We are now bidding on a shiny, brand new Richese {0}!\n",
                                game.getBidCard().name()
                        )
                );
            }

            List<Faction> factions = game.getFactions();

            List<Faction> bidOrder = new ArrayList<>();

            List<Faction> factionsInBidDirection;

            if (bidType.equalsIgnoreCase("OnceAroundCW")) {
                factionsInBidDirection = new ArrayList<>(factions);
                Collections.reverse(factionsInBidDirection);
            } else {
                factionsInBidDirection = factions;
            }

            int richeseIndex = factionsInBidDirection.indexOf(game.getFaction("Richese"));
            bidOrder.addAll(factionsInBidDirection.subList(richeseIndex + 1, factions.size()));
            bidOrder.addAll(factionsInBidDirection.subList(0, richeseIndex + 1));

            List<Faction> filteredBidOrder = bidOrder.stream()
                    .filter(f -> f.getHandLimit() > f.getTreacheryHand().size())
                    .toList();

            message.append(
                    MessageFormat.format(
                            "R{0}:C{1} (Once Around)\n{2} - {3}\n",
                            game.getTurn(), game.getBidCardNumber(),
                            filteredBidOrder.get(0).getEmoji(), filteredBidOrder.get(0).getPlayer()
                    )
            );

            message.append(
                    filteredBidOrder.subList(1, filteredBidOrder.size()).stream()
                            .map(f -> f.getEmoji() + " - \n")
                            .collect(Collectors.joining())
            );

            discordGame.sendMessage("bidding-phase", message.toString());
        }
    }
}
