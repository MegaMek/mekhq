package mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem;

public abstract class ID {

    private final int id;

    /**
     * Team ID is an object that just represents the ID of a thing, used to be able to use function signature overloads
     * @param id the ID of the object
     */
    protected ID(int id) {
        this.id = id;
    }

    public int getID() {
        return id;
    }

    @Override
    public String toString() {
        return String.valueOf(id);
    }
}
