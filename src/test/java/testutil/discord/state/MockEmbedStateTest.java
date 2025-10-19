package testutil.discord.state;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MockEmbedStateTest {

    private MockEmbedState embed;

    @BeforeEach
    void setUp() {
        embed = new MockEmbedState();
    }

    @Test
    void constructor_createsEmptyEmbed() {
        assertThat(embed.getTitle()).isNull();
        assertThat(embed.getDescription()).isNull();
        assertThat(embed.getColor()).isNull();
        assertThat(embed.getFields()).isEmpty();
        assertThat(embed.getFooter()).isNull();
        assertThat(embed.getImageUrl()).isNull();
        assertThat(embed.getThumbnailUrl()).isNull();
        assertThat(embed.getAuthorName()).isNull();
        assertThat(embed.getAuthorIconUrl()).isNull();
    }

    @Test
    void setTitle_setsTitle() {
        embed.setTitle("Storm Phase");

        assertThat(embed.getTitle()).isEqualTo("Storm Phase");
    }

    @Test
    void setTitle_canBeChanged() {
        embed.setTitle("First Title");
        embed.setTitle("Second Title");

        assertThat(embed.getTitle()).isEqualTo("Second Title");
    }

    @Test
    void setDescription_setsDescription() {
        embed.setDescription("The storm has moved!");

        assertThat(embed.getDescription()).isEqualTo("The storm has moved!");
    }

    @Test
    void setDescription_handlesMultilineText() {
        String multiline = "Line 1\nLine 2\nLine 3";
        embed.setDescription(multiline);

        assertThat(embed.getDescription()).isEqualTo(multiline);
    }

    @Test
    void setColor_setsColor() {
        embed.setColor(Color.BLUE);

        assertThat(embed.getColor()).isEqualTo(Color.BLUE);
    }

    @Test
    void setColor_supportsVariousColors() {
        embed.setColor(Color.RED);
        assertThat(embed.getColor()).isEqualTo(Color.RED);

        embed.setColor(Color.GREEN);
        assertThat(embed.getColor()).isEqualTo(Color.GREEN);

        embed.setColor(new Color(255, 165, 0)); // Orange
        assertThat(embed.getColor()).isEqualTo(new Color(255, 165, 0));
    }

    @Test
    void addField_withFieldObject_addsFieldToEmbed() {
        MockEmbedFieldState field = new MockEmbedFieldState("Spice", "5", true);
        embed.addField(field);

        List<MockEmbedFieldState> fields = embed.getFields();

        assertThat(fields).hasSize(1);
        assertThat(fields.get(0).getName()).isEqualTo("Spice");
    }

    @Test
    void addField_withParameters_addsFieldToEmbed() {
        embed.addField("Spice", "5", true);

        List<MockEmbedFieldState> fields = embed.getFields();

        assertThat(fields).hasSize(1);
        assertThat(fields.get(0).getName()).isEqualTo("Spice");
        assertThat(fields.get(0).getValue()).isEqualTo("5");
        assertThat(fields.get(0).isInline()).isTrue();
    }

    @Test
    void addField_addsMultipleFields() {
        embed.addField("Field 1", "Value 1", true);
        embed.addField("Field 2", "Value 2", false);
        embed.addField("Field 3", "Value 3", true);

        List<MockEmbedFieldState> fields = embed.getFields();

        assertThat(fields).hasSize(3);
        assertThat(fields.get(0).getName()).isEqualTo("Field 1");
        assertThat(fields.get(1).getName()).isEqualTo("Field 2");
        assertThat(fields.get(2).getName()).isEqualTo("Field 3");
    }

    @Test
    void getFields_returnsEmptyListInitially() {
        assertThat(embed.getFields()).isEmpty();
    }

    @Test
    void clearFields_removesAllFields() {
        embed.addField("Field 1", "Value 1", true);
        embed.addField("Field 2", "Value 2", false);

        embed.clearFields();

        assertThat(embed.getFields()).isEmpty();
    }

    @Test
    void getFields_returnsNewListEachTime() {
        embed.addField("Test", "Value", true);

        List<MockEmbedFieldState> fields1 = embed.getFields();
        List<MockEmbedFieldState> fields2 = embed.getFields();

        assertThat(fields1).isNotSameAs(fields2);
    }

    @Test
    void getFields_modifyingReturnedListDoesNotAffectState() {
        embed.addField("Original", "Value", true);

        List<MockEmbedFieldState> fields = embed.getFields();
        fields.add(new MockEmbedFieldState("Fake", "Fake Value", false));

        assertThat(embed.getFields()).hasSize(1);
        assertThat(embed.getFields().get(0).getName()).isEqualTo("Original");
    }

    @Test
    void setFooter_setsFooter() {
        embed.setFooter("Turn 3 of 10");

        assertThat(embed.getFooter()).isEqualTo("Turn 3 of 10");
    }

    @Test
    void setImageUrl_setsImageUrl() {
        embed.setImageUrl("https://example.com/image.png");

        assertThat(embed.getImageUrl()).isEqualTo("https://example.com/image.png");
    }

    @Test
    void setThumbnailUrl_setsThumbnailUrl() {
        embed.setThumbnailUrl("https://example.com/thumb.png");

        assertThat(embed.getThumbnailUrl()).isEqualTo("https://example.com/thumb.png");
    }

    @Test
    void setAuthorName_setsAuthorName() {
        embed.setAuthorName("Game Master");

        assertThat(embed.getAuthorName()).isEqualTo("Game Master");
    }

    @Test
    void setAuthorIconUrl_setsAuthorIconUrl() {
        embed.setAuthorIconUrl("https://example.com/icon.png");

        assertThat(embed.getAuthorIconUrl()).isEqualTo("https://example.com/icon.png");
    }

    @Test
    void hasFields_returnsFalseInitially() {
        assertThat(embed.hasFields()).isFalse();
    }

    @Test
    void hasFields_returnsTrueAfterAddingField() {
        embed.addField("Test", "Value", true);

        assertThat(embed.hasFields()).isTrue();
    }

    @Test
    void hasFields_returnsFalseAfterClearingFields() {
        embed.addField("Test", "Value", true);
        embed.clearFields();

        assertThat(embed.hasFields()).isFalse();
    }

    @Test
    void embed_canHaveAllPropertiesSet() {
        embed.setTitle("Complete Embed");
        embed.setDescription("A full description");
        embed.setColor(Color.CYAN);
        embed.addField("Field 1", "Value 1", true);
        embed.addField("Field 2", "Value 2", false);
        embed.setFooter("Footer text");
        embed.setImageUrl("https://example.com/image.png");
        embed.setThumbnailUrl("https://example.com/thumb.png");
        embed.setAuthorName("Author Name");
        embed.setAuthorIconUrl("https://example.com/author.png");

        assertThat(embed.getTitle()).isEqualTo("Complete Embed");
        assertThat(embed.getDescription()).isEqualTo("A full description");
        assertThat(embed.getColor()).isEqualTo(Color.CYAN);
        assertThat(embed.getFields()).hasSize(2);
        assertThat(embed.getFooter()).isEqualTo("Footer text");
        assertThat(embed.getImageUrl()).isEqualTo("https://example.com/image.png");
        assertThat(embed.getThumbnailUrl()).isEqualTo("https://example.com/thumb.png");
        assertThat(embed.getAuthorName()).isEqualTo("Author Name");
        assertThat(embed.getAuthorIconUrl()).isEqualTo("https://example.com/author.png");
        assertThat(embed.hasFields()).isTrue();
    }

    @Test
    void embed_canBePartiallyPopulated() {
        embed.setTitle("Partial Embed");
        embed.addField("Only Field", "Only Value", true);

        assertThat(embed.getTitle()).isEqualTo("Partial Embed");
        assertThat(embed.getDescription()).isNull();
        assertThat(embed.getColor()).isNull();
        assertThat(embed.getFields()).hasSize(1);
        assertThat(embed.getFooter()).isNull();
    }

    @Test
    void embed_propertiesCanBeSetToNull() {
        embed.setTitle("Title");
        embed.setTitle(null);

        assertThat(embed.getTitle()).isNull();
    }

    @Test
    void embed_mixingFieldAddMethods() {
        MockEmbedFieldState fieldObject = new MockEmbedFieldState("Field 1", "Value 1", true);
        embed.addField(fieldObject);
        embed.addField("Field 2", "Value 2", false);

        List<MockEmbedFieldState> fields = embed.getFields();

        assertThat(fields).hasSize(2);
        assertThat(fields.get(0)).isEqualTo(fieldObject);
        assertThat(fields.get(1).getName()).isEqualTo("Field 2");
    }
}
