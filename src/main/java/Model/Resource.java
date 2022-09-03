package Model;

public class Resource {
    private String name;
    private String sValue;
    private int iValue;

    public Resource(String name, Object value) {
        this.name = name;
        if (value instanceof String) this.sValue = (String) value;
        else this.iValue = (int) value;
    }

}
