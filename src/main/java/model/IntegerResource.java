package model;

import exceptions.InvalidGameStateException;

import java.text.MessageFormat;

public class IntegerResource extends Resource<Integer> {
    private final int minValue;
    private final int maxValue;

    public IntegerResource(String name, Integer value, Integer minValue, Integer maxValue) {
        super(name, value);

        this.minValue = minValue == null ? Integer.MIN_VALUE : minValue;
        this.maxValue = maxValue == null ? Integer.MAX_VALUE : maxValue;
    }

    public int addValue(int value) throws InvalidGameStateException {
        int newValue = getValue() + value;
        if ((newValue < minValue) || (newValue > maxValue)) {
            throw new InvalidGameStateException(
                    MessageFormat.format("Resource {0} outside of valid range.", getName())
            );
        }

        setValue(newValue);
        return newValue;
    }
}
