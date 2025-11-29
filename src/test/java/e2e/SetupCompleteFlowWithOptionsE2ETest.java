package e2e;

import model.Game;
import model.Leader;
import model.LeaderSkillCard;
import model.factions.BTFaction;
import model.factions.EcazFaction;
import model.factions.Faction;
import model.factions.IxFaction;
import model.factions.MoritaniFaction;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import testutil.discord.builders.MockSlashCommandEventBuilder;
import testutil.discord.state.MockButtonState;
import testutil.discord.state.MockMemberState;
import testutil.discord.state.MockMessageState;
import testutil.discord.state.MockThreadChannelState;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * E2E test for complete setup flow with game options and expansion factions.
 * Tests the setup process with:
 * - Game options: HOMEWORLDS, LEADER_SKILLS
 * - 6 expansion factions: Harkonnen, Ix, Ecaz, Moritani, BT, Choam
 * - All button and slash command interactions
 */
@DisplayName("Setup Complete Flow With Options E2E Test")
class SetupCompleteFlowWithOptionsE2ETest extends SetupCommandsE2ETestBase {

    @Test
    @DisplayName("Should complete full setup flow with HOMEWORLDS and LEADER_SKILLS options")
    void shouldCompleteFullSetupWithOptionsAndExpansionFactions() throws Exception {
        // ========== ADD GAME OPTIONS ==========
        addGameOption("HOMEWORLDS");
        addGameOption("LEADER_SKILLS");

        // ========== SETUP FACTIONS ==========
        addFactions("Harkonnen", "Ix", "Ecaz", "Moritani", "BT", "Choam");

        // Setup all card image channels for the game options
        setupCardImageChannels();

        // Pre-populate CardImages cache for LEADER_SKILLS option
        // Using false for attachments because showLeaderSkillCardsStep tries to download images
        // and handles the empty case gracefully by just sending text without images
        String[] skillNames = {
                "Bureaucrat", "Diplomat", "Killer Medic", "Master of Assassins",
                "Mentat", "Planetologist", "Prana Bindu Adept", "Rihani Decipherer",
                "Sandmaster", "Smuggler", "Spice Banker", "Suk Graduate",
                "Swordmaster of Ginaz", "Warmaster"
        };
        populateCardImagesCache("leader-skills", skillNames, false);

        // Pre-populate CardImages cache for leader images (used by getLeaderImageLink in leader skill embeds)
        // Only need leaders for factions in the game: Harkonnen, Ix, Ecaz, Moritani, BT, Choam
        String[] leaderNames = {
                // Harkonnen
                "Umman Kudu", "Feyd Rautha", "Beast Rabban", "Cpt. Iakin Nefud", "Piter de Vries",
                // Ix
                "Tessia Vernius", "Kailea Vernius", "Cammar Pilru", "Dominic Vernius", "C'Tair Pilru",
                // Ecaz
                "Sanya Ecaz", "Whitmore Bludd", "Ilesa Ecaz", "Rivvy Dinari", "Bindikk Narvi",
                // Moritani
                "Lupino Ord", "Grieu Kronos", "Hiih Resser", "Trin Kronos", "Vando Terboli",
                // BT
                "Hidar Fen Ajidica", "Master Zaaf", "Zoal", "Wykk", "Blin",
                // CHOAM
                "Viscount Tull", "Duke Verdun", "Auditor", "Rajiv Londine", "Lady Jalma", "Frankos Aru"
        };
        populateCardImagesCache("leaders", leaderNames);

        // Pre-populate CardImages cache for Ecaz ambassadors (required for EcazView embeds)
        String[] ambassadorNames = {
                "Ecaz", "Atreides", "BG", "CHOAM", "Emperor", "Fremen",
                "Harkonnen", "Ix", "Richese", "Guild", "BT"
        };
        populateCardImagesCache("ecaz-ambassadors", ambassadorNames);

        // Pre-populate CardImages cache for HOMEWORLDS option
        String[] homeworldNames = {
                "Kaitain", "Salusa Secundus", "Junction", "Caladan", "Richese",
                "Tleilax", "Ix", "Tupile", "Wallach Ix", "Ecaz", "Giedi Prime",
                "Grumman", "Southern Hemisphere"
        };
        populateCardImagesCache("homeworld-images", homeworldNames);
        populateCardImagesCache("homeworld-cards", homeworldNames);

        Game game = parseGameFromBotData();
        assertThat(game.getFactions()).hasSize(6);
        assertThat(game.hasGameOption(enums.GameOption.HOMEWORLDS)).isTrue();
        assertThat(game.hasGameOption(enums.GameOption.LEADER_SKILLS)).isTrue();

        // ========== STEP: CREATE_DECKS (auto) ==========
        executeSetupAdvance();
        assertMessageContains(getModInfoChannel(), "Starting step CREATE_DECKS");

        game = parseGameFromBotData();
        assertThat(game.getTreacheryDeck()).isNotEmpty();
        assertThat(game.getSpiceDeck()).isNotEmpty();
        assertThat(game.getTraitorDeck()).isNotEmpty();

        // ========== STEP: FACTION_POSITIONS (auto-continues) ==========
        // Positions are randomized; advance continues automatically

        // ========== STEP: IX_CARD_SELECTION (2 button clicks) ==========
        // With LEADER_SKILLS, IX_CARD_SELECTION comes before TREACHERY_CARDS
        assertMessageContains(getModInfoChannel(), "Starting step IX_CARD_SELECTION");
        completeIxCardSelection();

        // ========== STEP: TREACHERY_CARDS (auto) ==========
        // Cards are dealt automatically
        assertMessageContains(getModInfoChannel(), "Starting step TREACHERY_CARDS");

        // ========== STEP: LEADER_SKILL_CARDS (needs input from each faction) ==========
        assertMessageContains(getModInfoChannel(), "Starting step LEADER_SKILL_CARDS");

        // Each faction receives 2 leader skill cards and must select a leader + skill
        game = parseGameFromBotData();
        for (Faction faction : game.getFactions()) {
            assertThat(faction.getLeaderSkillsHand())
                    .as(faction.getName() + " should have 2 leader skill cards")
                    .hasSize(2);
        }

        // Assign leader skills for each faction
        for (Faction faction : game.getFactions()) {
            assignLeaderSkillForFaction(faction.getName());
        }

        // Advance past LEADER_SKILL_CARDS to SHOW_LEADER_SKILLS
        executeSetupAdvance();

        // ========== STEP: SHOW_LEADER_SKILLS (auto-continues) ==========
        assertMessageContains(getModInfoChannel(), "Starting step SHOW_LEADER_SKILLS");

        // Verify each faction has a skilled leader
        game = parseGameFromBotData();
        for (Faction faction : game.getFactions()) {
            boolean hasSkill = faction.getLeaders().stream()
                    .anyMatch(l -> l.getSkillCard() != null);
            assertThat(hasSkill)
                    .as(faction.getName() + " should have a leader with a skill")
                    .isTrue();
        }

        // ========== STEP: ECAZ_LOYALTY (auto) ==========
        assertMessageContains(getModInfoChannel(), "Starting step ECAZ_LOYALTY");

        game = parseGameFromBotData();
        EcazFaction ecaz = game.getEcazFaction();
        assertThat(ecaz.getLoyalLeader())
                .as("Ecaz should have a loyal leader after ECAZ_LOYALTY step")
                .isNotNull();

        // ========== STEP: TRAITORS (4 button clicks) ==========
        // Ecaz, Ix, Moritani, Choam select traitors (Harkonnen/BT skip)
        for (String factionName : new String[]{"Ecaz", "Ix", "Moritani", "Choam"}) {
            selectTraitorForFaction(factionName);
        }

        game = parseGameFromBotData();
        for (String factionName : new String[]{"Ecaz", "Ix", "Moritani", "Choam"}) {
            Faction faction = game.getFaction(factionName);
            assertThat(faction.getTraitorHand())
                    .as(factionName + " should have exactly 1 traitor")
                    .hasSize(1);
        }

        // ========== STEP: BT_FACE_DANCERS (auto) ==========
        // Face dancers drawn automatically

        // ========== STEP: MORITANI_FORCE (button clicks) ==========
        // Moritani places 6 forces using the category-based flow
        completeMoritaniForcePlacementWithCategory("stronghold", "Carthag");

        game = parseGameFromBotData();
        MoritaniFaction moritani = game.getMoritaniFaction();
        assertThat(moritani.getReservesStrength())
                .as("Moritani reserves should be less than 20 after placing forces")
                .isLessThan(20);

        // ========== STEP: STORM_SELECTION (2 button clicks) ==========
        // First and last factions in position order submit dials
        game = parseGameFromBotData();
        Faction firstFaction = game.getFactions().getFirst();
        Faction lastFaction = game.getFactions().getLast();

        submitStormDial(firstFaction.getName(), 10);
        submitStormDial(lastFaction.getName(), 5);

        game = parseGameFromBotData();
        assertThat(game.getStorm()).isBetween(1, 18);

        // ========== STEP: IX_HMS_PLACEMENT (4 button clicks) ==========
        completeIxHmsPlacement("spice-blow", "Cielago South");

        game = parseGameFromBotData();
        IxFaction ixAfterHms = game.getIxFaction();
        assertThat(ixAfterHms.getTerritoryWithHMS()).isNotNull();

        // Verify BT has face dancers
        BTFaction bt = game.getBTFaction();
        assertThat(bt.getTraitorHand()).hasSize(3); // Face dancers stored in traitor hand

        // ========== STEP: START_GAME (auto) ==========
        game = parseGameFromBotData();
        assertThat(game.getTurn()).isEqualTo(1);
        assertThat(game.isSetupFinished()).isTrue();

        // ========== FINAL VERIFICATION ==========
        // Verify game options are still set
        assertThat(game.hasGameOption(enums.GameOption.HOMEWORLDS)).isTrue();
        assertThat(game.hasGameOption(enums.GameOption.LEADER_SKILLS)).isTrue();

        assertThat(getModInfoChannel().getMessages())
                .extracting(MockMessageState::getContent)
                .anyMatch(content -> content.contains("Setup Complete") || content.contains("START_GAME") || content.contains("game has begun"));
    }

    // ===== HELPER METHODS =====

    /**
     * Assigns a leader skill to a faction.
     * Picks the first leader and first available skill card from the faction's hand.
     */
    private void assignLeaderSkillForFaction(String factionName) throws Exception {
        Game game = parseGameFromBotData();
        Faction faction = game.getFaction(factionName);

        // Get the first leader that doesn't already have a skill
        Leader leader = faction.getLeaders().stream()
                .filter(l -> l.getSkillCard() == null)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No leaders without skills for " + factionName));

        // Get the first skill card from the faction's hand
        LeaderSkillCard skillCard = faction.getLeaderSkillsHand().stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No skill cards for " + factionName));

        // Execute /setup leader-skill command
        SlashCommandInteractionEvent event = new MockSlashCommandEventBuilder(guildState)
                .setMember(moderatorMember)
                .setCommandName("setup")
                .setSubcommandName("leader-skill")
                .addStringOption("factionname", factionName)
                .addStringOption("factionleader", leader.getName())
                .addStringOption("factionleaderskill", skillCard.name())
                .setChannel(getGameActionsChannel())
                .build();
        commandManager.onSlashCommandInteraction(event);
    }

    private void completeMoritaniForcePlacementWithCategory(String category, String territory) throws Exception {
        MockThreadChannelState moritaniChat = getFactionChatThread("Moritani");
        MockMemberState moritaniMember = getFactionMember("Moritani");

        // Step 1: Click category button (stronghold)
        String categoryButtonId = category + "-starting-forces";
        clickButton(categoryButtonId, moritaniChat, moritaniMember);

        // Step 2: Click territory button
        moritaniChat = getFactionChatThread("Moritani"); // Refresh
        String territoryButtonId = "ship-starting-forces-" + territory;
        clickButton(territoryButtonId, moritaniChat, moritaniMember);

        // Step 3: Execute placement
        moritaniChat = getFactionChatThread("Moritani"); // Refresh
        clickButton("execute-shipment-starting-forces", moritaniChat, moritaniMember);
    }

    private void completeIxCardSelection() throws Exception {
        MockThreadChannelState ixChat = getFactionChatThread("Ix");
        MockMemberState ixMember = getFactionMember("Ix");

        // Step 1: Find card selection message and click first available card
        // Button IDs are: ix-starting-card-N-CardName
        MockMessageState cardMessage = findMessageWithButtonPattern(ixChat, "ix-starting-card-");
        if (cardMessage == null) {
            throw new IllegalStateException("No Ix card selection message found");
        }
        String firstCardButton = cardMessage.getButtons().get(0).getComponentId();
        clickButton(firstCardButton, ixChat, ixMember);

        // Step 2: Find and click the confirmation button
        // Button IDs are: ix-confirm-start-{CardName}
        // Refresh the chat to get the new confirmation message
        ixChat = getFactionChatThread("Ix");
        MockMessageState confirmMessage = findMessageWithButtonPattern(ixChat, "ix-confirm-start-");
        if (confirmMessage == null) {
            throw new IllegalStateException("No Ix card confirmation message found");
        }
        // Click the first button which should be the "Confirm" button (not "Choose a different card")
        String confirmButton = confirmMessage.getButtons().stream()
                .filter(btn -> btn.getComponentId().startsWith("ix-confirm-start-") &&
                               !btn.getComponentId().contains("reset"))
                .findFirst()
                .map(MockButtonState::getComponentId)
                .orElseThrow(() -> new IllegalStateException("No confirm button found"));
        clickButton(confirmButton, ixChat, ixMember);
    }

    private void completeIxHmsPlacement(String category, String territory) throws Exception {
        MockThreadChannelState ixChat = getFactionChatThread("Ix");
        MockMemberState ixMember = getFactionMember("Ix");

        // Step 1: Click category button (spice-blow or other)
        String categoryButtonId = category + "-hms-placement";
        clickButton(categoryButtonId, ixChat, ixMember);

        // Step 2: Click territory button
        // Button ID pattern is: ship-hms-placement-{territory}
        ixChat = getFactionChatThread("Ix"); // Refresh after category click
        String territoryButtonId = "ship-hms-placement-" + territory;
        clickButton(territoryButtonId, ixChat, ixMember);

        // Step 3: If territory has multiple sectors, select a sector
        ixChat = getFactionChatThread("Ix"); // Refresh after territory click
        MockMessageState sectorMessage = findMessageWithButtonPattern(ixChat, "ship-sector-hms-placement-");
        if (sectorMessage != null) {
            // Click the first sector button (not the reset button)
            String sectorButton = sectorMessage.getButtons().stream()
                    .filter(btn -> btn.getComponentId().startsWith("ship-sector-hms-placement-"))
                    .findFirst()
                    .map(MockButtonState::getComponentId)
                    .orElseThrow(() -> new IllegalStateException("No sector button found"));
            clickButton(sectorButton, ixChat, ixMember);
        }

        // Step 4: Execute placement
        ixChat = getFactionChatThread("Ix"); // Refresh after sector click
        clickButton("execute-shipment-hms-placement", ixChat, ixMember);
    }
}
