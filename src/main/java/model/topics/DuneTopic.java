package model.topics;

import model.DuneChoice;

import java.util.List;

public interface DuneTopic {
    void publish(String message);

    void publish(String message, List<DuneChoice> choices);
}
