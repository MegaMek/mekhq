/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.personnel.randomEvents.enums.personalities;

import megamek.logging.MMLogger;
import mekhq.MekHQ;

import java.util.ResourceBundle;

public enum PersonalityQuirk {
    // region Enum Declarations
    NONE("Personality.NONE.text", "Personality.NONE.description"),
    ADJUSTS_CLOTHES("PersonalityQuirk.ADJUSTS_CLOTHES.text", "PersonalityQuirk.ADJUSTS_CLOTHES.description"),
    AFFECTIONATE("PersonalityQuirk.AFFECTIONATE.text", "PersonalityQuirk.AFFECTIONATE.description"),
    APOLOGETIC("PersonalityQuirk.APOLOGETIC.text", "PersonalityQuirk.APOLOGETIC.description"),
    BOOKWORM("PersonalityQuirk.BOOKWORM.text", "PersonalityQuirk.BOOKWORM.description"),
    CALENDAR("PersonalityQuirk.CALENDAR.text", "PersonalityQuirk.CALENDAR.description"),
    CANDLES("PersonalityQuirk.CANDLES.text", "PersonalityQuirk.CANDLES.description"),
    CHEWING_GUM("PersonalityQuirk.CHEWING_GUM.text", "PersonalityQuirk.CHEWING_GUM.description"),
    CHRONIC_LATENESS("PersonalityQuirk.CHRONIC_LATENESS.text", "PersonalityQuirk.CHRONIC_LATENESS.description"),
    CLEANER("PersonalityQuirk.CLEANER.text", "PersonalityQuirk.CLEANER.description"),
    COLLECTOR("PersonalityQuirk.COLLECTOR.text", "PersonalityQuirk.COLLECTOR.description"),
    COMPETITIVE_NATURE("PersonalityQuirk.COMPETITIVE_NATURE.text", "PersonalityQuirk.COMPETITIVE_NATURE.description"),
    COMPLIMENTS("PersonalityQuirk.COMPLIMENTS.text", "PersonalityQuirk.COMPLIMENTS.description"),
    DAYDREAMER("PersonalityQuirk.DAYDREAMER.text", "PersonalityQuirk.DAYDREAMER.description"),
    DOODLER("PersonalityQuirk.DOODLER.text", "PersonalityQuirk.DOODLER.description"),
    DOOLITTLE("PersonalityQuirk.DOOLITTLE.text", "PersonalityQuirk.DOOLITTLE.description"),
    DRAMATIC("PersonalityQuirk.DRAMATIC.text", "PersonalityQuirk.DRAMATIC.description"),
    EATING_HABITS("PersonalityQuirk.EATING_HABITS.text", "PersonalityQuirk.EATING_HABITS.description"),
    ENVIRONMENTAL_SENSITIVITY("PersonalityQuirk.ENVIRONMENTAL_SENSITIVITY.text",
            "PersonalityQuirk.ENVIRONMENTAL_SENSITIVITY.description"),
    EXCESSIVE_CAUTION("PersonalityQuirk.EXCESSIVE_CAUTION.text", "PersonalityQuirk.EXCESSIVE_CAUTION.description"),
    EXCESSIVE_GREETING("PersonalityQuirk.EXCESSIVE_GREETING.text", "PersonalityQuirk.EXCESSIVE_GREETING.description"),
    EYE_CONTACT("PersonalityQuirk.EYE_CONTACT.text", "PersonalityQuirk.EYE_CONTACT.description"),
    FASHION_CHOICES("PersonalityQuirk.FASHION_CHOICES.text", "PersonalityQuirk.FASHION_CHOICES.description"),
    FIDGETS("PersonalityQuirk.FIDGETS.text", "PersonalityQuirk.FIDGETS.description"),
    FITNESS("PersonalityQuirk.FITNESS.text", "PersonalityQuirk.FITNESS.description"),
    FIXATES("PersonalityQuirk.FIXATES.text", "PersonalityQuirk.FIXATES.description"),
    FLASK("PersonalityQuirk.FLASK.text", "PersonalityQuirk.FLASK.description"),
    FOOT_TAPPER("PersonalityQuirk.FOOT_TAPPER.text", "PersonalityQuirk.FOOT_TAPPER.description"),
    FORGETFUL("PersonalityQuirk.FORGETFUL.text", "PersonalityQuirk.FORGETFUL.description"),
    FORMAL_SPEECH("PersonalityQuirk.FORMAL_SPEECH.text", "PersonalityQuirk.FORMAL_SPEECH.description"),
    FURNITURE("PersonalityQuirk.FURNITURE.text", "PersonalityQuirk.FURNITURE.description"),
    GLASSES("PersonalityQuirk.GLASSES.text", "PersonalityQuirk.GLASSES.description"),
    GLOVES("PersonalityQuirk.GLOVES.text", "PersonalityQuirk.GLOVES.description"),
    HAND_GESTURES("PersonalityQuirk.HAND_GESTURES.text", "PersonalityQuirk.HAND_GESTURES.description"),
    HAND_WRINGER("PersonalityQuirk.HAND_WRINGER.text", "PersonalityQuirk.HAND_WRINGER.description"),
    HANDSHAKE("PersonalityQuirk.HANDSHAKE.text", "PersonalityQuirk.HANDSHAKE.description"),
    HEADPHONES("PersonalityQuirk.HEADPHONES.text", "PersonalityQuirk.HEADPHONES.description"),
    HEALTHY_SNACKS("PersonalityQuirk.HEALTHY_SNACKS.text", "PersonalityQuirk.HEALTHY_SNACKS.description"),
    HISTORIAN("PersonalityQuirk.HISTORIAN.text", "PersonalityQuirk.HISTORIAN.description"),
    HUMMER("PersonalityQuirk.HUMMER.text", "PersonalityQuirk.HUMMER.description"),
    HYGIENIC("PersonalityQuirk.HYGIENIC.text", "PersonalityQuirk.HYGIENIC.description"),
    IRREGULAR_SLEEPER("PersonalityQuirk.IRREGULAR_SLEEPER.text", "PersonalityQuirk.IRREGULAR_SLEEPER.description"),
    JOKER("PersonalityQuirk.JOKER.text", "PersonalityQuirk.JOKER.description"),
    LISTS("PersonalityQuirk.LISTS.text", "PersonalityQuirk.LISTS.description"),
    LITERAL("PersonalityQuirk.LITERAL.text", "PersonalityQuirk.LITERAL.description"),
    LOCKS("PersonalityQuirk.LOCKS.text", "PersonalityQuirk.LOCKS.description"),
    MEASURED_TALKER("PersonalityQuirk.MEASURED_TALKER.text", "PersonalityQuirk.MEASURED_TALKER.description"),
    MINIMALIST("PersonalityQuirk.MINIMALIST.text", "PersonalityQuirk.MINIMALIST.description"),
    MUG("PersonalityQuirk.MUG.text", "PersonalityQuirk.MUG.description"),
    NAIL_BITER("PersonalityQuirk.NAIL_BITER.text", "PersonalityQuirk.NAIL_BITER.description"),
    NICKNAMING("PersonalityQuirk.NICKNAMING.text", "PersonalityQuirk.NICKNAMING.description"),
    NIGHT_OWL("PersonalityQuirk.NIGHT_OWL.text", "PersonalityQuirk.NIGHT_OWL.description"),
    NOTE_TAKER("PersonalityQuirk.NOTE_TAKER.text", "PersonalityQuirk.NOTE_TAKER.description"),
    NOTEBOOK("PersonalityQuirk.NOTEBOOK.text", "PersonalityQuirk.NOTEBOOK.description"),
    OBJECT("PersonalityQuirk.OBJECT.text", "PersonalityQuirk.OBJECT.description"),
    ORGANIZATIONAL_TENDENCIES("PersonalityQuirk.ORGANIZATIONAL_TENDENCIES.text",
            "PersonalityQuirk.ORGANIZATIONAL_TENDENCIES.description"),
    ORGANIZER("PersonalityQuirk.ORGANIZER.text", "PersonalityQuirk.ORGANIZER.description"),
    ORIGAMI("PersonalityQuirk.ORIGAMI.text", "PersonalityQuirk.ORIGAMI.description"),
    OVER_PLANNER("PersonalityQuirk.OVER_PLANNER.text", "PersonalityQuirk.OVER_PLANNER.description"),
    OVEREXPLAINER("PersonalityQuirk.OVEREXPLAINER.text", "PersonalityQuirk.OVEREXPLAINER.description"),
    PEN_CLICKER("PersonalityQuirk.PEN_CLICKER.text", "PersonalityQuirk.PEN_CLICKER.description"),
    PEN_TWIRLER("PersonalityQuirk.PEN_TWIRLER.text", "PersonalityQuirk.PEN_TWIRLER.description"),
    PERSONIFICATION("PersonalityQuirk.PERSONIFICATION.text", "PersonalityQuirk.PERSONIFICATION.description"),
    PESSIMIST("PersonalityQuirk.PESSIMIST.text", "PersonalityQuirk.PESSIMIST.description"),
    PHRASES("PersonalityQuirk.PHRASES.text", "PersonalityQuirk.PHRASES.description"),
    PLANTS("PersonalityQuirk.PLANTS.text", "PersonalityQuirk.PLANTS.description"),
    POLITE("PersonalityQuirk.POLITE.text", "PersonalityQuirk.POLITE.description"),
    PRACTICAL_JOKER("PersonalityQuirk.PRACTICAL_JOKER.text", "PersonalityQuirk.PRACTICAL_JOKER.description"),
    PREPARED("PersonalityQuirk.PREPARED.text", "PersonalityQuirk.PREPARED.description"),
    PUNCTUAL("PersonalityQuirk.PUNCTUAL.text", "PersonalityQuirk.PUNCTUAL.description"),
    PUZZLES("PersonalityQuirk.PUZZLES.text", "PersonalityQuirk.PUZZLES.description"),
    QUOTES("PersonalityQuirk.QUOTES.text", "PersonalityQuirk.QUOTES.description"),
    RARELY_SLEEPS("PersonalityQuirk.RARELY_SLEEPS.text", "PersonalityQuirk.RARELY_SLEEPS.description"),
    ROUTINE("PersonalityQuirk.ROUTINE.text", "PersonalityQuirk.ROUTINE.description"),
    SEEKS_APPROVAL("PersonalityQuirk.SEEKS_APPROVAL.text", "PersonalityQuirk.SEEKS_APPROVAL.description"),
    SENTIMENTAL("PersonalityQuirk.SENTIMENTAL.text", "PersonalityQuirk.SENTIMENTAL.description"),
    SHARPENING("PersonalityQuirk.SHARPENING.text", "PersonalityQuirk.SHARPENING.description"),
    SINGS("PersonalityQuirk.SINGS.text", "PersonalityQuirk.SINGS.description"),
    SKEPTICAL("PersonalityQuirk.SKEPTICAL.text", "PersonalityQuirk.SKEPTICAL.description"),
    SLEEP_TALKER("PersonalityQuirk.SLEEP_TALKER.text", "PersonalityQuirk.SLEEP_TALKER.description"),
    SMILER("PersonalityQuirk.SMILER.text", "PersonalityQuirk.SMILER.description"),
    SNACKS("PersonalityQuirk.SNACKS.text", "PersonalityQuirk.SNACKS.description"),
    STORYTELLING("PersonalityQuirk.STORYTELLING.text", "PersonalityQuirk.STORYTELLING.description"),
    STRETCHING("PersonalityQuirk.STRETCHING.text", "PersonalityQuirk.STRETCHING.description"),
    SUPERSTITIOUS_RITUALS("PersonalityQuirk.SUPERSTITIOUS_RITUALS.text",
            "PersonalityQuirk.SUPERSTITIOUS_RITUALS.description"),
    SUPERVISED_HABITS("PersonalityQuirk.SUPERVISED_HABITS.text", "PersonalityQuirk.SUPERVISED_HABITS.description"),
    TECH_TALK("PersonalityQuirk.TECH_TALK.text", "PersonalityQuirk.TECH_TALK.description"),
    TECHNOPHOBIA("PersonalityQuirk.TECHNOPHOBIA.text", "PersonalityQuirk.TECHNOPHOBIA.description"),
    THESAURUS("PersonalityQuirk.THESAURUS.text", "PersonalityQuirk.THESAURUS.description"),
    THIRD_PERSON("PersonalityQuirk.THIRD_PERSON.text", "PersonalityQuirk.THIRD_PERSON.description"),
    TIME_MANAGEMENT("PersonalityQuirk.TIME_MANAGEMENT.text", "PersonalityQuirk.TIME_MANAGEMENT.description"),
    TINKERER("PersonalityQuirk.TINKERER.text", "PersonalityQuirk.TINKERER.description"),
    TRUTH_TELLER("PersonalityQuirk.TRUTH_TELLER.text", "PersonalityQuirk.TRUTH_TELLER.description"),
    UNNECESSARY_CAUTION("PersonalityQuirk.UNNECESSARY_CAUTION.text",
            "PersonalityQuirk.UNNECESSARY_CAUTION.description"),
    UNPREDICTABLE_SPEECH("PersonalityQuirk.UNPREDICTABLE_SPEECH.text",
            "PersonalityQuirk.UNPREDICTABLE_SPEECH.description"),
    UNUSUAL_HOBBIES("PersonalityQuirk.UNUSUAL_HOBBIES.text", "PersonalityQuirk.UNUSUAL_HOBBIES.description"),
    WATCH("PersonalityQuirk.WATCH.text", "PersonalityQuirk.WATCH.description"),
    WEATHERMAN("PersonalityQuirk.WEATHERMAN.text", "PersonalityQuirk.WEATHERMAN.description"),
    WHISTLER("PersonalityQuirk.WHISTLER.text", "PersonalityQuirk.WHISTLER.description"),
    WORRIER("PersonalityQuirk.WORRIER.text", "PersonalityQuirk.WORRIER.description"),
    WRITER("PersonalityQuirk.WRITER.text", "PersonalityQuirk.WRITER.description"),
    BATTLEFIELD_NOSTALGIA("PersonalityQuirk.BATTLEFIELD_NOSTALGIA.text", "PersonalityQuirk.BATTLEFIELD_NOSTALGIA.description"),
    HEAVY_HANDED("PersonalityQuirk.HEAVY_HANDED.text", "PersonalityQuirk.HEAVY_HANDED.description"),
    RATION_HOARDER("PersonalityQuirk.RATION_HOARDER.text", "PersonalityQuirk.RATION_HOARDER.description"),
    EMERGENCY_MANUAL_READER("PersonalityQuirk.EMERGENCY_MANUAL_READER.text", "PersonalityQuirk.EMERGENCY_MANUAL_READER.description"),
    QUICK_TO_QUIP("PersonalityQuirk.QUICK_TO_QUIP.text", "PersonalityQuirk.QUICK_TO_QUIP.description"),
    TECH_SKEPTIC("PersonalityQuirk.TECH_SKEPTIC.text", "PersonalityQuirk.TECH_SKEPTIC.description"),
    POST_BATTLE_RITUALS("PersonalityQuirk.POST_BATTLE_RITUALS.text", "PersonalityQuirk.POST_BATTLE_RITUALS.description"),
    OVER_COMMUNICATOR("PersonalityQuirk.OVER_COMMUNICATOR.text", "PersonalityQuirk.OVER_COMMUNICATOR.description"),
    FIELD_MEDIC("PersonalityQuirk.FIELD_MEDIC.text", "PersonalityQuirk.FIELD_MEDIC.description"),
    SYSTEM_CALIBRATOR("PersonalityQuirk.SYSTEM_CALIBRATOR.text", "PersonalityQuirk.SYSTEM_CALIBRATOR.description"),
    AMMO_COUNTER("PersonalityQuirk.AMMO_COUNTER.text", "PersonalityQuirk.AMMO_COUNTER.description"),
    BRAVADO("PersonalityQuirk.BRAVADO.text", "PersonalityQuirk.BRAVADO.description"),
    COMBAT_SONG("PersonalityQuirk.COMBAT_SONG.text", "PersonalityQuirk.COMBAT_SONG.description"),
    COMMS_TOGGLE("PersonalityQuirk.COMMS_TOGGLE.text", "PersonalityQuirk.COMMS_TOGGLE.description"),
    EJECTION_READY("PersonalityQuirk.EJECTION_READY.text", "PersonalityQuirk.EJECTION_READY.description"),
    HAND_SIGNS("PersonalityQuirk.HAND_SIGNS.text", "PersonalityQuirk.HAND_SIGNS.description"),
    HATE_FOR_MEKS("PersonalityQuirk.HATE_FOR_MEKS.text", "PersonalityQuirk.HATE_FOR_MEKS.description"),
    IMPROVISED_WEAPONRY("PersonalityQuirk.IMPROVISED_WEAPONRY.text", "PersonalityQuirk.IMPROVISED_WEAPONRY.description"),
    PRE_BATTLE_SUPERSTITIONS("PersonalityQuirk.PRE_BATTLE_SUPERSTITIONS.text", "PersonalityQuirk.PRE_BATTLE_SUPERSTITIONS.description"),
    SILENT_LEADER("PersonalityQuirk.SILENT_LEADER.text", "PersonalityQuirk.SILENT_LEADER.description"),
    BATTLE_CRITIC("PersonalityQuirk.BATTLE_CRITIC.text", "PersonalityQuirk.BATTLE_CRITIC.description"),
    CHECKS_WEAPON_SAFETY("PersonalityQuirk.CHECKS_WEAPON_SAFETY.text", "PersonalityQuirk.CHECKS_WEAPON_SAFETY.description"),
    CLOSE_COMBAT_PREF("PersonalityQuirk.CLOSE_COMBAT_PREF.text", "PersonalityQuirk.CLOSE_COMBAT_PREF.description"),
    COMBAT_POET("PersonalityQuirk.COMBAT_POET.text", "PersonalityQuirk.COMBAT_POET.description"),
    CUSTOM_DECALS("PersonalityQuirk.CUSTOM_DECALS.text", "PersonalityQuirk.CUSTOM_DECALS.description"),
    DISPLAYS_TROPHIES("PersonalityQuirk.DISPLAYS_TROPHIES.text", "PersonalityQuirk.DISPLAYS_TROPHIES.description"),
    DO_IT_YOURSELF("PersonalityQuirk.DO_IT_YOURSELF.text", "PersonalityQuirk.DO_IT_YOURSELF.description"),
    FIELD_IMPROVISER("PersonalityQuirk.FIELD_IMPROVISER.text", "PersonalityQuirk.FIELD_IMPROVISER.description"),
    LOUD_COMMS("PersonalityQuirk.LOUD_COMMS.text", "PersonalityQuirk.LOUD_COMMS.description"),
    WAR_STORIES("PersonalityQuirk.WAR_STORIES.text", "PersonalityQuirk.WAR_STORIES.description"),
    ALL_OR_NOTHING("PersonalityQuirk.ALL_OR_NOTHING.text", "PersonalityQuirk.ALL_OR_NOTHING.description"),
    BOOTS_ON_THE_GROUND("PersonalityQuirk.BOOTS_ON_THE_GROUND.text", "PersonalityQuirk.BOOTS_ON_THE_GROUND.description"),
    BRAVERY_BOASTER("PersonalityQuirk.BRAVERY_BOASTER.text", "PersonalityQuirk.BRAVERY_BOASTER.description"),
    COCKPIT_DRIFTER("PersonalityQuirk.COCKPIT_DRIFTER.text", "PersonalityQuirk.COCKPIT_DRIFTER.description"),
    CONSPIRACY_THEORIST("PersonalityQuirk.CONSPIRACY_THEORIST.text", "PersonalityQuirk.CONSPIRACY_THEORIST.description"),
    DEVOUT_WARRIOR("PersonalityQuirk.DEVOUT_WARRIOR.text", "PersonalityQuirk.DEVOUT_WARRIOR.description"),
    DUAL_WIELDING("PersonalityQuirk.DUAL_WIELDING.text", "PersonalityQuirk.DUAL_WIELDING.description"),
    EMBLEM_LOVER("PersonalityQuirk.EMBLEM_LOVER.text", "PersonalityQuirk.EMBLEM_LOVER.description"),
    EXCESSIVE_DEBRIEFING("PersonalityQuirk.EXCESSIVE_DEBRIEFING.text", "PersonalityQuirk.EXCESSIVE_DEBRIEFING.description"),
    EYE_FOR_ART("PersonalityQuirk.EYE_FOR_ART.text", "PersonalityQuirk.EYE_FOR_ART.description"),
    FAST_TALKER("PersonalityQuirk.FAST_TALKER.text", "PersonalityQuirk.FAST_TALKER.description"),
    FINGER_GUNS("PersonalityQuirk.FINGER_GUNS.text", "PersonalityQuirk.FINGER_GUNS.description"),
    FLARE_DEPLOYER("PersonalityQuirk.FLARE_DEPLOYER.text", "PersonalityQuirk.FLARE_DEPLOYER.description"),
    FRIENDLY_INTERROGATOR("PersonalityQuirk.FRIENDLY_INTERROGATOR.text", "PersonalityQuirk.FRIENDLY_INTERROGATOR.description"),
    GUN_NUT("PersonalityQuirk.GUN_NUT.text", "PersonalityQuirk.GUN_NUT.description"),
    LAST_MAN_STANDING("PersonalityQuirk.LAST_MAN_STANDING.text", "PersonalityQuirk.LAST_MAN_STANDING.description"),
    LEGENDARY_MEK("PersonalityQuirk.LEGENDARY_MEK.text", "PersonalityQuirk.LEGENDARY_MEK.description"),
    PASSIVE_LEADER("PersonalityQuirk.PASSIVE_LEADER.text", "PersonalityQuirk.PASSIVE_LEADER.description"),
    REBEL_WITHOUT_CAUSE("PersonalityQuirk.REBEL_WITHOUT_CAUSE.text", "PersonalityQuirk.REBEL_WITHOUT_CAUSE.description"),
    SIMPLE_LIFE("PersonalityQuirk.SIMPLE_LIFE.text", "PersonalityQuirk.SIMPLE_LIFE.description"),
    ANTI_AUTHORITY("PersonalityQuirk.ANTI_AUTHORITY.text", "PersonalityQuirk.ANTI_AUTHORITY.description"),
    BLOODLUST("PersonalityQuirk.BLOODLUST.text", "PersonalityQuirk.BLOODLUST.description"),
    BRAVERY_IN_DOUBT("PersonalityQuirk.BRAVERY_IN_DOUBT.text", "PersonalityQuirk.BRAVERY_IN_DOUBT.description"),
    CLOSE_QUARTERS_ONLY("PersonalityQuirk.CLOSE_QUARTERS_ONLY.text", "PersonalityQuirk.CLOSE_QUARTERS_ONLY.description"),
    COOL_UNDER_FIRE("PersonalityQuirk.COOL_UNDER_FIRE.text", "PersonalityQuirk.COOL_UNDER_FIRE.description"),
    CRASH_TEST("PersonalityQuirk.CRASH_TEST.text", "PersonalityQuirk.CRASH_TEST.description"),
    DEAD_PAN_HUMOR("PersonalityQuirk.DEAD_PAN_HUMOR.text", "PersonalityQuirk.DEAD_PAN_HUMOR.description"),
    DRILLS("PersonalityQuirk.DRILLS.text", "PersonalityQuirk.DRILLS.description"),
    ENEMY_RESPECT("PersonalityQuirk.ENEMY_RESPECT.text", "PersonalityQuirk.ENEMY_RESPECT.description"),
    EXTREME_MORNING_PERSON("PersonalityQuirk.EXTREME_MORNING_PERSON.text", "PersonalityQuirk.EXTREME_MORNING_PERSON.description"),
    GALLANT("PersonalityQuirk.GALLANT.text", "PersonalityQuirk.GALLANT.description"),
    IRON_STOMACH("PersonalityQuirk.IRON_STOMACH.text", "PersonalityQuirk.IRON_STOMACH.description"),
    MISSION_CRITIC("PersonalityQuirk.MISSION_CRITIC.text", "PersonalityQuirk.MISSION_CRITIC.description"),
    NO_PAIN_NO_GAIN("PersonalityQuirk.NO_PAIN_NO_GAIN.text", "PersonalityQuirk.NO_PAIN_NO_GAIN.description"),
    PERSONAL_ARMORY("PersonalityQuirk.PERSONAL_ARMORY.text", "PersonalityQuirk.PERSONAL_ARMORY.description"),
    QUICK_ADAPTER("PersonalityQuirk.QUICK_ADAPTER.text", "PersonalityQuirk.QUICK_ADAPTER.description"),
    RETALIATOR("PersonalityQuirk.RETALIATOR.text", "PersonalityQuirk.RETALIATOR.description"),
    RUSH_HOUR("PersonalityQuirk.RUSH_HOUR.text", "PersonalityQuirk.RUSH_HOUR.description"),
    SILENT_PROTECTOR("PersonalityQuirk.SILENT_PROTECTOR.text", "PersonalityQuirk.SILENT_PROTECTOR.description"),
    ALWAYS_TACTICAL("PersonalityQuirk.ALWAYS_TACTICAL.text", "PersonalityQuirk.ALWAYS_TACTICAL.description"),
    BATTLE_SCREAM("PersonalityQuirk.BATTLE_SCREAM.text", "PersonalityQuirk.BATTLE_SCREAM.description"),
    BRIEF_AND_TO_THE_POINT("PersonalityQuirk.BRIEF_AND_TO_THE_POINT.text", "PersonalityQuirk.BRIEF_AND_TO_THE_POINT.description"),
    CALLSIGN_COLLECTOR("PersonalityQuirk.CALLSIGN_COLLECTOR.text", "PersonalityQuirk.CALLSIGN_COLLECTOR.description"),
    CHATTERBOX("PersonalityQuirk.CHATTERBOX.text", "PersonalityQuirk.CHATTERBOX.description"),
    COMBAT_ARTIST("PersonalityQuirk.COMBAT_ARTIST.text", "PersonalityQuirk.COMBAT_ARTIST.description"),
    DARING_ESCAPE("PersonalityQuirk.DARING_ESCAPE.text", "PersonalityQuirk.DARING_ESCAPE.description"),
    DOOMSDAY_PREPPER("PersonalityQuirk.DOOMSDAY_PREPPER.text", "PersonalityQuirk.DOOMSDAY_PREPPER.description"),
    EQUIPMENT_SCAVENGER("PersonalityQuirk.EQUIPMENT_SCAVENGER.text", "PersonalityQuirk.EQUIPMENT_SCAVENGER.description"),
    FRIEND_TO_FOES("PersonalityQuirk.FRIEND_TO_FOES.text", "PersonalityQuirk.FRIEND_TO_FOES.description"),
    GUNG_HO("PersonalityQuirk.GUNG_HO.text", "PersonalityQuirk.GUNG_HO.description"),
    INSPIRATIONAL_POET("PersonalityQuirk.INSPIRATIONAL_POET.text", "PersonalityQuirk.INSPIRATIONAL_POET.description"),
    MEK_MATCHMAKER("PersonalityQuirk.MEK_MATCHMAKER.text", "PersonalityQuirk.MEK_MATCHMAKER.description"),
    MISSILE_JUNKIE("PersonalityQuirk.MISSILE_JUNKIE.text", "PersonalityQuirk.MISSILE_JUNKIE.description"),
    NEVER_RETREAT("PersonalityQuirk.NEVER_RETREAT.text", "PersonalityQuirk.NEVER_RETREAT.description"),
    OPTIMISTIC_TO_A_FAULT("PersonalityQuirk.OPTIMISTIC_TO_A_FAULT.text", "PersonalityQuirk.OPTIMISTIC_TO_A_FAULT.description"),
    REACTIVE("PersonalityQuirk.REACTIVE.text", "PersonalityQuirk.REACTIVE.description"),
    RISK_TAKER("PersonalityQuirk.RISK_TAKER.text", "PersonalityQuirk.RISK_TAKER.description"),
    SIGNATURE_MOVE("PersonalityQuirk.SIGNATURE_MOVE.text", "PersonalityQuirk.SIGNATURE_MOVE.description"),
    TACTICAL_WITHDRAWAL("PersonalityQuirk.TACTICAL_WITHDRAWAL.text", "PersonalityQuirk.TACTICAL_WITHDRAWAL.description"),
    ACCENT_SWITCHER("PersonalityQuirk.ACCENT_SWITCHER.text", "PersonalityQuirk.ACCENT_SWITCHER.description"),
    AMBUSH_LOVER("PersonalityQuirk.AMBUSH_LOVER.text", "PersonalityQuirk.AMBUSH_LOVER.description"),
    BATTLE_HARDENED("PersonalityQuirk.BATTLE_HARDENED.text", "PersonalityQuirk.BATTLE_HARDENED.description"),
    BREAKS_RADIO_SILENCE("PersonalityQuirk.BREAKS_RADIO_SILENCE.text", "PersonalityQuirk.BREAKS_RADIO_SILENCE.description"),
    CONVOY_LOVER("PersonalityQuirk.CONVOY_LOVER.text", "PersonalityQuirk.CONVOY_LOVER.description"),
    DEBRIS_SLINGER("PersonalityQuirk.DEBRIS_SLINGER.text", "PersonalityQuirk.DEBRIS_SLINGER.description"),
    CAMOUFLAGE("PersonalityQuirk.CAMOUFLAGE.text", "PersonalityQuirk.CAMOUFLAGE.description"),
    DISTANT_LEADER("PersonalityQuirk.DISTANT_LEADER.text", "PersonalityQuirk.DISTANT_LEADER.description"),
    DRAMATIC_FINISH("PersonalityQuirk.DRAMATIC_FINISH.text", "PersonalityQuirk.DRAMATIC_FINISH.description"),
    ENGINE_REVERER("PersonalityQuirk.ENGINE_REVERER.text", "PersonalityQuirk.ENGINE_REVERER.description"),
    FLIRTY_COMMS("PersonalityQuirk.FLIRTY_COMMS.text", "PersonalityQuirk.FLIRTY_COMMS.description"),
    FOCUS_FREAK("PersonalityQuirk.FOCUS_FREAK.text", "PersonalityQuirk.FOCUS_FREAK.description"),
    FOUL_MOUTHED("PersonalityQuirk.FOUL_MOUTHED.text", "PersonalityQuirk.FOUL_MOUTHED.description"),
    FREESTYLE_COMBAT("PersonalityQuirk.FREESTYLE_COMBAT.text", "PersonalityQuirk.FREESTYLE_COMBAT.description"),
    GEOMETRY_GURU("PersonalityQuirk.GEOMETRY_GURU.text", "PersonalityQuirk.GEOMETRY_GURU.description"),
    ICE_COLD("PersonalityQuirk.ICE_COLD.text", "PersonalityQuirk.ICE_COLD.description"),
    PICKY_ABOUT_GEAR("PersonalityQuirk.PICKY_ABOUT_GEAR.text", "PersonalityQuirk.PICKY_ABOUT_GEAR.description"),
    RECORD_KEEPER("PersonalityQuirk.RECORD_KEEPER.text", "PersonalityQuirk.RECORD_KEEPER.description"),
    RESOURCE_SCROUNGER("PersonalityQuirk.RESOURCE_SCROUNGER.text", "PersonalityQuirk.RESOURCE_SCROUNGER.description"),
    TRASH_TALKER("PersonalityQuirk.TRASH_TALKER.text", "PersonalityQuirk.TRASH_TALKER.description"),
    CORRECTS_PRONOUNS("PersonalityQuirk.CORRECTS_PRONOUNS.text", "PersonalityQuirk.CORRECTS_PRONOUNS.description"),
    BODY_DISCOMFORT("PersonalityQuirk.BODY_DISCOMFORT.text", "PersonalityQuirk.BODY_DISCOMFORT.description");
    // endregion Enum Declarations

    // region Variable Declarations
    private final String name;
    private final String description;
    // endregion Variable Declarations

    // region Constructors
    PersonalityQuirk(final String name, final String description) {
        final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Personalities",
                MekHQ.getMHQOptions().getLocale());
        this.name = resources.getString(name);
        this.description = resources.getString(description);
    }
    // endregion Constructors

    // region Getters

    public String getDescription() {
        return description;
    }
    // endregion Getters

    // region Boolean Comparison Methods

    public boolean isNone() {
        return this == NONE;
    }
    // endregion Boolean Comparison Methods

    // region File I/O
    /**
     * Parses a given string and returns the corresponding Quirk enum.
     * Accepts either the ENUM ordinal value, or its name
     *
     * @param quirk the string to be parsed
     * @return the Greed enum that corresponds to the given string
     * @throws IllegalStateException if the given string does not match any valid
     *                               Quirk
     */
    @Deprecated
    public static PersonalityQuirk parseFromString(final String quirk) {
        return switch (quirk) {
            case "0", "none" -> NONE;
            case "1", "Constantly Adjusting Clothes" -> ADJUSTS_CLOTHES;
            case "2", "Overly Affectionate" -> AFFECTIONATE;
            case "3", "Overly Apologetic" -> APOLOGETIC;
            case "4", "Always Reading" -> BOOKWORM;
            case "5", "Keeps a Personal Calendar" -> CALENDAR;
            case "6", "Fond of Scented Candles" -> CANDLES;
            case "7", "Always Chewing Gum" -> CHEWING_GUM;
            case "8", "Chronically Late" -> CHRONIC_LATENESS;
            case "9", "Compulsive Cleaner" -> CLEANER;
            case "10", "Collects Odd Items" -> COLLECTOR;
            case "11", "Competitive Nature" -> COMPETITIVE_NATURE;
            case "12", "Excessive Complimenter" -> COMPLIMENTS;
            case "13", "Prone to Daydreaming" -> DAYDREAMER;
            case "14", "Compulsive Doodler" -> DOODLER;
            case "15", "Talks to Animals" -> DOOLITTLE;
            case "16", "Overly Dramatic" -> DRAMATIC;
            case "17", "Unpredictable Eating Habits" -> EATING_HABITS;
            case "18", "Extreme Environmental Sensitivity" -> ENVIRONMENTAL_SENSITIVITY;
            case "19", "Excessive Caution" -> EXCESSIVE_CAUTION;
            case "20", "Over-the-Top Greetings" -> EXCESSIVE_GREETING;
            case "21", "Intense Eye Contact" -> EYE_CONTACT;
            case "22", "Eccentric Fashion Choices" -> FASHION_CHOICES;
            case "23", "Constantly Fidgeting" -> FIDGETS;
            case "24", "Extreme Personal Fitness Routine" -> FITNESS;
            case "25", "Fixates on One Topic" -> FIXATES;
            case "26", "Carries a Flask" -> FLASK;
            case "27", "Always Tapping Foot" -> FOOT_TAPPER;
            case "28", "Chronically Forgetful" -> FORGETFUL;
            case "29", "Overly Formal Speech" -> FORMAL_SPEECH;
            case "30", "Constantly Rearranges Furniture" -> FURNITURE;
            case "31", "Constantly Adjusts Glasses" -> GLASSES;
            case "32", "Always Wearing Gloves" -> GLOVES;
            case "33", "Excessive Hand Gestures" -> HAND_GESTURES;
            case "34", "Compulsive Hand-Wringer" -> HAND_WRINGER;
            case "35", "Overly Enthusiastic Handshake" -> HANDSHAKE;
            case "36", "Always Wearing Headphones" -> HEADPHONES;
            case "37", "Frequently Snacking on Healthy Foods" -> HEALTHY_SNACKS;
            case "38", "Passionate about History" -> HISTORIAN;
            case "39", "Habitual Hummer" -> HUMMER;
            case "40", "Obsessed with Hygiene" -> HYGIENIC;
            case "41", "Unusual Sleep Patterns" -> IRREGULAR_SLEEPER;
            case "42", "Fond of Puns" -> JOKER;
            case "43", "Compulsive List Maker" -> LISTS;
            case "44", "Overly Literal" -> LITERAL;
            case "45", "Checks Locks Repeatedly" -> LOCKS;
            case "46", "Tends to Speak in a Measured Pace" -> MEASURED_TALKER;
            case "47", "Extreme Minimalism" -> MINIMALIST;
            case "48", "Prefers Using a Specific Mug" -> MUG;
            case "49", "Constant Nail Biter" -> NAIL_BITER;
            case "50", "Frequent Nicknaming" -> NICKNAMING;
            case "51", "Night Owl" -> NIGHT_OWL;
            case "52", "Compulsive Note-Taking" -> NOTE_TAKER;
            case "53", "Always Carrying a Notebook" -> NOTEBOOK;
            case "54", "Carries a Personal Object" -> OBJECT;
            case "55", "Obsessive Organizational Tendencies" -> ORGANIZATIONAL_TENDENCIES;
            case "56", "Always Organizing" -> ORGANIZER;
            case "57", "Fond of Origami" -> ORIGAMI;
            case "58", "Obsessive Over-Planner" -> OVER_PLANNER;
            case "59", "Chronic Overexplainer" -> OVEREXPLAINER;
            case "60", "abitual Pen Clicker" -> PEN_CLICKER;
            case "61", "Habitual Pen Twirler" -> PEN_TWIRLER;
            case "62", "Overly Friendly with Equipment" -> PERSONIFICATION;
            case "63", "Habitual Pessimist" -> PESSIMIST;
            case "64", "Tends to Use Specific Phrases" -> PHRASES;
            case "65", "Loves Plants" -> PLANTS;
            case "66", "Excessive Politeness" -> POLITE;
            case "67", "Loves Practical Jokes" -> PRACTICAL_JOKER;
            case "68", "Always Prepared" -> PREPARED;
            case "69", "Overly Punctual" -> PUNCTUAL;
            case "70", "Obsessed with Puzzles" -> PUZZLES;
            case "71", "Collects Quotes" -> QUOTES;
            case "72", "Rarely Sleeps" -> RARELY_SLEEPS;
            case "73", "Has a Routine for Small Tasks" -> ROUTINE;
            case "74", "Constantly Seeking Approval" -> SEEKS_APPROVAL;
            case "75", "Overly Sentimental" -> SENTIMENTAL;
            case "76", "Compulsive Sharpening" -> SHARPENING;
            case "77", "Sings to Themselves" -> SINGS;
            case "78", "Chronically Skeptical" -> SKEPTICAL;
            case "79", "Talks in Sleep" -> SLEEP_TALKER;
            case "80", "Compulsive Smiler" -> SMILER;
            case "81", "Always Has a Snack" -> SNACKS;
            case "82", "Compulsive Storytelling" -> STORYTELLING;
            case "83", "Constantly Stretching" -> STRETCHING;
            case "84", "Superstitious Rituals" -> SUPERSTITIOUS_RITUALS;
            case "85", "Highly Supervised Habits" -> SUPERVISED_HABITS;
            case "86", "Incessant Tech Talk" -> TECH_TALK;
            case "87", "Phobia of Technology" -> TECHNOPHOBIA;
            case "88", "Uses Obscure Words" -> THESAURUS;
            case "89", "Speaks in Third Person" -> THIRD_PERSON;
            case "90", "Obsessed with Time Management" -> TIME_MANAGEMENT;
            case "91", "Compulsive Tinkerer" -> TINKERER;
            case "92", "Habitual Truth-Teller" -> TRUTH_TELLER;
            case "93", "Unnecessary Caution" -> UNNECESSARY_CAUTION;
            case "94", "Unpredictable Speech" -> UNPREDICTABLE_SPEECH;
            case "95", "Unusual Hobbies" -> UNUSUAL_HOBBIES;
            case "96", "Constantly Checking the Time" -> WATCH;
            case "97", "Obsessed with Weather" -> WEATHERMAN;
            case "98", "Frequent Whistler" -> WHISTLER;
            case "99", "Persistent Worrier" -> WORRIER;
            case "100", "Writes Everything Down" -> WRITER;
            case "101", "Constantly Reminiscing" -> BATTLEFIELD_NOSTALGIA;
            default ->
                throw new IllegalStateException(
                        "Unexpected value in mekhq/campaign/personnel/enums/randomEvents/personalities/PersonalityQuirk.java/parseFromString: "
                                + quirk);
        };
    }

    /**
     * Returns the {@link PersonalityQuirk} associated with the given ordinal.
     *
     * @param ordinal the ordinal value of the {@link PersonalityQuirk}
     * @return the {@link PersonalityQuirk} associated with the given ordinal, or default value
     * {@code NONE} if not found
     */
    public static PersonalityQuirk fromOrdinal(int ordinal) {
        for (PersonalityQuirk quirk : values()) {
            if (quirk.ordinal() == ordinal) {
                return quirk;
            }
        }

        final MMLogger logger = MMLogger.create(PersonalityQuirk.class);
        logger.error(String.format("Unknown PersonalityQuirk ordinal: %s - returning NONE.", ordinal));

        return NONE;
    }

    @Override
    public String toString() {
        return name;
    }
}
