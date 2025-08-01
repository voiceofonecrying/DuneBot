package view.factions;

import constants.Emojis;
import controller.DiscordGame;
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
                embedBuilder.addField(
                        "Tech",
                        discordGame.tagEmojis(String.join("", techTokenEmojis)),
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

    protected List<MessageEmbed> getHomeworldEmbeds() {
        return Collections.singletonList(
                getHomeworldEmbed(faction.getHomeworld(), faction.isHighThreshold())
        );
    }

    protected MessageEmbed getHomeworldEmbed(String homeworldName, boolean isHighThreshold) {
        HomeworldCard homeworldCard = new HomeworldCard(homeworldName);

        EmbedBuilder homeworldBuilder = new EmbedBuilder()
                .setTitle(homeworldCard.name() + " Homeworld")
                .setColor(faction.getColor())
                .setThumbnail(CardImages.getHomeworldImageLink(discordGame.getEvent().getGuild(), homeworldCard.name()))
                .setUrl(CardImages.getHomeworldCardLink(discordGame.getEvent().getGuild(), homeworldCard.name()))
                ;

        String reservesModifier = "";
        if (homeworldCard.name().equals("Salusa Secundus"))
            reservesModifier = " Sardaukar";
        if (isHighThreshold) {
            homeworldBuilder.addField(
                    "High Threshold",
                    homeworldCard.highLowerThreshold() + " to " + homeworldCard.highUpperThreshold() + reservesModifier + " Reserves",
                    false
            );
            homeworldBuilder.addField("", homeworldCard.highDescription(), false);
            homeworldBuilder.addField("",
                    discordGame.tagEmojis(
                            MessageFormat.format(
                                    "{0} add {1} to dial in battle here.\n{0} only lose {1} to Lasgun/Shield explosion on {2}.",
                                    faction.getName(), homeworldCard.highBattleExplosion(), homeworldCard.name()
                            )),
                    false);
        } else {
            homeworldBuilder.addField(
                    "Low Threshold",
                    homeworldCard.lowLowerThreshold() + " to " + homeworldCard.lowUpperThreshold() + reservesModifier + " Reserves",
                    false
            );
            homeworldBuilder.addField("", homeworldCard.lowDescription(), false);

            if (homeworldCard.lowRevivalCharity() > 0) {
                homeworldBuilder.addField("",
                        discordGame.tagEmojis(
                                MessageFormat.format(
                                        "+{0} free revival",
                                        homeworldCard.lowRevivalCharity()
                                )),
                        false);

                homeworldBuilder.addField("",
                        discordGame.tagEmojis(
                                MessageFormat.format(
                                        "+{0} CHOAM Charity (from Spice Bank)",
                                        homeworldCard.lowRevivalCharity()
                                )),
                        false);

                homeworldBuilder.addField("",
                        discordGame.tagEmojis(
                                MessageFormat.format(
                                        "{0} add {1} to dial in battle here.\n{0} only lose {1} to Lasgun/Shield explosion on {2}.",
                                        faction.getName(), homeworldCard.lowBattleExplosion(), homeworldCard.name()
                                )),
                        false);

                homeworldBuilder.addField("When Occupied",
                        homeworldCard.occupiedDescription(),
                        false);

                homeworldBuilder.addField("Occupied Spice",
                        discordGame.tagEmojis(homeworldCard.occupiedSpice() + " " + Emojis.SPICE),
                        false);
            }
        }

        return homeworldBuilder.build();
    }

    private List<MessageEmbed> getLeaderSkillsEmbeds() {
        List<Leader> skilledLeaders = faction.getSkilledLeaders();

        return skilledLeaders.stream()
                .map(this::getLeaderSkillEmbed)
                .toList();
    }

    private MessageEmbed getLeaderSkillEmbed(Leader leader) {
        LeaderSkillCard leaderSkillCard = leader.getSkillCard();

        return new EmbedBuilder()
                .setTitle(
                        discordGame.tagEmojis(
                                MessageFormat.format(
                                        "{0} {1} is a {2} {0}",
                                        Emojis.LEADER, leader.getName(), leaderSkillCard.name()
                                )))
                .setColor(faction.getColor())
                .setThumbnail(CardImages.getLeaderImageLink(discordGame.getEvent().getGuild(), leader.getName()))
                .setUrl(CardImages.getLeaderSkillCardLink(discordGame.getEvent().getGuild(), leaderSkillCard.name()))
                .addField(
                        "When Leader is in Front of Shield",
                        leaderSkillCard.description(),
                        false
                )
                .addField(
                        "When Leader is in Battle",
                        leaderSkillCard.inBattleDescription(),
                        false
                )
                .build();
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
