package model;

import org.json.JSONObject;

import java.io.File;

public class GameBoard {
    private JSONObject data;
    private File board;

    public GameBoard(JSONObject data) {
        this.data = data;
    }

}
