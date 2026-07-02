package mekhq.campaign.mission.mission.contractGeneration;

public class NegotiationsModifierData {
    private double employmentMultiplier = 0.0;
    private double tempoMultiplier = 0.0;
    private int commandModifier = 0;
    private int salvageModifier = 0;
    private int supportModifier = 0;
    private int transportModifier = 0;

    public NegotiationsModifierData() {}

    public double getEmploymentMultiplier() {
        return employmentMultiplier;
    }

    public void modifyEmploymentMultiplier(double delta) {
        employmentMultiplier += delta;
    }

    public double getTempoMultiplier() {
        return tempoMultiplier;
    }

    public void modifyTempoMultiplier(double delta) {
        tempoMultiplier += delta;
    }

    public int getCommandModifier() {
        return commandModifier;
    }

    public void modifyCommandModifier(int delta) {
        commandModifier += delta;
    }

    public int getSalvageModifier() {
        return salvageModifier;
    }

    public void modifySalvageModifier(int delta) {
        salvageModifier += delta;
    }

    public int getSupportModifier() {
        return supportModifier;
    }

    public void modifySupportModifier(int delta) {
        supportModifier += delta;
    }

    public int getTransportModifier() {
        return transportModifier;
    }

    public void modifyTransportModifier(int delta) {
        transportModifier += delta;
    }
}
