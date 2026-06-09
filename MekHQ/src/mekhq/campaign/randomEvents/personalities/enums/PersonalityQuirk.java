/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package mekhq.campaign.randomEvents.personalities.enums;

import static mekhq.campaign.randomEvents.personalities.enums.PersonalityTraitType.PERSONALITY_QUIRK;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import megamek.logging.MMLogger;

/**
 * Represents various personality quirks that can define an individual's behavior or habits.
 *
 * <p>
 * This enumeration describes a wide array of personality quirks that may manifest in characters, ranging from minor
 * habits like "FIDGETS" or "CLEANER" to more distinct and situational traits like "DRAMATIC" or
 * "BATTLEFIELD_NOSTALGIA." These quirks provide depth and individuality to characters in campaigns, aiding storytelling
 * and immersion in the MekHQ ecosystem.
 * </p>
 *
 * <p>
 * Each personality quirk is paired with localized labels and descriptions using resource bundles, enabling custom,
 * gender-specific, and role-based descriptions such as combat or support roles. Additionally, quirks can integrate with
 * broader faction-based labels for more tailored storytelling.
 * </p>
 */
public enum PersonalityQuirk {
    // region Enum Declarations
    NONE,
    ADJUSTS_CLOTHES,
    AFFECTIONATE,
    APOLOGETIC,
    BOOKWORM,
    CALENDAR,
    CANDLES,
    CHEWING_GUM,
    CHRONIC_LATENESS,
    CLEANER,
    COLLECTOR,
    COMPETITIVE_NATURE,
    COMPLIMENTS,
    DAYDREAMER,
    DOODLER,
    DOOLITTLE,
    DRAMATIC,
    EATING_HABITS,
    ENVIRONMENTAL_SENSITIVITY,
    EXCESSIVE_CAUTION,
    EXCESSIVE_GREETING,
    EYE_CONTACT,
    FASHION_CHOICES,
    FIDGETS,
    FITNESS,
    FIXATES,
    FLASK,
    FOOT_TAPPER,
    FORGETFUL,
    FORMAL_SPEECH,
    FURNITURE,
    GLASSES,
    GLOVES,
    HAND_GESTURES,
    HAND_WRINGER,
    HANDSHAKE,
    HEADPHONES,
    HEALTHY_SNACKS,
    HISTORIAN,
    HUMMER,
    HYGIENIC,
    IRREGULAR_SLEEPER,
    JOKER,
    LISTS,
    LITERAL,
    LOCKS,
    MEASURED_TALKER,
    MINIMALIST,
    MUG,
    NAIL_BITER,
    NICKNAMING,
    NIGHT_OWL,
    NOTE_TAKER,
    NOTEBOOK,
    OBJECT,
    ORGANIZATIONAL_TENDENCIES,
    ORGANIZER,
    ORIGAMI,
    OVER_PLANNER,
    OVEREXPLAINER,
    PEN_CLICKER,
    PEN_TWIRLER,
    PERSONIFICATION,
    PESSIMIST,
    PHRASES,
    PLANTS,
    POLITE,
    PRACTICAL_JOKER,
    PREPARED,
    PUNCTUAL,
    PUZZLES,
    QUOTES,
    RARELY_SLEEPS,
    ROUTINE,
    SEEKS_APPROVAL,
    SENTIMENTAL,
    SHARPENING,
    SINGS,
    SKEPTICAL,
    SLEEP_TALKER,
    SMILER,
    SNACKS,
    STORYTELLING,
    STRETCHING,
    SUPERSTITIOUS_RITUALS,
    SUPERVISED_HABITS,
    TECH_TALK,
    TECHNOPHOBIA,
    THESAURUS,
    THIRD_PERSON,
    TIME_MANAGEMENT,
    TINKERER,
    TRUTH_TELLER,
    UNNECESSARY_CAUTION,
    UNPREDICTABLE_SPEECH,
    UNUSUAL_HOBBIES,
    WATCH,
    WEATHERMAN,
    WHISTLER,
    WORRIER,
    WRITER,
    BATTLEFIELD_NOSTALGIA,
    HEAVY_HANDED,
    RATION_HOARDER,
    EMERGENCY_MANUAL_READER,
    QUICK_TO_QUIP,
    TECH_SKEPTIC,
    POST_BATTLE_RITUALS,
    OVER_COMMUNICATOR,
    FIELD_MEDIC,
    SYSTEM_CALIBRATOR,
    AMMO_COUNTER,
    BRAVADO,
    COMBAT_SONG,
    COMMS_TOGGLE,
    EJECTION_READY,
    HAND_SIGNS,
    HATE_FOR_MEKS,
    IMPROVISED_WEAPONRY,
    PRE_BATTLE_SUPERSTITIONS,
    SILENT_LEADER,
    BATTLE_CRITIC,
    CHECKS_WEAPON_SAFETY,
    CLOSE_COMBAT_PREF,
    COMBAT_POET,
    CUSTOM_DECALS,
    DISPLAYS_TROPHIES,
    DO_IT_YOURSELF,
    FIELD_IMPROVISER,
    LOUD_COMMS,
    WAR_STORIES,
    ALL_OR_NOTHING,
    BOOTS_ON_THE_GROUND,
    BRAVERY_BOASTER,
    COCKPIT_DRIFTER,
    CONSPIRACY_THEORIST,
    DEVOUT_WARRIOR,
    DUAL_WIELDING,
    EMBLEM_LOVER,
    EXCESSIVE_DEBRIEFING,
    EYE_FOR_ART,
    FAST_TALKER,
    FINGER_GUNS,
    FLARE_DEPLOYER,
    FRIENDLY_INTERROGATOR,
    GUN_NUT,
    LAST_MAN_STANDING,
    LEGENDARY_MEK,
    PASSIVE_LEADER,
    REBEL_WITHOUT_CAUSE,
    SIMPLE_LIFE,
    ANTI_AUTHORITY,
    BLOODLUST,
    BRAVERY_IN_DOUBT,
    CLOSE_QUARTERS_ONLY,
    COOL_UNDER_FIRE,
    CRASH_TEST,
    DEAD_PAN_HUMOR,
    DRILLS,
    ENEMY_RESPECT,
    EXTREME_MORNING_PERSON,
    GALLANT,
    IRON_STOMACH,
    MISSION_CRITIC,
    NO_PAIN_NO_GAIN,
    PERSONAL_ARMORY,
    QUICK_ADAPTER,
    RETALIATOR,
    RUSH_HOUR,
    SILENT_PROTECTOR,
    ALWAYS_TACTICAL,
    BATTLE_SCREAM,
    BRIEF_AND_TO_THE_POINT,
    CALLSIGN_COLLECTOR,
    CHATTERBOX,
    COMBAT_ARTIST,
    DARING_ESCAPE,
    DOOMSDAY_PREPPER,
    EQUIPMENT_SCAVENGER,
    FRIEND_TO_FOES,
    GUNG_HO,
    INSPIRATIONAL_POET,
    MEK_MATCHMAKER,
    MISSILE_JUNKIE,
    NEVER_RETREAT,
    OPTIMISTIC_TO_A_FAULT,
    REACTIVE,
    RISK_TAKER,
    SIGNATURE_MOVE,
    TACTICAL_WITHDRAWAL,
    ACCENT_SWITCHER,
    AMBUSH_LOVER,
    BATTLE_HARDENED,
    BREAKS_RADIO_SILENCE,
    CONVOY_LOVER,
    CAMOUFLAGE,
    DISTANT_LEADER,
    DRAMATIC_FINISH,
    ENGINE_REVERER,
    FLIRTY_COMMS,
    FOCUS_FREAK,
    FOUL_MOUTHED,
    FREESTYLE_COMBAT,
    GEOMETRY_GURU,
    ICE_COLD,
    PICKY_ABOUT_GEAR,
    RECORD_KEEPER,
    RESOURCE_SCROUNGER,
    TRASH_TALKER,
    CORRECTS_PRONOUNS,
    BODY_DISCOMFORT,
    MEK_COMFORT,
    FEAR_OF_FIRE,
    TOUCH_SENSITIVE,
    HOLOVID_FANATIC,
    INVINCIBLE,
    STUTTERS,
    ACROPHOBIA,
    CLAUSTROPHOBIA,
    AGORAPHOBIA,
    NYCTOPHOBIA,
    MONOPHOBIA,
    HEMOPHOBIA,
    ZERO_G_PARALYSIS,
    HEDONIST,
    BROADCASTS_MUSIC,
    NEMESIS,
    FEAR_MEKS,
    LOCAL_CONNECTOR,
    HATRED,
    PANTSLESS,
    MAKES_CLOTHES,
    ACTS_SUSPICIOUSLY,
    NIGHTMARES,
    FREEZES,
    TRAUMATIZED,
    HAUNTED,
    BROKEN,
    REGRETS,
    DARK_SECRET,
    ANTHROPOMORPHIC,
    CLUMSY,
    JUSTIFIES,
    ENJOYS_DRAMA;
    // endregion Enum Declarations

    final private String RESOURCE_BUNDLE = "mekhq.resources." + getClass().getSimpleName();

    private final String label;

    PersonalityQuirk() {
        this.label = this.generateLabel();
    }

    public String getLabel() {
        return label;
    }

    /**
     * Defines the number of individual description variants available for each trait.
     *
     * <p>Note that this should be equal to the number of description variants for each type of
     * role (combat or support) and not both combined. i.e., if there are three variants for Combat and three for
     * Support, this should equal 3 and not 6.</p>
     */
    public final static int MAXIMUM_VARIATIONS = 3;

    /**
     * @return the {@link PersonalityTraitType} representing personality quirks
     *
     * @author Illiani
     * @since 0.50.06
     */
    public PersonalityTraitType getPersonalityTraitType() {
        return PERSONALITY_QUIRK;
    }

    /**
     * @return the label string for the personality quirk trait type
     *
     * @author Illiani
     * @since 0.50.06
     */
    public String getPersonalityTraitTypeLabel() {
        return getPersonalityTraitType().getLabel();
    }

    /**
     * Retrieves the label associated with the current enumeration value.
     *
     * <p>The label is determined based on the resource bundle for the application,
     * utilizing the enum name combined with a specific key suffix to fetch the relevant localized string.</p>
     *
     * @return the localized label string corresponding to the enumeration value.
     */
    // region Getters
    private String generateLabel() {
        final String RESOURCE_KEY = name() + ".label";

        return getTextAt(RESOURCE_BUNDLE, RESOURCE_KEY);
    }

    /**
     * Returns a list of Personality Quirks sorted alphabetically by their label.
     *
     * <p>{@link #NONE} is always the first element in the list.</p>
     *
     * @return a sorted {@link List} of {@link PersonalityQuirk} objects by their label.
     */
    public static List<PersonalityQuirk> personalityQuirksSortedAlphabetically() {
        List<PersonalityQuirk> quirks = new ArrayList<>();
        for (PersonalityQuirk quirk : PersonalityQuirk.values()) {
            if (quirk != PersonalityQuirk.NONE) {
                quirks.add(quirk);
            }
        }
        quirks.sort(Comparator.comparing(PersonalityQuirk::getLabel));
        quirks.addFirst(PersonalityQuirk.NONE);
        return quirks;
    }
    // endregion Getters

    // region Boolean Comparison Methods
    public boolean isNone() {
        return this == NONE;
    }
    // endregion Boolean Comparison Methods

    // region File I/O

    /**
     * Converts the given string into an instance of the {@code PersonalityQuirk} enum. The method tries to interpret
     * the string as both a name of an enumeration constant and as an ordinal index. If neither interpretation succeeds,
     * it logs an error and returns {@code NONE}.
     *
     * @param text the string representation of the quirk; can be either the name of an enumeration constant or the
     *             ordinal string.
     *
     * @return the corresponding {@code PersonalityQuirk} enum instance if the string is a valid name or ordinal;
     *       otherwise, returns {@code NONE}.
     */
    public static PersonalityQuirk fromString(String text) {
        try {
            return PersonalityQuirk.valueOf(text);
        } catch (Exception ignored) {}

        try {
            return PersonalityQuirk.values()[Integer.parseInt(text)];
        } catch (Exception ignored) {}


        MMLogger logger = MMLogger.create(PersonalityQuirk.class);
        logger.error("Unknown PersonalityQuirk ordinal: {} - returning {}.", text, NONE);

        return NONE;
    }

    @Override
    public String toString() {
        return getLabel();
    }
}
