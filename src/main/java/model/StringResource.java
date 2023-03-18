package model;

import exceptions.InvalidGameStateException;

import java.text.MessageFormat;

public class StringResource extends Resource<String> {
    public StringResource(String name, String value) {
        super(name, value);
    }
}
