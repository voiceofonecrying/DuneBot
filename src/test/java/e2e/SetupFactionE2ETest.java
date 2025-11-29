package e2e;

import model.Game;
import model.factions.Faction;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import testutil.discord.builders.MockSlashCommandEventBuilder;
import testutil.discord.state.MockChannelState;
import testutil.discord.state.MockThreadChannelState;
import testutil.discord.state.MockUserState;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end tests for the /setup faction subcommand.
 *
 * <p>These tests verify faction registration including:
 * <ul>
 *   <li>Adding single and multiple factions</li>
 *   <li>Preventing duplicate factions</li>
 *   <li>Case-insensitive duplicate detection</li>
 *   <li>Support for all 12 faction types (6 base + 6 expansion)</li>
 * </ul>
 */
@DisplayName("Setup Faction E2E Tests")
class SetupFactionE2ETest extends SetupCommandsE2ETestBase {

    @Test
    @DisplayName("Should add Atreides faction to game")
    void shouldAddAtreidesFactionToGame() throws Exception {
        // Given: A player user and member
        MockUserState playerUser = guildState.createUser("AtreidesPlayer");
        guildState.createMember(playerUser.getUserId());

        // And: A setup faction command event in the game-actions channel
        SlashCommandInteractionEvent event = new MockSlashCommandEventBuilder(guildState)
                .setMember(moderatorMember)
                .setCommandName("setup")
                .setSubcommandName("faction")
                .addStringOption("faction", "Atreides")
                .addUserOption("player", playerUser)
                .setChannel(getGameActionsChannel())
                .build();

        // When: The setup faction command is executed
        commandManager.onSlashCommandInteraction(event);

        // Then: An atreides-info channel should be created
        MockChannelState atreidesChannel = guildState.getChannels().stream()
                .filter(ch -> ch.getChannelName().equals("atreides-info"))
                .findFirst()
                .orElseThrow();

        assertThat(atreidesChannel).isNotNull();
        assertThat(atreidesChannel.getCategoryId()).isEqualTo(gameCategory.getCategoryId());

        // And: Three threads should be created for the faction
        List<MockThreadChannelState> threads = guildState.getThreadsInChannel(atreidesChannel.getChannelId());
        assertThat(threads).hasSize(3);
        assertThat(threads).extracting(MockThreadChannelState::getThreadName)
                .containsExactlyInAnyOrder("notes", "chat", "ledger");
    }

    @Test
    @DisplayName("Should add multiple factions to game")
    void shouldAddMultipleFactions() throws Exception {
        // Given: Multiple player users
        MockUserState atreidesPlayer = guildState.createUser("AtreidesPlayer");
        guildState.createMember(atreidesPlayer.getUserId());

        MockUserState harkonnenPlayer = guildState.createUser("HarkonnenPlayer");
        guildState.createMember(harkonnenPlayer.getUserId());

        MockUserState emperorPlayer = guildState.createUser("EmperorPlayer");
        guildState.createMember(emperorPlayer.getUserId());

        // When: Multiple factions are added
        SlashCommandInteractionEvent atreidesEvent = new MockSlashCommandEventBuilder(guildState)
                .setMember(moderatorMember)
                .setCommandName("setup")
                .setSubcommandName("faction")
                .addStringOption("faction", "Atreides")
                .addUserOption("player", atreidesPlayer)
                .setChannel(getGameActionsChannel())
                .build();
        commandManager.onSlashCommandInteraction(atreidesEvent);

        SlashCommandInteractionEvent harkonnenEvent = new MockSlashCommandEventBuilder(guildState)
                .setMember(moderatorMember)
                .setCommandName("setup")
                .setSubcommandName("faction")
                .addStringOption("faction", "Harkonnen")
                .addUserOption("player", harkonnenPlayer)
                .setChannel(getGameActionsChannel())
                .build();
        commandManager.onSlashCommandInteraction(harkonnenEvent);

        SlashCommandInteractionEvent emperorEvent = new MockSlashCommandEventBuilder(guildState)
                .setMember(moderatorMember)
                .setCommandName("setup")
                .setSubcommandName("faction")
                .addStringOption("faction", "Emperor")
                .addUserOption("player", emperorPlayer)
                .setChannel(getGameActionsChannel())
                .build();
        commandManager.onSlashCommandInteraction(emperorEvent);

        // Then: Three faction info channels should be created
        List<MockChannelState> factionChannels = guildState.getChannelsInCategory(gameCategory.getCategoryId())
                .stream()
                .filter(ch -> ch.getChannelName().endsWith("-info") && !ch.getChannelName().equals("mod-info"))
                .toList();

        assertThat(factionChannels).hasSize(3);
        assertThat(factionChannels).extracting(MockChannelState::getChannelName)
                .containsExactlyInAnyOrder("atreides-info", "harkonnen-info", "emperor-info");
    }

    @Test
    @DisplayName("Should not allow adding the same faction twice")
    void shouldNotAllowAddingSameFactionTwice() throws Exception {
        // Given: Atreides faction is already added
        MockUserState firstPlayer = guildState.createUser("FirstAtreidesPlayer");
        guildState.createMember(firstPlayer.getUserId());

        SlashCommandInteractionEvent firstEvent = new MockSlashCommandEventBuilder(guildState)
                .setMember(moderatorMember)
                .setCommandName("setup")
                .setSubcommandName("faction")
                .addStringOption("faction", "Atreides")
                .addUserOption("player", firstPlayer)
                .setChannel(getGameActionsChannel())
                .build();

        commandManager.onSlashCommandInteraction(firstEvent);

        // Verify first faction was added successfully
        guildState.getChannels().stream()
                .filter(ch -> ch.getChannelName().equals("atreides-info"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("First Atreides faction should be added successfully"));

        // When: Trying to add Atreides faction again with a different player
        MockUserState secondPlayer = guildState.createUser("SecondAtreidesPlayer");
        guildState.createMember(secondPlayer.getUserId());

        SlashCommandInteractionEvent secondEvent = new MockSlashCommandEventBuilder(guildState)
                .setMember(moderatorMember)
                .setCommandName("setup")
                .setSubcommandName("faction")
                .addStringOption("faction", "Atreides")
                .addUserOption("player", secondPlayer)
                .setChannel(getGameActionsChannel())
                .build();

        // Record the number of channels before attempting duplicate
        int channelCountBefore = guildState.getChannels().size();
        int atreidesInfoCountBefore = (int) guildState.getChannels().stream()
                .filter(ch -> ch.getChannelName().equals("atreides-info"))
                .count();

        // Attempt to add the duplicate faction
        commandManager.onSlashCommandInteraction(secondEvent);

        // Then: No new atreides-info channel should be created
        int channelCountAfter = guildState.getChannels().size();
        int atreidesInfoCountAfter = (int) guildState.getChannels().stream()
                .filter(ch -> ch.getChannelName().equals("atreides-info"))
                .count();

        assertThat(atreidesInfoCountAfter)
                .as("Should not create duplicate atreides-info channel")
                .isEqualTo(atreidesInfoCountBefore)
                .isEqualTo(1);

        // The total channel count should not increase (no new faction channels created)
        assertThat(channelCountAfter)
                .as("No new channels should be created for duplicate faction")
                .isEqualTo(channelCountBefore);

        // Verify the game state still has only one Atreides faction
        Game currentGame = parseGameFromBotData();
        long atreidesCount = currentGame.getFactions().stream()
                .filter(faction -> faction.getName().equals("Atreides"))
                .count();

        assertThat(atreidesCount)
                .as("Game should have exactly one Atreides faction")
                .isEqualTo(1);
    }

    @Test
    @DisplayName("Should not allow adding the same faction with different case")
    void shouldNotAllowAddingSameFactionWithDifferentCase() throws Exception {
        // Given: Harkonnen faction is already added with lowercase
        MockUserState firstPlayer = guildState.createUser("FirstHarkonnenPlayer");
        guildState.createMember(firstPlayer.getUserId());

        SlashCommandInteractionEvent firstEvent = new MockSlashCommandEventBuilder(guildState)
                .setMember(moderatorMember)
                .setCommandName("setup")
                .setSubcommandName("faction")
                .addStringOption("faction", "harkonnen")  // lowercase
                .addUserOption("player", firstPlayer)
                .setChannel(getGameActionsChannel())
                .build();

        commandManager.onSlashCommandInteraction(firstEvent);

        // Verify first faction was added successfully
        guildState.getChannels().stream()
                .filter(ch -> ch.getChannelName().equals("harkonnen-info"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("First Harkonnen faction should be added successfully"));

        // When: Trying to add Harkonnen faction again with different case
        MockUserState secondPlayer = guildState.createUser("SecondHarkonnenPlayer");
        guildState.createMember(secondPlayer.getUserId());

        SlashCommandInteractionEvent secondEvent = new MockSlashCommandEventBuilder(guildState)
                .setMember(moderatorMember)
                .setCommandName("setup")
                .setSubcommandName("faction")
                .addStringOption("faction", "HARKONNEN")  // uppercase
                .addUserOption("player", secondPlayer)
                .setChannel(getGameActionsChannel())
                .build();

        int harkonnenInfoCountBefore = (int) guildState.getChannels().stream()
                .filter(ch -> ch.getChannelName().equals("harkonnen-info"))
                .count();

        // Attempt to add the duplicate faction with different case
        commandManager.onSlashCommandInteraction(secondEvent);

        // Then: No new harkonnen-info channel should be created
        int harkonnenInfoCountAfter = (int) guildState.getChannels().stream()
                .filter(ch -> ch.getChannelName().equals("harkonnen-info"))
                .count();

        assertThat(harkonnenInfoCountAfter)
                .as("Should not create duplicate harkonnen-info channel regardless of case")
                .isEqualTo(harkonnenInfoCountBefore)
                .isEqualTo(1);
    }

    @Test
    @DisplayName("Should handle all major faction types")
    void shouldHandleAllMajorFactionTypes() throws Exception {
        // Test adding all the major factions
        List<String> factions = List.of("Atreides", "Harkonnen", "Emperor", "Fremen", "Guild", "BG");

        for (String faction : factions) {
            MockUserState player = guildState.createUser(faction + "Player");
            guildState.createMember(player.getUserId());

            SlashCommandInteractionEvent event = new MockSlashCommandEventBuilder(guildState)
                    .setMember(moderatorMember)
                    .setCommandName("setup")
                    .setSubcommandName("faction")
                    .addStringOption("faction", faction)
                    .addUserOption("player", player)
                    .setChannel(getGameActionsChannel())
                    .build();

            commandManager.onSlashCommandInteraction(event);
        }

        // Verify all faction channels were created
        List<MockChannelState> factionChannels = guildState.getChannelsInCategory(gameCategory.getCategoryId())
                .stream()
                .filter(ch -> ch.getChannelName().endsWith("-info") && !ch.getChannelName().equals("mod-info"))
                .toList();

        assertThat(factionChannels).hasSize(6);
    }

    @Test
    @DisplayName("Should handle all expansion faction types")
    void shouldHandleAllExpansionFactionTypes() throws Exception {
        // Test adding all the expansion factions
        List<String> expansionFactions = List.of("Ix", "BT", "CHOAM", "Richese", "Moritani", "Ecaz");

        for (String faction : expansionFactions) {
            MockUserState player = guildState.createUser(faction + "Player");
            guildState.createMember(player.getUserId());

            SlashCommandInteractionEvent event = new MockSlashCommandEventBuilder(guildState)
                    .setMember(moderatorMember)
                    .setCommandName("setup")
                    .setSubcommandName("faction")
                    .addStringOption("faction", faction)
                    .addUserOption("player", player)
                    .setChannel(getGameActionsChannel())
                    .build();

            commandManager.onSlashCommandInteraction(event);
        }

        // Verify all expansion faction channels were created
        List<MockChannelState> factionChannels = guildState.getChannelsInCategory(gameCategory.getCategoryId())
                .stream()
                .filter(ch -> ch.getChannelName().endsWith("-info"))
                .filter(ch -> !ch.getChannelName().equals("mod-info"))
                .filter(ch -> {
                    String channelName = ch.getChannelName();
                    // Check if it's one of the expansion faction channels
                    return channelName.equals("ix-info") ||
                           channelName.equals("bt-info") ||
                           channelName.equals("choam-info") ||
                           channelName.equals("richese-info") ||
                           channelName.equals("moritani-info") ||
                           channelName.equals("ecaz-info");
                })
                .toList();

        assertThat(factionChannels)
                .as("All 6 expansion faction info channels should be created")
                .hasSize(6);

        // Verify the faction names in the channels
        List<String> channelNames = factionChannels.stream()
                .map(MockChannelState::getChannelName)
                .toList();

        assertThat(channelNames)
                .as("Should have all expansion faction info channels")
                .containsExactlyInAnyOrder(
                        "ix-info",
                        "bt-info",
                        "choam-info",
                        "richese-info",
                        "moritani-info",
                        "ecaz-info"
                );

        // Verify game state has all the expansion factions
        Game updatedGame = parseGameFromBotData();
        assertThat(updatedGame.getFactions())
                .as("Game should have 6 expansion factions")
                .hasSize(6);

        // Verify each expansion faction type is present
        assertThat(updatedGame.getFactions().stream().map(Faction::getName))
                .as("Should have all expansion faction types")
                .containsExactlyInAnyOrder("Ix", "BT", "CHOAM", "Richese", "Moritani", "Ecaz");
    }
}
