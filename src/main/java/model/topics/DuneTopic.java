package model.topics;

import model.DuneChoice;
import model.Game;

import java.io.IOException;
import java.util.List;

public interface DuneTopic {
    void publish(String message);

    void publish(String message, List<DuneChoice> choices);

    void reply(String message);

    void reply(String message, List<DuneChoice> choices);

    void showMap(Game game) throws IOException;
}
