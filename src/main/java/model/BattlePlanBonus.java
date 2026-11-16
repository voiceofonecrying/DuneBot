package model;

public record BattlePlanBonus(float value, String description) {
    public static final BattlePlanBonus ZERO = new BattlePlanBonus(0, "");

    public BattlePlanBonus add(BattlePlanBonus other) {
        return new BattlePlanBonus(value + other.value, description + other.description);
    }
}
