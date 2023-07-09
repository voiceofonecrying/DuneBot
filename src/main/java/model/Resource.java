package model;

public class Resource<V> {

    private final String name;
    private final V value;

    public Resource(String name, V value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public V getValue() {
        return value;
    }
}
