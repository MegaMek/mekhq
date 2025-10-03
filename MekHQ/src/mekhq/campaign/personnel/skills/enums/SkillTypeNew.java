package mekhq.campaign.personnel.skills.enums;

import static mekhq.campaign.personnel.skills.SkillUtilities.DEFAULT_SKILL_COSTS;
import static mekhq.campaign.personnel.skills.SkillUtilities.DISABLED_SKILL_LEVEL;
import static mekhq.campaign.personnel.skills.SkillUtilities.SKILL_LEVEL_ELITE;
import static mekhq.campaign.personnel.skills.SkillUtilities.SKILL_LEVEL_GREEN;
import static mekhq.campaign.personnel.skills.SkillUtilities.SKILL_LEVEL_HEROIC;
import static mekhq.campaign.personnel.skills.SkillUtilities.SKILL_LEVEL_LEGENDARY;
import static mekhq.campaign.personnel.skills.SkillUtilities.SKILL_LEVEL_REGULAR;
import static mekhq.campaign.personnel.skills.SkillUtilities.SKILL_LEVEL_ULTRA_GREEN;
import static mekhq.campaign.personnel.skills.SkillUtilities.SKILL_LEVEL_VETERAN;
import static mekhq.campaign.personnel.skills.enums.SkillAttribute.CHARISMA;
import static mekhq.campaign.personnel.skills.enums.SkillAttribute.DEXTERITY;
import static mekhq.campaign.personnel.skills.enums.SkillAttribute.INTELLIGENCE;
import static mekhq.campaign.personnel.skills.enums.SkillAttribute.NONE;
import static mekhq.campaign.personnel.skills.enums.SkillAttribute.REFLEXES;
import static mekhq.campaign.personnel.skills.enums.SkillAttribute.STRENGTH;
import static mekhq.campaign.personnel.skills.enums.SkillAttribute.WILLPOWER;
import static mekhq.campaign.personnel.skills.enums.SkillSubType.*;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import mekhq.campaign.personnel.skills.SkillUtilities;

public enum SkillTypeNew {
    S_ACROBATICS(
          7,
          ROLEPLAY_GENERAL,
          REFLEXES,
          NONE,
          1,
          2,
          3,
          4,
          6,
          7,
          false,
          DEFAULT_SKILL_COSTS),
    S_ACTING(
          8,
          ROLEPLAY_GENERAL,
          CHARISMA,
          NONE,
          1,
          2,
          3,
          5,
          7,
          8,
          false,
          DEFAULT_SKILL_COSTS),
    S_ADMIN(
          10,
          SUPPORT,
          INTELLIGENCE,
          WILLPOWER,
          1,
          2,
          3,
          5,
          7,
          8,
          true,
          DEFAULT_SKILL_COSTS),
    S_ANIMAL_HANDLING(
          8,
          ROLEPLAY_GENERAL,
          WILLPOWER,
          NONE,
          1,
          2,
          3,
          5,
          7,
          8,
          false,
          DEFAULT_SKILL_COSTS),
    S_ANTI_MEK(
          8,
          COMBAT_PILOTING,
          DEXTERITY,
          NONE,
          2,
          3,
          4,
          5,
          6,
          7,
          false,
          DEFAULT_SKILL_COSTS),
    S_APPRAISAL(
          8,
          ROLEPLAY_GENERAL,
          INTELLIGENCE,
          NONE,
          1,
          2,
          3,
          5,
          7,
          8,
          false,
          DEFAULT_SKILL_COSTS),
    S_ARCHERY(
          7,
          ROLEPLAY_GENERAL,
          DEXTERITY,
          NONE,
          1,
          2,
          3,
          4,
          6,
          7,
          false,
          DEFAULT_SKILL_COSTS),
    S_ARTILLERY(
          7,
          COMBAT_GUNNERY,
          INTELLIGENCE,
          WILLPOWER,
          2,
          4,
          5,
          6,
          7,
          8,
          false,
          DEFAULT_SKILL_COSTS),
    S_ART_COOKING(
          9,
          ROLEPLAY_ART,
          DEXTERITY,
          INTELLIGENCE,
          1,
          3,
          4,
          6,
          8,
          9,
          false,
          DEFAULT_SKILL_COSTS),
    S_ART_DANCING(
          9,
          ROLEPLAY_ART,
          DEXTERITY,
          INTELLIGENCE,
          1,
          3,
          4,
          6,
          8,
          9,
          false,
          DEFAULT_SKILL_COSTS),
    S_ART_DRAWING(
          9,
          ROLEPLAY_ART,
          DEXTERITY,
          INTELLIGENCE,
          1,
          3,
          4,
          6,
          8,
          9,
          false,
          DEFAULT_SKILL_COSTS),
    S_ART_INSTRUMENT(
          9,
          ROLEPLAY_ART,
          DEXTERITY,
          INTELLIGENCE,
          1,
          3,
          4,
          6,
          8,
          9,
          false,
          DEFAULT_SKILL_COSTS),
    S_ART_OTHER(
          9,
          ROLEPLAY_ART,
          DEXTERITY,
          INTELLIGENCE,
          1,
          3,
          4,
          6,
          8,
          9,
          false,
          DEFAULT_SKILL_COSTS),
    S_ART_PAINTING(
          9,
          ROLEPLAY_ART,
          DEXTERITY,
          INTELLIGENCE,
          1,
          3,
          4,
          6,
          8,
          9,
          false,
          DEFAULT_SKILL_COSTS),
    S_ART_POETRY(
          9,
          ROLEPLAY_ART,
          DEXTERITY,
          INTELLIGENCE,
          1,
          3,
          4,
          6,
          8,
          9,
          false,
          DEFAULT_SKILL_COSTS),
    S_ART_SCULPTURE(
          9,
          ROLEPLAY_ART,
          DEXTERITY,
          INTELLIGENCE,
          1,
          3,
          4,
          6,
          8,
          9,
          false,
          DEFAULT_SKILL_COSTS),
    S_ART_SINGING(
          9,
          ROLEPLAY_ART,
          DEXTERITY,
          INTELLIGENCE,
          1,
          3,
          4,
          6,
          8,
          9,
          false,
          DEFAULT_SKILL_COSTS),
    S_ART_WRITING(
          9,
          ROLEPLAY_ART,
          DEXTERITY,
          INTELLIGENCE,
          1,
          3,
          4,
          6,
          8,
          9,
          false,
          DEFAULT_SKILL_COSTS),
    S_ASTECH(
          10,
          SUPPORT,
          INTELLIGENCE,
          NONE,
          1,
          2,
          3,
          4,
          6,
          7,
          true,
          DEFAULT_SKILL_COSTS),
    S_CAREER_ANY(
          7,
          ROLEPLAY_GENERAL,
          INTELLIGENCE,
          NONE,
          1,
          2,
          3,
          4,
          6,
          7,
          false,
          DEFAULT_SKILL_COSTS),
    S_COMMUNICATIONS(
          7,
          ROLEPLAY_GENERAL,
          INTELLIGENCE,
          NONE,
          1,
          2,
          3,
          4,
          6,
          7,
          false,
          DEFAULT_SKILL_COSTS),
    S_COMPUTERS(
          9,
          ROLEPLAY_GENERAL,
          INTELLIGENCE,
          NONE,
          1,
          2,
          3,
          4,
          6,
          7,
          false,
          DEFAULT_SKILL_COSTS),
    S_CRYPTOGRAPHY(
          9,
          ROLEPLAY_GENERAL,
          INTELLIGENCE,
          WILLPOWER,
          1,
          3,
          4,
          6,
          8,
          9,
          false,
          DEFAULT_SKILL_COSTS),
    S_DEMOLITIONS(
          9,
          ROLEPLAY_GENERAL,
          DEXTERITY,
          INTELLIGENCE,
          1,
          3,
          4,
          6,
          8,
          9,
          false,
          DEFAULT_SKILL_COSTS),
    S_DISGUISE(
          7,
          ROLEPLAY_GENERAL,
          CHARISMA,
          NONE,
          1,
          2,
          3,
          4,
          6,
          7,
          false,
          DEFAULT_SKILL_COSTS),
    S_ESCAPE_ARTIST(
          9,
          ROLEPLAY_GENERAL,
          STRENGTH,
          DEXTERITY,
          1,
          3,
          4,
          6,
          8,
          9,
          false,
          DEFAULT_SKILL_COSTS),
    S_FORGERY(
          8,
          ROLEPLAY_GENERAL,
          DEXTERITY,
          INTELLIGENCE,
          1,
          2,
          3,
          5,
          7,
          8,
          false,
          DEFAULT_SKILL_COSTS),
    S_GUN_AERO(
          7,
          COMBAT_GUNNERY,
          REFLEXES,
          DEXTERITY,
          2,
          4,
          5,
          6,
          7,
          8,
          false,
          DEFAULT_SKILL_COSTS),
    S_GUN_BA(
          7,
          COMBAT_GUNNERY,
          REFLEXES,
          DEXTERITY,
          2,
          4,
          5,
          6,
          7,
          8,
          false,
          DEFAULT_SKILL_COSTS),
    S_GUN_JET(
          7,
          COMBAT_GUNNERY,
          REFLEXES,
          DEXTERITY,
          2,
          4,
          5,
          6,
          7,
          8,
          false,
          DEFAULT_SKILL_COSTS),
    S_GUN_MEK(
          7,
          COMBAT_GUNNERY,
          REFLEXES,
          DEXTERITY,
          2,
          4,
          5,
          6,
          7,
          8,
          false,
          DEFAULT_SKILL_COSTS),
    S_GUN_PROTO(
          7,
          COMBAT_GUNNERY,
          REFLEXES,
          DEXTERITY,
          2,
          4,
          5,
          6,
          7,
          8,
          false,
          DEFAULT_SKILL_COSTS),
    S_GUN_VEE(
          7,
          COMBAT_GUNNERY,
          REFLEXES,
          DEXTERITY,
          2,
          4,
          5,
          6,
          7,
          8,
          false,
          DEFAULT_SKILL_COSTS),
    S_GUN_SPACE(
          7,
          COMBAT_GUNNERY,
          REFLEXES,
          DEXTERITY,
          2,
          4,
          5,
          6,
          7,
          8,
          false,
          DEFAULT_SKILL_COSTS),
    S_INTEREST_ANTIQUES(
          9,
          ROLEPLAY_INTEREST,
          INTELLIGENCE,
          WILLPOWER,
          1,
          3,
          4,
          6,
          8,
          9,
          false,
          DEFAULT_SKILL_COSTS),
    S_INTEREST_ASTROLOGY(
          9,
          ROLEPLAY_INTEREST,
          INTELLIGENCE,
          WILLPOWER,
          1,
          3,
          4,
          6,
          8,
          9,
          false,
          DEFAULT_SKILL_COSTS),
    S_INTEREST_ARCHEOLOGY(
          9,
          ROLEPLAY_INTEREST,
          INTELLIGENCE,
          WILLPOWER,
          1,
          3,
          4,
          6,
          8,
          9,
          false,
          DEFAULT_SKILL_COSTS),
    S_INTEREST_CARTOGRAPHY(
          9,
          ROLEPLAY_INTEREST,
          INTELLIGENCE,
          WILLPOWER,
          1,
          3,
          4,
          6,
          8,
          9,
          false,
          DEFAULT_SKILL_COSTS),
    S_INTEREST_ECONOMICS(
          9,
          ROLEPLAY_INTEREST,
          INTELLIGENCE,
          WILLPOWER,
          1,
          3,
          4,
          6,
          8,
          9,
          false,
          DEFAULT_SKILL_COSTS),
    S_INTEREST_EXOTIC_ANIMALS(
          9,
          ROLEPLAY_INTEREST,
          INTELLIGENCE,
          WILLPOWER,
          1,
          3,
          4,
          6,
          8,
          9,
          false,
          DEFAULT_SKILL_COSTS),
    S_INTEREST_FASHION(
          9,
          ROLEPLAY_INTEREST,
          INTELLIGENCE,
          WILLPOWER,
          1,
          3,
          4,
          6,
          8,
          9,
          false,
          DEFAULT_SKILL_COSTS),
    S_INTEREST_FISHING(
          9,
          ROLEPLAY_INTEREST,
          INTELLIGENCE,
          WILLPOWER,
          1,
          3,
          4,
          6,
          8,
          9,
          false,
          DEFAULT_SKILL_COSTS),
    S_INTEREST_GAMBLING(
          9,
          ROLEPLAY_INTEREST,
          INTELLIGENCE,
          WILLPOWER,
          1,
          3,
          4,
          6,
          8,
          9,
          false,
          DEFAULT_SKILL_COSTS),
    S_INTEREST_HISTORY(
          9,
          ROLEPLAY_INTEREST,
          INTELLIGENCE,
          WILLPOWER,
          1,
          3,
          4,
          6,
          8,
          9,
          false,
          DEFAULT_SKILL_COSTS),
    S_INTEREST_HOLO_CINEMA(
          9,
          ROLEPLAY_INTEREST,
          INTELLIGENCE,
          WILLPOWER,
          1,
          3,
          4,
          6,
          8,
          9,
          false,
          DEFAULT_SKILL_COSTS),
    S_INTEREST_HOLO_GAMES(
          9,
          ROLEPLAY_INTEREST,
          INTELLIGENCE,
          WILLPOWER,
          1,
          3,
          4,
          6,
          8,
          9,
          false,
          DEFAULT_SKILL_COSTS),
    S_INTEREST_LAW(
          9,
          ROLEPLAY_INTEREST,
          INTELLIGENCE,
          WILLPOWER,
          1,
          3,
          4,
          6,
          8,
          9,
          false,
          DEFAULT_SKILL_COSTS),
    S_INTEREST_LITERATURE(
          9,
          ROLEPLAY_INTEREST,
          INTELLIGENCE,
          WILLPOWER,
          1,
          3,
          4,
          6,
          8,
          9,
          false,
          DEFAULT_SKILL_COSTS),
    S_INTEREST_MILITARY(
          9,
          ROLEPLAY_INTEREST,
          INTELLIGENCE,
          WILLPOWER,
          1,
          3,
          4,
          6,
          8,
          9,
          false,
          DEFAULT_SKILL_COSTS),
    S_INTEREST_MUSIC(
          9,
          ROLEPLAY_INTEREST,
          INTELLIGENCE,
          WILLPOWER,
          1,
          3,
          4,
          6,
          8,
          9,
          false,
          DEFAULT_SKILL_COSTS),
    S_INTEREST_MYTHOLOGY(
          9,
          ROLEPLAY_INTEREST,
          INTELLIGENCE,
          WILLPOWER,
          1,
          3,
          4,
          6,
          8,
          9,
          false,
          DEFAULT_SKILL_COSTS),
    S_INTEREST_OTHER(
          9,
          ROLEPLAY_INTEREST,
          INTELLIGENCE,
          WILLPOWER,
          1,
          3,
          4,
          6,
          8,
          9,
          false,
          DEFAULT_SKILL_COSTS),
    S_INTEREST_PHILOSOPHY(
          9,
          ROLEPLAY_INTEREST,
          INTELLIGENCE,
          WILLPOWER,
          1,
          3,
          4,
          6,
          8,
          9,
          false,
          DEFAULT_SKILL_COSTS),
    S_INTEREST_POLITICS(
          9,
          ROLEPLAY_INTEREST,
          INTELLIGENCE,
          WILLPOWER,
          1,
          3,
          4,
          6,
          8,
          9,
          false,
          DEFAULT_SKILL_COSTS),
    S_INTEREST_POP_CULTURE(
          9,
          ROLEPLAY_INTEREST,
          INTELLIGENCE,
          WILLPOWER,
          1,
          3,
          4,
          6,
          8,
          9,
          false,
          DEFAULT_SKILL_COSTS),
    S_INTEREST_SPORTS(
          9,
          ROLEPLAY_INTEREST,
          INTELLIGENCE,
          WILLPOWER,
          1,
          3,
          4,
          6,
          8,
          9,
          false,
          DEFAULT_SKILL_COSTS),
    S_INTEREST_THEOLOGY(
          9,
          ROLEPLAY_INTEREST,
          INTELLIGENCE,
          WILLPOWER,
          1,
          3,
          4,
          6,
          8,
          9,
          false,
          DEFAULT_SKILL_COSTS),
    S_INTERROGATION(
          9,
          ROLEPLAY_GENERAL,
          WILLPOWER,
          CHARISMA,
          1,
          3,
          4,
          6,
          8,
          9,
          false,
          DEFAULT_SKILL_COSTS),
    S_INVESTIGATION(
          9,
          ROLEPLAY_GENERAL,
          INTELLIGENCE,
          WILLPOWER,
          1,
          3,
          4,
          6,
          8,
          9,
          false,
          DEFAULT_SKILL_COSTS),
    S_LANGUAGES(
          8,
          ROLEPLAY_GENERAL,
          INTELLIGENCE,
          CHARISMA,
          1,
          2,
          3,
          5,
          7,
          8,
          false,
          DEFAULT_SKILL_COSTS),
    S_LEADER(
          8,
          SUPPORT_COMMAND,
          WILLPOWER,
          CHARISMA,
          1,
          2,
          3,
          5,
          7,
          8,
          true,
          DEFAULT_SKILL_COSTS),
    S_MARTIAL_ARTS(
          8,
          ROLEPLAY_GENERAL,
          REFLEXES,
          DEXTERITY,
          1,
          2,
          3,
          5,
          7,
          8,
          false,
          DEFAULT_SKILL_COSTS),
    S_MEDTECH(
          11,
          SUPPORT,
          INTELLIGENCE,
          NONE,
          1,
          2,
          3,
          4,
          6,
          7,
          true,
          DEFAULT_SKILL_COSTS),
    S_MELEE_WEAPONS(
          8,
          ROLEPLAY_GENERAL,
          REFLEXES,
          DEXTERITY,
          1,
          2,
          3,
          5,
          7,
          8,
          false,
          DEFAULT_SKILL_COSTS),
    S_NAVIGATION(
          8,
          SUPPORT,
          INTELLIGENCE,
          NONE,
          1,
          2,
          3,
          4,
          6,
          7,
          false,
          DEFAULT_SKILL_COSTS),
    S_NEGOTIATION(
          10,
          SUPPORT,
          CHARISMA,
          NONE,
          1,
          2,
          3,
          5,
          7,
          8,
          false,
          DEFAULT_SKILL_COSTS),
    S_PERCEPTION(
          7,
          ROLEPLAY_GENERAL,
          INTELLIGENCE,
          NONE,
          1,
          2,
          3,
          4,
          6,
          7,
          false,
          DEFAULT_SKILL_COSTS),
    S_PILOT_AERO(
          8,
          COMBAT_PILOTING,
          REFLEXES,
          DEXTERITY,
          2,
          3,
          4,
          5,
          6,
          7,
          false,
          DEFAULT_SKILL_COSTS),
    S_PILOT_GVEE(
          8,
          COMBAT_PILOTING,
          REFLEXES,
          DEXTERITY,
          2,
          3,
          4,
          5,
          6,
          7,
          false,
          DEFAULT_SKILL_COSTS),
    S_PILOT_JET(
          8,
          COMBAT_PILOTING,
          REFLEXES,
          DEXTERITY,
          2,
          3,
          4,
          5,
          6,
          7,
          false,
          DEFAULT_SKILL_COSTS),
    S_PILOT_MEK(
          8,
          COMBAT_PILOTING,
          REFLEXES,
          DEXTERITY,
          2,
          3,
          4,
          5,
          6,
          7,
          false,
          DEFAULT_SKILL_COSTS),
    S_PILOT_NVEE(
          8,
          COMBAT_PILOTING,
          REFLEXES,
          DEXTERITY,
          2,
          3,
          4,
          5,
          6,
          7,
          false,
          DEFAULT_SKILL_COSTS),
    S_PILOT_SPACE(
          7,
          COMBAT_GUNNERY,
          REFLEXES,
          DEXTERITY,
          2,
          3,
          4,
          5,
          6,
          7,
          false,
          DEFAULT_SKILL_COSTS),
    S_PILOT_VTOL(
          8,
          COMBAT_PILOTING,
          REFLEXES,
          DEXTERITY,
          2,
          3,
          4,
          5,
          6,
          7,
          false,
          DEFAULT_SKILL_COSTS),
    S_PROTOCOLS(
          8,
          ROLEPLAY_GENERAL,
          WILLPOWER,
          CHARISMA,
          1,
          2,
          3,
          5,
          7,
          8,
          false,
          DEFAULT_SKILL_COSTS),
    S_RUNNING(
          7,
          ROLEPLAY_GENERAL,
          REFLEXES,
          DEXTERITY,
          1,
          2,
          3,
          4,
          6,
          7,
          false,
          DEFAULT_SKILL_COSTS),
    S_SCIENCE_BIOLOGY(
          9,
          ROLEPLAY_SCIENCE,
          INTELLIGENCE,
          WILLPOWER,
          1,
          3,
          4,
          6,
          8,
          9,
          false,
          DEFAULT_SKILL_COSTS),
    S_SCIENCE_GENETICS(
          9,
          ROLEPLAY_SCIENCE,
          INTELLIGENCE,
          WILLPOWER,
          1,
          3,
          4,
          6,
          8,
          9,
          false,
          DEFAULT_SKILL_COSTS),
    S_SCIENCE_CHEMISTRY(
          9,
          ROLEPLAY_SCIENCE,
          INTELLIGENCE,
          WILLPOWER,
          1,
          3,
          4,
          6,
          8,
          9,
          false,
          DEFAULT_SKILL_COSTS),
    S_SCIENCE_GEOLOGY(
          9,
          ROLEPLAY_SCIENCE,
          INTELLIGENCE,
          WILLPOWER,
          1,
          3,
          4,
          6,
          8,
          9,
          false,
          DEFAULT_SKILL_COSTS),
    S_SCIENCE_MATHEMATICS(
          9,
          ROLEPLAY_SCIENCE,
          INTELLIGENCE,
          WILLPOWER,
          1,
          3,
          4,
          6,
          8,
          9,
          false,
          DEFAULT_SKILL_COSTS),
    S_SCIENCE_MILITARY(
          9,
          ROLEPLAY_SCIENCE,
          INTELLIGENCE,
          WILLPOWER,
          1,
          3,
          4,
          6,
          8,
          9,
          false,
          DEFAULT_SKILL_COSTS),
    S_SCIENCE_OTHER(
          9,
          ROLEPLAY_SCIENCE,
          INTELLIGENCE,
          WILLPOWER,
          1,
          3,
          4,
          6,
          8,
          9,
          false,
          DEFAULT_SKILL_COSTS),
    S_SCIENCE_PHARMACOLOGY(
          9,
          ROLEPLAY_SCIENCE,
          INTELLIGENCE,
          WILLPOWER,
          1,
          3,
          4,
          6,
          8,
          9,
          false,
          DEFAULT_SKILL_COSTS),
    S_SCIENCE_PHYSICS(
          9,
          ROLEPLAY_SCIENCE,
          INTELLIGENCE,
          WILLPOWER,
          1,
          3,
          4,
          6,
          8,
          9,
          false,
          DEFAULT_SKILL_COSTS),
    S_SCIENCE_PSYCHOLOGY(
          9,
          ROLEPLAY_SCIENCE,
          INTELLIGENCE,
          WILLPOWER,
          1,
          3,
          4,
          6,
          8,
          9,
          false,
          DEFAULT_SKILL_COSTS),
    S_SCIENCE_XENOBIOLOGY(
          9,
          ROLEPLAY_SCIENCE,
          INTELLIGENCE,
          WILLPOWER,
          1,
          3,
          4,
          6,
          8,
          9,
          false,
          DEFAULT_SKILL_COSTS),
    S_SECURITY_SYSTEMS_ELECTRONIC(
          9,
          ROLEPLAY_SECURITY,
          DEXTERITY,
          INTELLIGENCE,
          1,
          3,
          4,
          6,
          8,
          9,
          false,
          DEFAULT_SKILL_COSTS),
    S_SECURITY_SYSTEMS_MECHANICAL(
          9,
          ROLEPLAY_SECURITY,
          DEXTERITY,
          INTELLIGENCE,
          1,
          3,
          4,
          6,
          8,
          9,
          false,
          DEFAULT_SKILL_COSTS),
    S_SENSOR_OPERATIONS(
          8,
          ROLEPLAY_GENERAL,
          INTELLIGENCE,
          WILLPOWER,
          1,
          2,
          3,
          5,
          7,
          8,
          false,
          DEFAULT_SKILL_COSTS),
    S_SLEIGHT_OF_HAND(
          8,
          ROLEPLAY_GENERAL,
          REFLEXES,
          DEXTERITY,
          1,
          2,
          3,
          5,
          7,
          8,
          false,
          DEFAULT_SKILL_COSTS),
    S_SMALL_ARMS(
          7,
          COMBAT_GUNNERY,
          DEXTERITY,
          NONE,
          2,
          4,
          5,
          6,
          7,
          8,
          false,
          DEFAULT_SKILL_COSTS),
    S_STEALTH(
          8,
          ROLEPLAY_GENERAL,
          REFLEXES,
          INTELLIGENCE,
          1,
          2,
          3,
          5,
          7,
          8,
          false,
          DEFAULT_SKILL_COSTS),
    S_STRATEGY(
          9,
          SUPPORT_COMMAND,
          INTELLIGENCE,
          WILLPOWER,
          1,
          3,
          4,
          6,
          8,
          9,
          true,
          DEFAULT_SKILL_COSTS),
    S_STREETWISE(
          8,
          ROLEPLAY_GENERAL,
          CHARISMA,
          NONE,
          1,
          2,
          3,
          5,
          7,
          8,
          false,
          DEFAULT_SKILL_COSTS),
    S_SUPPORT_WEAPONS(
          7,
          ROLEPLAY_GENERAL,
          DEXTERITY,
          NONE,
          1,
          2,
          3,
          4,
          6,
          7,
          false,
          DEFAULT_SKILL_COSTS),
    S_SURGERY(
          11,
          SUPPORT,
          DEXTERITY,
          INTELLIGENCE,
          1,
          3,
          4,
          6,
          8,
          9,
          false,
          DEFAULT_SKILL_COSTS),
    S_SURVIVAL(
          9,
          ROLEPLAY_GENERAL,
          DEXTERITY,
          INTELLIGENCE,
          1,
          3,
          4,
          6,
          8,
          9,
          false,
          DEFAULT_SKILL_COSTS),
    S_SWIMMING(
          7,
          ROLEPLAY_GENERAL,
          STRENGTH,
          NONE,
          1,
          2,
          3,
          4,
          6,
          7,
          false,
          DEFAULT_SKILL_COSTS),
    S_TACTICS(
          9,
          SUPPORT_COMMAND,
          INTELLIGENCE,
          WILLPOWER,
          1,
          3,
          4,
          6,
          8,
          9,
          true,
          DEFAULT_SKILL_COSTS),
    S_TECH_AERO(
          10,
          SUPPORT,
          DEXTERITY,
          INTELLIGENCE,
          1,
          3,
          4,
          6,
          8,
          9,
          false,
          DEFAULT_SKILL_COSTS),
    S_TECH_BA(
          10,
          SUPPORT,
          DEXTERITY,
          INTELLIGENCE,
          1,
          3,
          4,
          6,
          8,
          9,
          false,
          DEFAULT_SKILL_COSTS),
    S_TECH_MECHANIC(
          10,
          SUPPORT,
          DEXTERITY,
          INTELLIGENCE,
          1,
          3,
          4,
          6,
          8,
          9,
          false,
          DEFAULT_SKILL_COSTS),
    S_TECH_MEK(
          10,
          SUPPORT,
          DEXTERITY,
          INTELLIGENCE,
          1,
          3,
          4,
          6,
          8,
          9,
          false,
          DEFAULT_SKILL_COSTS),
    S_TECH_VESSEL(
          10,
          SUPPORT,
          DEXTERITY,
          INTELLIGENCE,
          1,
          3,
          4,
          6,
          8,
          9,
          false,
          DEFAULT_SKILL_COSTS),
    S_THROWN_WEAPONS(
          7,
          ROLEPLAY_GENERAL,
          DEXTERITY,
          NONE,
          1,
          2,
          3,
          4,
          6,
          7,
          false,
          DEFAULT_SKILL_COSTS),
    S_TRACKING(
          8,
          ROLEPLAY_GENERAL,
          INTELLIGENCE,
          WILLPOWER,
          1,
          2,
          3,
          4,
          6,
          7,
          false,
          DEFAULT_SKILL_COSTS),
    S_TRAINING(
          9,
          SUPPORT,
          INTELLIGENCE,
          CHARISMA,
          1,
          2,
          3,
          4,
          6,
          7,
          false,
          DEFAULT_SKILL_COSTS),
    S_ZERO_G_OPERATIONS(
          7,
          ROLEPLAY_GENERAL,
          REFLEXES,
          NONE,
          1,
          2,
          3,
          4,
          6,
          7,
          false,
          DEFAULT_SKILL_COSTS);

    private static final String RESOURCE_BUNDLE = "mekhq.resources.SkillType";

    private final int baseTargetNumber;
    private final SkillSubType subType;
    private final SkillAttribute firstAttribute;
    private final SkillAttribute secondAttribute;
    private final int skillMilestoneGreen;
    private final int skillMilestoneRegular;
    private final int skillMilestoneVeteran;
    private final int skillMilestoneElite;
    private final int skillMilestoneHeroic;
    private final int skillMilestoneLegendary;
    private final boolean skillLevelsMatter;
    private final int[] costs;

    SkillTypeNew(int baseTargetNumber, SkillSubType subType, SkillAttribute firstAttribute,
          SkillAttribute secondAttribute,
          int skillMilestoneGreen, int skillMilestoneRegular, int skillMilestoneVeteran, int skillMilestoneElite,
          int skillMilestoneHeroic, int skillMilestoneLegendary, boolean skillLevelsMatter, int[] costs) {
        this.baseTargetNumber = baseTargetNumber;
        this.subType = subType;
        this.firstAttribute = firstAttribute;
        this.secondAttribute = secondAttribute;
        this.skillMilestoneGreen = skillMilestoneGreen;
        this.skillMilestoneRegular = skillMilestoneRegular;
        this.skillMilestoneVeteran = skillMilestoneVeteran;
        this.skillMilestoneElite = skillMilestoneElite;
        this.skillMilestoneHeroic = skillMilestoneHeroic;
        this.skillMilestoneLegendary = skillMilestoneLegendary;
        this.skillLevelsMatter = skillLevelsMatter;
        this.costs = costs;
    }

    public int getTarget() {
        return getBaseTargetNumber();
    }

    public int getBaseTargetNumber() {
        return baseTargetNumber;
    }

    public boolean isSubTypeOf(SkillSubType subType) {
        return this.subType == subType;
    }

    public SkillSubType getSubType() {
        return subType;
    }

    public List<SkillAttribute> getAttributes() {
        return Arrays.asList(firstAttribute, secondAttribute);
    }

    /**
     * Determines whether the skill type has the specified attribute.
     *
     * <p>This method checks if the provided {@link SkillAttribute} matches either of the two attributes associated
     * with the skill type.</p>
     *
     * @param attribute the {@link SkillAttribute} to check
     *
     * @return {@code true} if the skill type includes the specified attribute; {@code false} otherwise.
     *
     * @author Illiani
     * @since 0.50.05
     */
    public boolean hasAttribute(SkillAttribute attribute) {
        return (firstAttribute == attribute) || (secondAttribute == attribute);
    }

    /**
     * Calculates the number of linked attributes.
     *
     * <p>This method checks the primary and secondary attributes to determine how many are valid (i.e., not {@code
     * null} and not {@code NONE}). It returns the total count of linked attributes.</p>
     *
     * @return the number of linked attributes, which can be 0, 1, or 2 depending on the validity of the attributes.
     */
    public int getLinkedAttributeCount() {
        int count = 0;
        count += (firstAttribute != null && firstAttribute != NONE) ? 1 : 0;
        count += (secondAttribute != null && secondAttribute != NONE) ? 1 : 0;
        return count;
    }

    public SkillAttribute getFirstAttribute() {
        return firstAttribute;
    }

    public SkillAttribute getSecondAttribute() {
        return secondAttribute;
    }

    public int getSkillMilestoneGreen() {
        return skillMilestoneGreen;
    }

    public int getSkillMilestoneRegular() {
        return skillMilestoneRegular;
    }

    public int getSkillMilestoneVeteran() {
        return skillMilestoneVeteran;
    }

    public int getSkillMilestoneElite() {
        return skillMilestoneElite;
    }

    public int getSkillMilestoneHeroic() {
        return skillMilestoneHeroic;
    }

    public int getSkillMilestoneLegendary() {
        return skillMilestoneLegendary;
    }

    public boolean isSkillLevelsMatter() {
        return skillLevelsMatter;
    }

    public boolean isRoleplaySkill() {
        return getRoleplaySkillSubTypes().contains(subType);
    }

    /**
     * Returns the localized display name for this skill type.
     *
     * <p>Retrieves the name from the resource bundle. If the skill is a roleplay skill, appends a marker indicating
     * that it is roleplay-only.</p>
     *
     * @return the localized name of this skill type, with a roleplay-only marker if applicable
     */
    public String getName() {
        String name = getTextAt(RESOURCE_BUNDLE, "SkillTypeNew." + this.name() + ".name");

        if (isRoleplaySkill()) {
            name += ' ' + getTextAt(RESOURCE_BUNDLE, "SkillType.rpOnly");
        }

        return name;
    }

    /**
     * Retrieves the flavor text for this skill, optionally including HTML tags and attribute details.
     *
     * @param includeHtmlTags   if {@code true}, the returned string will be wrapped with {@code <html>} and
     *                          {@code </html>} tags
     * @param includeAttributes if {@code true}, the returned string will append the object's attributes as labels; if
     *                          {@code false}, only the raw flavor text is returned
     *
     * @return the assembled flavor text, with optional HTML formatting and attribute information
     *
     * @author Illiani
     * @since 0.50.06
     */
    public String getDescription(boolean includeHtmlTags, boolean includeAttributes) {
        String rawFlavorText = getTextAt(RESOURCE_BUNDLE, "SkillTypeNew." + this.name() + ".flavorText");

        String htmlOpenTag = includeHtmlTags ? "<html>" : "";
        String htmlCloseTag = includeHtmlTags ? "</html>" : "";

        if (!includeAttributes) {
            return htmlOpenTag + rawFlavorText + htmlCloseTag;
        }

        String flavorText = htmlOpenTag + rawFlavorText + "<br>(" + firstAttribute.getLabel();

        if (secondAttribute != NONE) {
            flavorText += ", " + secondAttribute.getLabel() + ')';
        } else {
            flavorText += ")";
        }

        flavorText += htmlCloseTag;

        return flavorText;
    }

    /**
     * Retrieves a list of unique {@link SkillTypeNew} instances that match any of the specified {@link SkillSubType}s.
     *
     * <p>This method iterates through all available {@link SkillTypeNew} values and adds those whose subtype is
     * included in the provided list of {@code skillSubTypes} to the result list. Each {@code SkillTypeNew} will appear
     * only once in the resulting list, even if multiple subtypes share the same name.</p>
     *
     * @param skillSubTypes a list of {@link SkillSubType}s for which to find matching skill types
     *
     * @return a list of unique {@link SkillTypeNew} instances that belong to one of the specified skill subtypes
     *
     * @author Illiani
     * @since 0.50.06
     */
    public static List<SkillTypeNew> getSkillsBySkillSubType(List<SkillSubType> skillSubTypes) {
        List<SkillTypeNew> relevantSkills = new ArrayList<>();

        for (SkillTypeNew skillType : SkillTypeNew.values()) {
            if (skillSubTypes.contains(skillType.getSubType())) {
                relevantSkills.add(skillType);
            }
        }

        return relevantSkills;
    }

    public int getLevelFromExperience(int expLvl) {
        return switch (expLvl) {
            case SKILL_LEVEL_GREEN -> skillMilestoneGreen;
            case SKILL_LEVEL_REGULAR -> skillMilestoneRegular;
            case SKILL_LEVEL_VETERAN -> skillMilestoneVeteran;
            case SKILL_LEVEL_ELITE -> skillMilestoneElite;
            case SKILL_LEVEL_HEROIC -> skillMilestoneHeroic;
            case SKILL_LEVEL_LEGENDARY -> skillMilestoneLegendary;
            default ->
                // for ultra-green we take the midpoint between green and 0, rounding down.
                // If the user has set green as zero, then this will be the same
                  (int) Math.floor(skillMilestoneGreen / 2.0);
        };
    }

    /**
     * Returns the experience level constant corresponding to the given numeric level.
     *
     * <p>The method compares the input level against predetermined thresholds for each experience rank, in
     * descending order: legendary, heroic, elite, veteran, regular, and green. It returns the constant representing the
     * matching or next lower experience category. If the input level does not meet any of these thresholds, it returns
     * the constant for the "ultra green" experience level.</p>
     *
     * @param level the numeric level to evaluate
     *
     * @return the constant representing the corresponding experience level
     */
    public int getExperienceLevel(final int level) {
        if (level >= skillMilestoneLegendary) {
            return SKILL_LEVEL_LEGENDARY;
        } else if (level >= skillMilestoneHeroic) {
            return SKILL_LEVEL_HEROIC;
        } else if (level >= skillMilestoneElite) {
            return SKILL_LEVEL_ELITE;
        } else if (level >= skillMilestoneVeteran) {
            return SKILL_LEVEL_VETERAN;
        } else if (level >= skillMilestoneRegular) {
            return SKILL_LEVEL_REGULAR;
        } else if (level >= skillMilestoneGreen) {
            return SKILL_LEVEL_GREEN;
        } else {
            return SKILL_LEVEL_ULTRA_GREEN;
        }
    }

    /**
     * Returns the cost associated with the specified skill level.
     *
     * <p>If the provided level is outside the valid range (less than 0 or greater than 10),
     * {@link SkillUtilities#DISABLED_SKILL_LEVEL} is returned. Otherwise, returns the cost for the specified
     * level.</p>
     *
     * @param level the skill level for which to retrieve the cost
     *
     * @return the cost for the given skill level, or {@link SkillUtilities#DISABLED_SKILL_LEVEL} if the level is
     *       invalid
     */
    public int getCost(int level) {
        if ((level > 10) || (level < 0)) {
            return DISABLED_SKILL_LEVEL;
        } else {
            return costs[level];
        }
    }

    /**
     * Returns the array of costs for all skill levels.
     *
     * @return an array of integers representing the cost at each skill level
     */
    public int[] getCosts() {
        return costs;
    }

    /**
     * Calculates the total cumulative cost up to and including the specified skill level.
     *
     * @param level the skill level up to which to sum the costs
     *
     * @return the total cost from level 0 to the specified level, inclusive
     */
    public int getTotalCost(int level) {
        int totalCost = 0;
        for (int i = 0; i <= level; i++) {
            totalCost = totalCost + costs[i];
        }
        return totalCost;
    }

    /**
     * Determines the maximum enabled skill level for this skill type.
     *
     * <p>The maximum level is defined as the highest index in the {@code costs} array that does not have the value
     * {@link SkillUtilities#DISABLED_SKILL_LEVEL}.</p>
     *
     * @return the maximum enabled skill level, or one less than the index of the first disabled level
     */
    public int getMaxLevel() {
        for (int lvl = 0; lvl < costs.length; ++lvl) {
            if (costs[lvl] == DISABLED_SKILL_LEVEL) {
                return lvl - 1;
            }
        }
        return costs.length - 1;
    }

    /**
     * Sets the cost for a given skill type, skill level, and value.
     *
     * <p>The method updates the cost for the specified skill type and level, provided the skill name is valid and the
     * level is less than 11.</p>
     *
     * @param name  the name of the skill type to update
     * @param cost  the new cost to set
     * @param level the skill level at which to set the new cost
     */
    public static void setCost(String name, int cost, int level) {
        SkillTypeNew type = SkillTypeNew.valueOf(name);
        if ((name != null) && (level < 11)) {
            type.costs[level] = cost;
        }
    }

    public static SkillTypeNew getType(String name) {
        return SkillTypeNew.valueOf(name);
    }

    /**
     * Checks if the current instance is affected by the "Gremlins" or "Tech Empathy" SPAs.
     *
     * @return {@code true} if the {@code name} field matches one of the known tech or electronic-related skills,
     *       {@code false} otherwise.
     *
     * @author Illiani
     * @since 0.50.05
     */
    public boolean isAffectedByGremlinsOrTechEmpathy() {
        return Objects.equals(this, S_TECH_BA) ||
                     Objects.equals(this, S_TECH_AERO) ||
                     Objects.equals(this, S_TECH_MECHANIC) ||
                     Objects.equals(this, S_TECH_MEK) ||
                     Objects.equals(this, S_TECH_VESSEL) ||
                     Objects.equals(this, S_COMPUTERS) ||
                     Objects.equals(this, S_COMMUNICATIONS) ||
                     Objects.equals(this, S_SECURITY_SYSTEMS_ELECTRONIC);
    }

    /**
     * Checks if the current instance is affected by the "Impatient" or "Patient" SPAs.
     *
     * @return {@code true} if the instance is related to one of the affected subtypes or names, {@code false}
     *       otherwise.
     *
     * @author Illiani
     * @since 0.50.05
     */
    public boolean isAffectedByImpatientOrPatient() {
        return this.isSubTypeOf(ROLEPLAY_ART) ||
                     this.isSubTypeOf(ROLEPLAY_SECURITY) ||
                     Objects.equals(this, S_CRYPTOGRAPHY) ||
                     Objects.equals(this, S_DEMOLITIONS) ||
                     Objects.equals(this, S_INVESTIGATION) ||
                     Objects.equals(this, S_PROTOCOLS) ||
                     Objects.equals(this, S_STRATEGY) ||
                     Objects.equals(this, S_TACTICS) ||
                     Objects.equals(this, S_TRAINING);
    }
}
