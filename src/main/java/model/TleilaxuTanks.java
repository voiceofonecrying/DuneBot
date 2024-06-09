package model;

import java.util.LinkedList;
import java.util.List;

public class TleilaxuTanks {
    private final List<Force> forces;

    public TleilaxuTanks() {
        forces = new LinkedList<>();
    }

    public int getForceStrength(String forceName) {
        return getForce(forceName).getStrength();
    }

    public void addForces(String forceName, int amount) {
        if (forceName.equals("Advisor"))
            forceName = "BG";
        int forceStrength = getForceStrength(forceName);
        setForceStrength(forceName, forceStrength + amount);
    }

    public void removeForces(String forceName, int amount) {
        if (amount < 0) throw new IllegalArgumentException("You cannot remove a negative strength value from a force.");
        int forceStrength = getForceStrength(forceName);
        if (forceStrength < amount) throw new IllegalArgumentException("There are only " + forceStrength + " " + forceName + " forces in the tanks.");
        setForceStrength(forceName, forceStrength - amount);
    }

    private void setForceStrength(String name, int strength) {
        if (strength < 0) throw new IllegalArgumentException("You cannot set a negative strength value for a force.");
        forces.remove(getForce(name));
        if (strength > 0)
            forces.add(new Force(name, strength));
    }

    public Force getForce(String name) {
        return forces.stream().filter(force -> force.getName().equals(name)).findFirst().orElse(new Force(name, 0));
    }

    public List<Force> getForces() {
        return forces;
    }
}
