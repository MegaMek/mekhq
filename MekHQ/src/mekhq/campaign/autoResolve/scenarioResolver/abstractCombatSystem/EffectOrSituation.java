package mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem;

import static mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.EosType.*;

/**
 * Represents the various effects and situations that can occur in a battle and their effects on the battle.
 * -> Advanced Combat System Master Modifier Table
 */
public enum EffectOrSituation {
    PROBES(EQUIPMENT, 0,0,1,1,0,0,0,0,0,0,0),
    ECM(EQUIPMENT,0,0,0,-1,0,0,0,0,-10,0,0),
    RECON(EQUIPMENT,0,0,1,0,1,1,0,0,0,0,0),
    REMOTE_SENSOR_DISPENSER(EQUIPMENT,0,0,-1,-1,1,1,0,0,0,0,0),
    LAM(EQUIPMENT,0,0,1,1,0,0,0,0,20,0,0),
    SATELLITE_RECON(EQUIPMENT,1,0,2,1,0,0,0,0,0,0,0),
    C3_NETWORK(EQUIPMENT,0,0,0,0,0,0,0,0,20,0,0),

    LEADERSHIP_RATING(FORCE_ABILITY, LR_MOD,0,0,0,LR_MOD,0,0,0,0,LR_MOD,LR_MOD),
    COMMAND_SKILLS(FORCE_ABILITY, CMD_SKILL, CMD_SKILL, CMD_SKILL, CMD_SKILL, CMD_SKILL, CMD_SKILL, CMD_SKILL, CMD_SKILL, CMD_SKILL, CMD_SKILL, CMD_SKILL),
    QUESTIONABLE_LOYALTY(FORCE_ABILITY,0,0,0,0,-2,0,1,1,0,1,0),
    RELIABLE_LOYALTY(FORCE_ABILITY,0,0,0,0,0,0,0,0,0,-1,0),
    FANATICAL_LOYALTY(FORCE_ABILITY,0,0,0,0,0,0,0,0,0,-4,0),
    SUPERIOR_COMBAT_DOCTRINE(FORCE_ABILITY,1,0,0,0,-1,0,-1,0,10,0,0),
    FLAWED_COMBAT_DOCTRINE(FORCE_ABILITY,-1,0,0,0,1,0,1,0,-10,0,0),

    FORMATION_IS_MERCENARY(FORMATION_MODIFIER,0,0,0,0,-1,0,0,1,0,0,0),
    FORMATION_IS_INFANTRY_ONLY(FORMATION_MODIFIER,0,0,0,0,2,0,1,0,0,2, -2),
    FORMATION_IS_VEHICLE_ONLY(FORMATION_MODIFIER,0,0,0,0,1,0,0,0,0,1,1),
    FORMATION_HAS_ENGINEERS(FORMATION_MODIFIER,0,0,0,0,-1,0,-1,1,-20,0,0),
    FORMATION_HAS_FIELDWORKERS_ENGINEERS(FORMATION_MODIFIER,0,0,0,0,-1,0,-1,1,-20,0,0),
    FORMATION_IS_HIDDEN(FORMATION_MODIFIER,0,0,2,0,0,0,0,0,0,0,0),
    FORMATION_CONDUCTS_AN_AMBUSH(FORMATION_MODIFIER,0,0,0,0,0,0,-1,0,0,0,0),
    FORMATION_HAS_ARRIVED_ON_TRANSPORT_ORDERS(FORMATION_MODIFIER,0,0,0,0,0,0,0,-2,0,0,0),
    ATTACKER_OR_TARGET_HAS_FORTIFY_ORDERS(FORMATION_MODIFIER,0,0,0,0,0,0,0,0, -10,0,0),
    ATTACKER_OR_TARGET_HAS_DEFEND_ORDERS(FORMATION_MODIFIER,0,0,0,0,0,0,0,0,-10,0,0),
    ATTACKER_HAS_HEADHUNT_OR_INFRASTRUCTURE_DESTRUCTION_ATTACK_ORDERS(FORMATION_MODIFIER, HEADHUNT_SPECIAL,0,0,0,0,HEADHUNT_SPECIAL,HEADHUNT_SPECIAL,0,HEADHUNT_SPECIAL,0,0),
    AEROSPACE_FORMATION(FORMATION_MODIFIER,0,-1,2,0,0,2,0,-2,0,0,0),
    RECON_FORMATION_ONE_HEX(FORMATION_MODIFIER,0,0,3,0,2,-2,1,1,-50,0,0),
    RECON_FORMATION_TWO_HEX(FORMATION_MODIFIER,0,0,2,0,2,-2,1,1,-50,0,0),
    RECON_FORMATION_THREE_HEX(FORMATION_MODIFIER,0,0,1,0,2,-2,1,1,-50,0,0),
    RECON_STRIKE(FORMATION_MODIFIER,0,0,0,0,0,0,0,2,-75, -50, 0, 0,0),
    AERIAL_RECON(FORMATION_MODIFIER,0,0,3,0,0,0,0,0,0,0,0), // average between +2 and +4
    ENGAGEMENT_CONTROL_FORCED_ENGAGEMENT(FORMATION_MODIFIER,0,0,0,0,-3,1,0,0,-50,0,0),
    ENGAGEMENT_CONTROL_OVERRUN(FORMATION_MODIFIER,0,0,0,0,0,0,0,0,-75, -50, 0, 0,0),
    ENGAGEMENT_CONTROL_SUCCESSFUL_EVADE(FORMATION_MODIFIER,0,0,0,0,-3,0,1,0,0,0,0),
    ENGAGEMENT_CONTROL_FAILED_EVADE(FORMATION_MODIFIER,0,0,0,0,0,0,1,2,0,0,0),
    ENGAGEMENT_CONTROL_REAR_GUARD_TACTIC_ATTACKER_ENTERING_FROM_PRIMARY_DIRECTION(FORMATION_MODIFIER,0,0,0,0,2,-2,0,0,0, -50, -25,0,0),
    ENGAGEMENT_CONTROL_REAR_GUARD_TACTIC_ATTACKER_ENTERING_FROM_OPPOSITE_DIRECTION(FORMATION_MODIFIER,0,0,0,0,2,0,0,0,0, -50, -25,0,0),
    ENGAGEMENT_CONTROL_REAR_GUARD_TACTIC_ATTACKER_ENTERING_FROM_SIDE_DIRECTION(FORMATION_MODIFIER,0,0,0,0,-1,-1,0,0,0, -50, -25,0,0),
    WARSHIP(FORMATION_MODIFIER,0,0,0,0,0,0,0,0,0,0,0),

    WET_BEHIND_EARS(COMBAT_UNIT, 0,3,0,-3,3,4,2,2,-20,2,4),
    REALLY_GREEN(COMBAT_UNIT, 0,2,0,-2,2,3,1,1,-10,1,3),
    GREEN(COMBAT_UNIT, 0,1,0,-1,1,2,0,0,0,0,2),
    REGULAR(COMBAT_UNIT, 0,0,0,0,0,1,-1,0,0,-1,1),
    VETERAN(COMBAT_UNIT, 0,-1,0, 1,-1,0,-2,0,10,-2,0),
    ELITE(COMBAT_UNIT, 0,-2,0,2,-2,-1,-3,0,20,-3,-1),
    HEROIC(COMBAT_UNIT, 0,-3,0,3,-3,-2,-4,0,30,-4,-2),
    LEGENDARY(COMBAT_UNIT, 0,-3,0,3,-4,-3,-4,0,40,-5,-3),
    TARGET_MOVEMENT_MODIFIER(COMBAT_UNIT, 0,0,0,0,0,0, TMM_SPECIAL,0,0,0,0),
    MORALE_SHAKEN(COMBAT_UNIT, 0,-1,-1,-1,1,0,1,-1,0,0,0),
    MORALE_UNSTEADY(COMBAT_UNIT, 0,-2,-2,-2,2,1,2,-2,0,0,0),
    MORALE_BROKEN(COMBAT_UNIT, 0, AUTO_FAIL,-3,-3,3,2,3,-3,-20,0,0),
    MORALE_RETREATING_ROUTED(COMBAT_UNIT, 0, AUTO_FAIL,-4,-4, AUTO_FAIL,2,4,-4,-40,0,0),
    FATIGUE_1(COMBAT_UNIT, 0,-1,-1,-1,1,1,1,0,0,-1,-1),
    FATIGUE_2(COMBAT_UNIT, 0,-2, -2,-2,2,2,2,0,-10,-2,-2),
    FATIGUE_3(COMBAT_UNIT, 0,-3, -3,-3,3,3,3,0,-20,-3,-3),
    FATIGUE_4(COMBAT_UNIT, 0,-4, -4,-4,4,4,4,0,-30,-4,-4),
    FATIGUE_5(COMBAT_UNIT, 0,-5, -5,-5,5,5,5,0,-40,-5,-5),
    NO_SUPPLY(COMBAT_UNIT, 0,0,0,0,4,0,3,0,-10,4,0),
    CONDUCTED_A_QUICK_MARCH(COMBAT_UNIT, 0, AUTO_FAIL,-1,-1,0,0,1,-1,0,0,0),

    ATTACK_AT_SHORT_RANGE(COMBAT_MODIFIER, 0,0,0,0,0,0,-1,0,0,0,0),
    ATTACK_AT_MEDIUM_RANGE(COMBAT_MODIFIER, 0,0,0,0,0,0,2,0,0,0,0),
    ATTACK_AT_LONG_RANGE(COMBAT_MODIFIER, 0,0,0,0,0,0,4,0,0,0,0),
    OFFENSIVE_TACTICS(COMBAT_MODIFIER, 0,0,0,0,0,0,3,0,30,0,0), // 1 to 5
    DEFENSIVE_TACTICS(COMBAT_MODIFIER, 0,0,0,0,0,0,3,0,-30,0,0), // 1 to 5
    STANDARD_TACTICS(COMBAT_MODIFIER, 0,0,0,0,0,0,0,0,0,0,0),
    TARGET_IS_SECONDARY_TARGET(COMBAT_MODIFIER, 0,0,0,0,0,0,2,0,-25, 0, 0,0,0),
    FORMATION_IS_IN_A_FORTIFICATION(COMBAT_MODIFIER, 0,0,0,0,0,0,0,0,0,0,0),
    COMBAT_DROPS(COMBAT_MODIFIER, 0,0,0,0,0,0,0,0,0,4,0),
    AMBUSHED_OR_FAILED_AMBUSH(COMBAT_MODIFIER, 0,0,0,0,0,0,0,-6,0,4,0),
    ATTACKER_SALLYING_FROM_A_CASTLE_BRIAN(COMBAT_MODIFIER, 0,0,0,0,0,-2,-2,0,-20,0,0),
    FORMATION_ATTACKED_FROM_BEHIND(COMBAT_MODIFIER, 0,0,0,0,0,0,-1,0,-20, 0, 0,0,0),
    INFANTRY_OR_PROTOMEK_IN_URBAN_HEX(COMBAT_MODIFIER, 0,2,0,0,0,0,0,1,0,0,0),

    CAPITAL_AND_SUB_CAPITAL_NON_MISSILES(AEROSPACE_WEAPON_CLASS_USED, 0,0,0,0,0,0,1,0,0,0,0),

    ADVANCED_CAPITAL_MISSILE_ATTACK(MISC_AEROSPACE_MODIFIER, 0,0,0,0,0,0,0,0,0,0,0),
    VS_TARGET_IN_SAME_VECTOR(MISC_AEROSPACE_MODIFIER, 0,0,0,0,0,0,0,0,0,0,0),
    VS_TARGET_IN_ADJACENT_SECTOR(MISC_AEROSPACE_MODIFIER, 0,0,0,0,0,0,2,0,0,0,0),
    ORBITAL_ARTILLERY_ATTACKS(MISC_AEROSPACE_MODIFIER, 0,0,0,0,0,0,0,0,0,0,0),
    ORBIT_TO_SURFACE_ATTACK(MISC_AEROSPACE_MODIFIER, 0,0,0,0,0,0,3,0,0,0,0),
    ATTACK_IS_FROM_CENTRAL_ZONE(MISC_AEROSPACE_MODIFIER, 0,0,0,0,0,0,1,0,0,0,0),
    ATTACK_IS_FROM_INNER_ZONE(MISC_AEROSPACE_MODIFIER, 0,0,0,0,0,0,4,0,0,0,0),
    GROUND_TARGET_DESIGNATED_BY_FRIENDLY_TAG(MISC_AEROSPACE_MODIFIER, 0,0,0,0,0,0,-1,0,0,0,0),
    ATTACKER_IS_A_ROBOTIC_UNIT(MISC_AEROSPACE_MODIFIER, 0,0,0,0,0,0,0,1,0,0,0),
    ATTACKER_IS_IN_A_NAVAL_C3_NETWORK_SPACE_COMBAT_ONLY(MISC_AEROSPACE_MODIFIER, 0,0,0,0,0,0,-1,0,0,0,0),
    ATTACKER_HAS_TARGETING_DAMAGE_PER_HIT(MISC_AEROSPACE_MODIFIER, 0,0,0,0,0,0,2,0,0,0,0),
    HIGH_SPEED_ATTACK(MISC_AEROSPACE_MODIFIER, 0,0,0,0,0,0,8,0,0,0,0),
    POINT_DEFENSE_VS_CAPITAL_SUB_CAPITAL_MISSILES(MISC_AEROSPACE_MODIFIER, 0,0,0,0,0,0,0,0,0,0,0),
    POINT_DEFENSE_DAMAGE_1_DMG(MISC_AEROSPACE_MODIFIER, 0,0,0,0,0,0,1,0,0,0,0),
    POINT_DEFENSE_DAMAGE_2_DMG_OR_MORE(MISC_AEROSPACE_MODIFIER, 0,0,0,0,0,0, AUTO_FAIL,0,0,0,0),
    SCREEN_LAUNCHERS_USED(MISC_AEROSPACE_MODIFIER, 0,0,0,0,0,0, SCR_SPECIAL,0,0,0,0),
    TARGET_IS_CRIPPLED_OR_DRIFTING(MISC_AEROSPACE_MODIFIER, 0,0,0,0,0,0,-2,0,0,0,0),
    TELEOPERATED_MISSILE(MISC_AEROSPACE_MODIFIER, 0,0,0,0,0,0,-1,0,0,0,0),

    GROUND_STRIKE(AIR_TO_GROUND_ATTACK, 0,0,0,0,0,0, GRD_SPECIAL,GRD_SPECIAL, 0,0,0), // special value on attacker to hit and target
    GROUND_BOMBING(AIR_TO_GROUND_ATTACK, 0,0,0,0,0,0,1,0,-75,0,0),
    ATTACKING_FORMATION_IS_BEING_ATTACKED_BY_ANOTHER_AEROSPACE_FORMATION(AIR_TO_GROUND_ATTACK, 0,0,0,0,0,0,2,0,0,0,0),
    TARGET_IS_GROUND_RECON_FORMATION(AIR_TO_GROUND_ATTACK, 0,0,0,0,0,0,3,0,0,0,0);

    private final int[] values;
    public final EosType type;

    EffectOrSituation(EosType type, int initiative, int conceal, int detection, int recon, int engagementCtrl, int maneuver, int atkToHit,
                      int target, int dmgMod, int morale, int combatDrop) {
        this(type, initiative, conceal, detection, recon, engagementCtrl, maneuver, atkToHit, target, dmgMod, dmgMod, dmgMod, morale,
            combatDrop);
    }

    EffectOrSituation(EosType type, int initiative, int conceal, int detection, int recon, int engagementCtrl, int maneuver, int atkToHit,
                      int target, int dmgModShort, int dmgModMedium, int dmgModLong, int morale, int combatDrop) {
        this.type = type;
        values = new int[13];
        values[0] = initiative;
        values[1] = conceal;
        values[2] = detection;
        values[3] = recon;
        values[4] = engagementCtrl;
        values[5] = maneuver;
        values[6] = atkToHit;
        values[7] = target;
        values[8] = dmgModShort; // this value is multiplied by 100 to fit in an integer. Its normal range is 0.0-1.0
        values[9] = dmgModMedium; // this value is multiplied by 100 to fit in an integer. Its normal range is 0.0-1.0
        values[10] = dmgModLong; // this value is multiplied by 100 to fit in an integer. Its normal range is 0.0-1.0
        values[11] = morale;
        values[12] = combatDrop;
    }

    public static boolean modifiersIsLRMOD(int value) {
        return value == LR_MOD;
    }

    public static boolean modifiersIsAutoFail(int value) {
        return value == AUTO_FAIL;
    }

    public static boolean modifiersIsCmdSkill(int value) {
        return value == CMD_SKILL;
    }

    public static boolean modifiersIsScrSpecial(int value) {
        return value == SCR_SPECIAL;
    }

    public static boolean modifierIsGroundStrikeSpecial(int value) {
        return value == GRD_SPECIAL;
    }

    public static boolean modifierIsHeadhuntSpecial(int value) {
        return value == HEADHUNT_SPECIAL;
    }

    public static boolean modifierIsTmmSpecial(int value) {
        return value == TMM_SPECIAL;
    }

    public boolean hasSpecialValue() {
        for (int value : values) {
            if (value == LR_MOD || value == AUTO_FAIL || value == CMD_SKILL || value == SCR_SPECIAL || value == GRD_SPECIAL || value == HEADHUNT_SPECIAL || value == TMM_SPECIAL) {
                return true;
            }
        }
        return false;
    }

    public int getModifier(Modifier modifier) {
        return values[modifier.ordinal()];
    }

}
