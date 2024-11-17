package mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem;

/**
 * Effect or Situation enum for use with master table
 */
public enum EosType {
    EQUIPMENT,
    FORCE_ABILITY,
    FORMATION_MODIFIER,
    COMBAT_UNIT,
    COMBAT_MODIFIER,
    AEROSPACE_WEAPON_CLASS_USED,
    MISC_AEROSPACE_MODIFIER,
    AIR_TO_GROUND_ATTACK;

    public static final int LR_MOD = Integer.MIN_VALUE;
    public static final int AUTO_FAIL = 999;
    public static final int CMD_SKILL = -900;
    public static final int SCR_SPECIAL = -901;
    public static final int GRD_SPECIAL = -902;
    public static final int HEADHUNT_SPECIAL = -903;
    public static final int TMM_SPECIAL = -904;
}
