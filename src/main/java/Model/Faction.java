package Model;

import org.hibernate.annotations.GenericGenerator;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity
public class Faction {
    @Id
    @GeneratedValue(generator = "incrementor")
    @GenericGenerator(name="incrementor", strategy = "increment")
    int id;
    String player;
    String name;
    int spice;
    int lostForces;

    public Faction(String player, String name, int spice, int lostForces) {
        this.player = player;
        this.name = name;
        this.spice = spice;
        this.lostForces = lostForces;
    }

    public Faction() {}
}