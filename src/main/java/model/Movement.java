package model;

import enums.MoveType;

public class Movement {
    private MoveType moveType;
    private String movingFrom;
    private String secondMovingFrom;
    private String movingTo;
    private String mustMoveOutOf;
    private int force;
    private int secondForce;
    private int specialForce;

    private int secondSpecialForce;
    private boolean hasMoved;
    private boolean movingNoField;

    public Movement() {
    }

    public void clear() {
        this.moveType = MoveType.TBD;
        this.movingFrom = "";
        this.movingTo = "";
        this.mustMoveOutOf = null;
        this.force = 0;
        this.specialForce = 0;
        this.secondForce = 0;
        this.secondSpecialForce = 0;
        this.secondMovingFrom = "";
        this.hasMoved = true;
        this.movingNoField = false;
    }

    public MoveType getMoveType() {
        return moveType;
    }

    public void setMoveType(MoveType moveType) {
        this.moveType = moveType;
    }

    public String getChoicePrefix() {
        if (moveType == MoveType.GUILD_AMBASSADOR)
            return "ambassador-guild-";
        else if (moveType == MoveType.FREMEN_AMBASSADOR)
            return "ambassador-fremen-";
        return "";
    }

    public String getMovingFrom() {
        return movingFrom;
    }

    public void setMovingFrom(String movingFrom) {
        this.movingFrom = movingFrom;
    }

    public String getMovingTo() {
        return movingTo;
    }

    public void setMovingTo(String movingTo) {
        this.movingTo = movingTo;
    }

    public String getMustMoveOutOf() {
        return mustMoveOutOf;
    }

    public void setMustMoveOutOf(String mustMoveOutOf) {
        this.mustMoveOutOf = mustMoveOutOf;
    }

    public int getForce() {
        return force;
    }

    public void setForce(int force) {
        this.force = force;
    }

    public int getSpecialForce() {
        return specialForce;
    }

    public void setSpecialForce(int specialForce) {
        this.specialForce = specialForce;
    }

    public boolean hasMoved() {
        return hasMoved;
    }

    public void setMoved(boolean hasMoved) {
        this.hasMoved = hasMoved;
    }

    public boolean isMovingNoField() {
        return movingNoField;
    }

    public void setMovingNoField(boolean movingNoField) {
        this.movingNoField = movingNoField;
    }

    public String getSecondMovingFrom() {
        return secondMovingFrom;
    }

    public void setSecondMovingFrom(String secondMovingFrom) {
        this.secondMovingFrom = secondMovingFrom;
    }

    public int getSecondForce() {
        return secondForce;
    }

    public void setSecondForce(int secondForce) {
        this.secondForce = secondForce;
    }

    public int getSecondSpecialForce() {
        return secondSpecialForce;
    }

    public void setSecondSpecialForce(int secondSpecialForce) {
        this.secondSpecialForce = secondSpecialForce;
    }
}
