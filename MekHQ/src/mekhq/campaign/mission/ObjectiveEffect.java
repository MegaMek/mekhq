package mekhq.campaign.mission;

/**
 * A data structure containing metadata relevant to the effect that completing or failing an objective 
 * can have. 
 * @author NickAragua
 *
 */
public class ObjectiveEffect {
    
    public enum EffectScalingType {
        None,
        Linear,
        Inverted
    }
    
    public enum ObjectiveEffectType {
        ScenarioVictory,
        ScenarioDefeat,
        ContractScoreUpdate,
        SupportPointUpdate,
        ContractMoraleUpdate,
        ContractVictory,
        ContractDefeat,
        BVBudgetUpdate,
        AtBBonus
    }        
    
    public ObjectiveEffectType effectType;
    // whether the effect is scaled to the # of units or fixed in nature
    public EffectScalingType effectScaling = EffectScalingType.None;
    // how much of the effect per unit, or how much of the effect fixed
    public int howMuch;
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(effectType.toString());
        sb.append(" - ");
        sb.append(effectScaling.toString());
        sb.append(" - ");
        sb.append(howMuch);
        return sb.toString();
    }
}
