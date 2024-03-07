package model;

import model.topics.DuneTopic;

import java.util.ArrayList;
import java.util.List;

public class TestTopic implements DuneTopic {
    final List<String> messages;
    private final List<List<DuneChoice>> choices;

    public TestTopic() {
        messages = new ArrayList<>();
        choices = new ArrayList<>();
    }

    public List<String> getMessages() {
        return messages;
    }

    public List<List<DuneChoice>> getChoices() {
        return choices;
    }

    @Override
    public void publish(String message) {
        messages.add(message);
    }

    @Override
    public void publish(String message, List<DuneChoice> messageChoices) {
        messages.add(message);
        choices.add(messageChoices);
    }
}
