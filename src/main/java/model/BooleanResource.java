package model;

public class BooleanResource extends Resource<Boolean>{
    public BooleanResource(String name, Boolean value) {
        super(name, value);
    }

    public boolean toggle() {
        Boolean newValue = !getValue();
        setValue(newValue);

        return newValue;
    }
}
