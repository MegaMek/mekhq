package mekhq.campaign.mission.atb;

import java.util.ArrayList;
import java.util.ResourceBundle;

import megamek.common.Entity;
import mekhq.campaign.Campaign;

public interface IAtBScenario {
    public int getScenarioType();
    public String getScenarioTypeDescription();
    public void setExtraScenarioForces(Campaign campaign, ArrayList<Entity> allyEntities, ArrayList<Entity> enemyEntities);
    public boolean canAddDropShips();
    public boolean isStandardScenario();
    public boolean isSpecialScenario();
    public boolean isBigBattle();
    public String getResourceKey();
    public ResourceBundle getResourceBundle();
}
