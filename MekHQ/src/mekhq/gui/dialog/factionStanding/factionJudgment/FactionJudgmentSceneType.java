package mekhq.gui.dialog.factionStanding.factionJudgment;

public enum FactionJudgmentSceneType {
    DISBAND("DISBAND"),
    GO_ROGUE_WARNING("GO_ROGUE_WARNING"),
    GO_ROGUE_RETIRED("GO_ROGUE_RETIRED"),
    GO_ROGUE_IMPRISONED("GO_ROGUE_IMPRISONED"),
    GO_ROGUE_REPLACED("GO_ROGUE_REPLACED"),
    GO_ROGUE_DISBAND("GO_ROGUE_DISBAND"),
    SEPPUKU("SEPPUKU");

    private final String lookUpName;

    FactionJudgmentSceneType(String lookUp) {
        this.lookUpName = lookUp;
    }

    public String getLookUpName() {
        return lookUpName;
    }
}
