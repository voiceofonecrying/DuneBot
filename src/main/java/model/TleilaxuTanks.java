package model;

import java.util.LinkedList;
import java.util.List;

public class TleilaxuTanks {
    private List<Force> forces;

    public TleilaxuTanks() {
        forces = new LinkedList<>();
    }

    public List<Force> getForces() {
        return forces;
    }

    public void addForce(Force force) {
        List<Force> specificForces = forces.stream().filter(f -> f.getName().equalsIgnoreCase(force.getName())).toList();
        if (specificForces.isEmpty())
            forces.add(new Force(force.getName(), force.getStrength(), force.getFactionName()));
        else {
            specificForces.getFirst().addStrength(force.getStrength());
        }
    }

    public void removeZeroStrengthTanks() {
        forces.removeIf(f -> f.getStrength() == 0);
    }

    public Force getForceFromTanks(String forceName) {
        // This is a temporary fix for duplicates in the tanks list.
        removeZeroStrengthTanks();

        List<Force> specificForces = forces.stream().filter(f -> f.getName().equalsIgnoreCase(forceName)).toList();

        Force force;
        if (specificForces.size() > 1) {
            throw new IllegalArgumentException("Duplicate forces found in tanks list.");
        } else if (specificForces.size() == 1) {
            return specificForces.getFirst();
        } else {
            force = new Force(forceName, 0);
            forces.add(force);
            return force;
        }
    }
}
