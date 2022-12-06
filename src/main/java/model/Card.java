package model;

import java.net.URL;

public class Card {
    private final String name;
    private URL imageURL;

    public Card(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public URL getImageURL() {
        return imageURL;
    }

    public void setImageURL(URL imageURL) {
        this.imageURL = imageURL;
    }
}
