package model;

import model.topics.DuneTopic;

import java.util.ArrayList;
import java.util.List;

public class TestTopic implements DuneTopic {
    List<String> messages = new ArrayList<>();

    public List<String> getMessages() {
        return messages;
    }

    public void publish(String message) {
        messages.add(message);
    }
}
