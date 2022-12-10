package model;

//Name = the faction name, or the special troop type (Sardaukar, Advisor, etc)
//value = the strength of the force
public class Force extends Resource<Integer>{

    public Force(String name, int value) {
        super(name, value);
    }
}