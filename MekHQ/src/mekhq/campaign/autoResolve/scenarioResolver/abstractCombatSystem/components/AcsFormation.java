package mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.components;

import megamek.common.Entity;
import megamek.common.alphaStrike.ASRange;
import megamek.common.strategicBattleSystems.SBFFormation;

import java.util.Map;

public class AcsFormation extends SBFFormation {

    private int targetFormationId = Entity.NONE;
    private EngagementControl engagementControl;
    private boolean engagementControlFailed;
    private int fatigue = 0;
    private ASRange range;

    private Map<Integer, ASRange> rangeAgainstFormations;

    public EngagementControl getEngagementControl() {
        return engagementControl;
    }

    public void setEngagementControl(EngagementControl engagementControl) {
        this.engagementControl = engagementControl;
    }

    public int getTargetFormationId() {
        return targetFormationId;
    }

    public void setTargetFormationId(int targetFormationId) {
        this.targetFormationId = targetFormationId;
    }

    public boolean isEngagementControlFailed() {
        return engagementControlFailed;
    }

    public void setEngagementControlFailed(boolean engagementControlFailed) {
        this.engagementControlFailed = engagementControlFailed;
    }

    public int getFatigue() {
        return fatigue;
    }

    public void setFatigue(int fatigue) {
        this.fatigue = fatigue;
    }

    public boolean isRangeSet(int formationId) {
        return rangeAgainstFormations.containsKey(formationId);
    }

    public ASRange getRange(int formationId) {
        return rangeAgainstFormations.getOrDefault(formationId, ASRange.LONG);
    }

    public void setRange(int formationId, ASRange range) {
        this.rangeAgainstFormations.put(formationId, range);
    }
}
