package model;

import model.topics.DuneTopic;

import java.util.ArrayList;
import java.util.List;

public class TestTopic implements DuneTopic {
    final List<String> messages = new ArrayList<>();

    public List<String> getMessages() {
        return messages;
    }

    @Override
    public void publish(String message) {
        messages.add(message);
    }

    @Override
    public void publish(String message, List<DuneChoice> choices) {
        messages.add(message);
    }
}
