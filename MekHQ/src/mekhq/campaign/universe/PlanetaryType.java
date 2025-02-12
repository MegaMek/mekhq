package mekhq.campaign.universe;

public enum PlanetaryType {
    ASTEROID_BELT("Asteroid Belt"),
    DWARF_TERRESTRIAL("Dwarf Terrestrial"),
    TERRESTRIAL ("Terrestrial"),
    GIANT_TERRESTRIAL("Giant Terrestrial"),
    ICE_GIANT("Ice Giant"),
    GAS_GIANT("Gas Giant");

    public final String name;

    private PlanetaryType(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
