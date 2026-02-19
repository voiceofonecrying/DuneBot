package view.factions;

import constants.Emojis;
import controller.DiscordGame;
import controller.commands.ShowCommands;
import enums.GameOption;
import exceptions.ChannelNotFoundException;
import model.*;
import model.factions.*;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import utils.CardImages;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FactionView {
    protected final DiscordGame discordGame;
    protected final Game game;
    protected final model.factions.Faction faction;

    public static FactionView factory(DiscordGame discordGame, Faction faction) throws ChannelNotFoundException {
        return switch (faction) {
            case BTFaction btFaction -> new BTView(discordGame, btFaction);
            case ChoamFaction choamFaction -> new ChoamView(discordGame, choamFaction);
            case EcazFaction ecazFaction -> new EcazView(discordGame, ecazFaction);
            case EmperorFaction emperorFaction -> new EmperorView(discordGame, emperorFaction);
            case MoritaniFaction moritaniFaction -> new MoritaniView(discordGame, moritaniFaction);
            case RicheseFaction richeseFaction -> new RicheseView(discordGame, richeseFaction);
            case null, default -> new FactionView(discordGame, faction);
        };
    }

    public FactionView(DiscordGame discordGame, Faction faction) throws ChannelNotFoundException {
        this.discordGame = discordGame;
        this.game = discordGame.getGame();
        this.faction = faction;
    }

    public MessageCreateBuilder getPublicMessage() {
        MessageCreateBuilder builder = new MessageCreateBuilder();

        builder.addContent(
                discordGame.tagEmojis(
                        MessageFormat.format(
                                "{0} {1} Info {0}\n",
                                faction.getEmoji(), faction.getName()
                        ))
        );

        builder.addEmbeds(getSummaryEmbed());

        additionalEmbedsTop().forEach(builder::addEmbeds);

        if (game.hasGameOption(GameOption.HOMEWORLDS)) {
            getHomeworldEmbeds().forEach(builder::addEmbeds);
        }

        if (game.hasGameOption(GameOption.LEADER_SKILLS)) {
            getLeaderSkillsEmbeds().forEach(builder::addEmbeds);
        }

        if (game.hasGameOption(GameOption.STRONGHOLD_SKILLS)) {
            getStrongholdCardEmbeds().forEach(builder::addEmbeds);
        }

        additionalEmbedsBottom().forEach(builder::addEmbeds);

        return  builder;
    }

    public static String getTaggedReservesString(DiscordGame discordGame, Faction faction) {
        return discordGame.tagEmojis(faction.forcesStringWithZeroes(faction.getReservesStrength(), faction.getSpecialReservesStrength()));
    }

    private MessageEmbed getSummaryEmbed() {
        EmbedBuilder embedBuilder = new EmbedBuilder()
                .setColor(faction.getColor());

        if (game.hasGameOption(GameOption.SPICE_PUBLIC)) {
            embedBuilder.addField(
                    "Back",
                    discordGame.tagEmojis(faction.getSpice() + " " + Emojis.SPICE),
                    true
            );
        }

        embedBuilder.addField(
                "Front",
                discordGame.tagEmojis(faction.getFrontOfShieldSpice() + " " + Emojis.SPICE),
                true
        );

        if (game.hasGameOption(GameOption.TREACHERY_CARD_COUNT_PUBLIC)) {
            embedBuilder.addField(
                    "Treachery",
                    discordGame.tagEmojis(faction.getTreacheryHand().size() + " " + Emojis.TREACHERY),
                    true
            );
        }

        if (!game.hasGameOption(GameOption.HOMEWORLDS)) {
            embedBuilder.addField("Reserves", getTaggedReservesString(discordGame, faction), true);
        }

        if (game.hasGameOption(GameOption.TECH_TOKENS)) {
            List<String> techTokenEmojis = faction.getTechTokens().stream()
                    .map(TechToken::getName)
                    .map(Emojis::getTechTokenEmoji)
                    .toList();

            if (!techTokenEmojis.isEmpty()) {
                String techTokenString = String.join("", techTokenEmojis);
                int techSpice = faction.getTechTokens().stream().map(TechToken::getSpice).reduce(0, Math::max);
                if (techSpice > 0)
                    techTokenString += " + " + techSpice + " " + Emojis.SPICE;
                embedBuilder.addField(
                        "Tech",
                        discordGame.tagEmojis(techTokenString),
                        true
                );
            }
        }

        additionalSummaryFields().forEach(embedBuilder::addField);

        return embedBuilder.build();
    }

    protected List<MessageEmbed.Field> additionalSummaryFields() {
        return new ArrayList<>();
    }

    protected List<MessageEmbed> additionalEmbedsTop() {
        return new ArrayList<>();
    }

    protected List<MessageEmbed> additionalEmbedsBottom() {
        return new ArrayList<>();
    }

    /**
     * Returns fields to be added to the shared front-of-shield embed (the top embed with Deck Sizes, etc.)
     * Override this in faction-specific views to add game-wide information.
     */
    public List<MessageEmbed.Field> sharedFrontOfShieldFields() {
        return new ArrayList<>();
    }

    protected List<MessageEmbed> getHomeworldEmbeds() {
        String homeworldName = faction.getHomeworld();
        if (faction instanceof HomebrewFaction hbFaction)
            homeworldName = hbFaction.getHomeworldProxy();
        return Collections.singletonList(
                getHomeworldEmbed(homeworldName, faction.isHighThreshold())
        );
    }

    protected MessageEmbed getHomeworldEmbed(String homeworldName, boolean isHighThreshold) {
        HomeworldCard homeworldCard = new HomeworldCard(homeworldName);

        int highLowerThreshold = homeworldCard.highLowerThreshold();
        int highUpperThreshold = homeworldCard.highUpperThreshold();
        String highDescription = homeworldCard.highDescription();
        int highBattleExplosion = homeworldCard.highBattleExplosion();
        int lowLowerThreshold = homeworldCard.lowLowerThreshold();
        int lowUpperThreshold = homeworldCard.lowUpperThreshold();
        String lowDescription = homeworldCard.lowDescription();
        int lowRevivalCharity = homeworldCard.lowRevivalCharity();
        int lowBattleExplosion = homeworldCard.lowBattleExplosion();
        String occupiedDescription = homeworldCard.occupiedDescription();
        int occupiedSpice = homeworldCard.occupiedSpice();
        if (faction instanceof HomebrewFaction hbf) {
            homeworldName = hbf.getHomeworld();
            highLowerThreshold = hbf.getHighThreshold();
            highDescription = hbf.getHighDescription();
            highBattleExplosion = hbf.getHighBattleExplosion();
            lowUpperThreshold = hbf.getLowThreshold();
            lowDescription = hbf.getLowDescription();
            lowRevivalCharity = hbf.getLowRevivalCharity();
            lowBattleExplosion = hbf.getLowBattleExplosion();
            occupiedDescription = hbf.getOccupiedDescription();
            occupiedSpice = hbf.getOccupiedIncome();
        }
        String thumbnail = CardImages.getHomeworldImageLink(discordGame.getEvent().getGuild(), homeworldCard.name());
        if (faction instanceof HomebrewFaction) {
            try {
                String imageUrl = ShowCommands.getHomebrewFactionImageUrlFromHomebrewChannel(discordGame, faction.getName().toLowerCase(), "homeworld", faction.getHomeworld());
                if (imageUrl != null)
                    thumbnail = imageUrl;
            } catch (Exception ignored) {}
        }
        EmbedBuilder homeworldBuilder = new EmbedBuilder()
                .setTitle(homeworldName + " Homeworld")
                .setColor(faction.getColor())
                .setThumbnail(thumbnail)
                .setUrl(CardImages.getHomeworldCardLink(discordGame.getEvent().getGuild(), homeworldCard.name()))
                ;

        String reservesModifier = "";
        if (homeworldCard.name().equals("Salusa Secundus"))
            reservesModifier = " Sardaukar";
        if (isHighThreshold) {
            homeworldBuilder.addField(
                    "High Threshold",
                    highLowerThreshold + " to " + highUpperThreshold + reservesModifier + " Reserves",
                    false
            );
            homeworldBuilder.addField("", highDescription, false);
            homeworldBuilder.addField("",
                    discordGame.tagEmojis(
                            MessageFormat.format(
                                    "{0} add {1} to dial in battle here.\n{0} only lose {1} to Lasgun/Shield explosion on {2}.",
                                    faction.getName(), highBattleExplosion, homeworldName
                            )),
                    false);
        } else {
            homeworldBuilder.addField(
                    "Low Threshold",
                    lowLowerThreshold + " to " + lowUpperThreshold + reservesModifier + " Reserves",
                    false
            );
            homeworldBuilder.addField("", lowDescription, false);

            if (lowRevivalCharity > 0) {
                homeworldBuilder.addField("",
                        discordGame.tagEmojis(
                                MessageFormat.format(
                                        "+{0} free revival",
                                        lowRevivalCharity
                                )),
                        false);

                homeworldBuilder.addField("",
                        discordGame.tagEmojis(
                                MessageFormat.format(
                                        "+{0} CHOAM Charity (from Spice Bank)",
                                        lowRevivalCharity
                                )),
                        false);

                homeworldBuilder.addField("",
                        discordGame.tagEmojis(
                                MessageFormat.format(
                                        "{0} add {1} to dial in battle here.\n{0} only lose {1} to Lasgun/Shield explosion on {2}.",
                                        faction.getName(), lowBattleExplosion, homeworldName
                                )),
                        false);

                homeworldBuilder.addField("When Occupied",
                        occupiedDescription,
                        false);

                homeworldBuilder.addField("Occupied Spice",
                        discordGame.tagEmojis(occupiedSpice + " " + Emojis.SPICE),
                        false);
            }
        }

        return homeworldBuilder.build();
    }

    private List<MessageEmbed> getLeaderSkillsEmbeds() {
        List<Leader> skilledLeaders = faction.getSkilledLeaders();

        return skilledLeaders.stream()
                .filter(l -> l.getOriginalFactionName().equals(faction.getName()))
                .map(l -> getLeaderSkillEmbed(game, l))
                .toList();
    }

    public MessageEmbed getLeaderSkillEmbed(Game game, Leader leader) {
        LeaderSkillCard leaderSkillCard = leader.getSkillCard();
        String description = leaderSkillCard.description();
        if (description.isEmpty())
            description = game.getHomebrewLeaderSkillDescription(leaderSkillCard.name());
        String inBattleDescription = leaderSkillCard.inBattleDescription();
        if (inBattleDescription.isEmpty())
            inBattleDescription = game.getHomebrewLeaderSkillInBattleDescription(leaderSkillCard.name());

        EmbedBuilder eb = new EmbedBuilder()
                .setTitle(
                        discordGame.tagEmojis(
                                MessageFormat.format(
                                        "{0} {1} is a {2} {0}",
                                        Emojis.LEADER, leader.getName(), leaderSkillCard.name()
                                )))
                .setColor(faction.getColor())
                .setUrl(CardImages.getLeaderSkillCardLink(discordGame.getEvent().getGuild(), leaderSkillCard.name()))
                .addField(
                        "When Leader is in Front of Shield",
                        description,
                        false
                )
                .addField(
                        "When Leader is in Battle",
                        inBattleDescription,
                        false
                );
        List<String> allFactionNames = List.of("Atreides", "BG", "Harkonnen", "Emperor", "Fremen", "Guild",
                "BT", "Ix", "CHOAM", "Richese", "Ecaz", "Moritani");
        if (!(faction instanceof HomebrewFaction) && allFactionNames.contains(leader.getOriginalFactionName()))
            eb = eb.setThumbnail(CardImages.getLeaderImageLink(discordGame.getEvent().getGuild(), leader.getName()));
        else
            eb = eb.setThumbnail(ShowCommands.getHomebrewFactionImageUrlFromHomebrewChannel(discordGame, leader.getOriginalFactionName().toLowerCase(), "leaders", leader.getName()));
        return eb.build();
    }

    private List<MessageEmbed> getStrongholdCardEmbeds() {
        List<StrongholdCard> strongholdCards = faction.getStrongholdCards();

        return strongholdCards.stream()
                .map(this::getStrongholdCardEmbed)
                .toList();
    }

    private MessageEmbed getStrongholdCardEmbed(StrongholdCard strongholdCard) {
        return new EmbedBuilder()
                .setTitle(
                        discordGame.tagEmojis(
                                MessageFormat.format(
                                        "{0} {1} {0}",
                                        Emojis.STRONGHOLD, strongholdCard.name()
                                ))
                )
                .setDescription(strongholdCard.description())
                .setColor(faction.getColor())
                .setUrl(CardImages.getStrongholdCardLink(discordGame.getEvent().getGuild(), strongholdCard.name()))
                .build();
    }




}
