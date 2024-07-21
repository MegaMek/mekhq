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
package mekhq.campaign.personnel.enums.randomEvents.personalities;

import mekhq.MekHQ;

import java.util.ResourceBundle;

public enum PersonalityQuirks {
    //region Enum Declarations
    NONE("Personality.NONE.text", "Personality.NONE.description"),
    ADJUSTS_CLOTHES("PersonalityQuirks.ADJUSTS_CLOTHES.text", "PersonalityQuirks.ADJUSTS_CLOTHES.description"),
    AFFECTIONATE("PersonalityQuirks.AFFECTIONATE.text", "PersonalityQuirks.AFFECTIONATE.description"),
    APOLOGETIC("PersonalityQuirks.APOLOGETIC.text", "PersonalityQuirks.APOLOGETIC.description"),
    BOOKWORM("PersonalityQuirks.BOOKWORM.text", "PersonalityQuirks.BOOKWORM.description"),
    CALENDAR("PersonalityQuirks.CALENDAR.text", "PersonalityQuirks.CALENDAR.description"),
    CANDLES("PersonalityQuirks.CANDLES.text", "PersonalityQuirks.CANDLES.description"),
    CHEWING_GUM("PersonalityQuirks.CHEWING_GUM.text", "PersonalityQuirks.CHEWING_GUM.description"),
    CHRONIC_LATENESS("PersonalityQuirks.CHRONIC_LATENESS.text", "PersonalityQuirks.CHRONIC_LATENESS.description"),
    CLEANER("PersonalityQuirks.CLEANER.text", "PersonalityQuirks.CLEANER.description"),
    COLLECTOR("PersonalityQuirks.COLLECTOR.text", "PersonalityQuirks.COLLECTOR.description"),
    COMPETITIVE_NATURE("PersonalityQuirks.COMPETITIVE_NATURE.text", "PersonalityQuirks.COMPETITIVE_NATURE.description"),
    COMPLIMENTS("PersonalityQuirks.COMPLIMENTS.text", "PersonalityQuirks.COMPLIMENTS.description"),
    DAYDREAMER("PersonalityQuirks.DAYDREAMER.text", "PersonalityQuirks.DAYDREAMER.description"),
    DOODLER("PersonalityQuirks.DOODLER.text", "PersonalityQuirks.DOODLER.description"),
    DOOLITTLE("PersonalityQuirks.DOOLITTLE.text", "PersonalityQuirks.DOOLITTLE.description"),
    DRAMATIC("PersonalityQuirks.DRAMATIC.text", "PersonalityQuirks.DRAMATIC.description"),
    EATING_HABITS("PersonalityQuirks.EATING_HABITS.text", "PersonalityQuirks.EATING_HABITS.description"),
    ENVIRONMENTAL_SENSITIVITY("PersonalityQuirks.ENVIRONMENTAL_SENSITIVITY.text", "PersonalityQuirks.ENVIRONMENTAL_SENSITIVITY.description"),
    EXCESSIVE_CAUTION("PersonalityQuirks.EXCESSIVE_CAUTION.text", "PersonalityQuirks.EXCESSIVE_CAUTION.description"),
    EXCESSIVE_GREETING("PersonalityQuirks.EXCESSIVE_GREETING.text", "PersonalityQuirks.EXCESSIVE_GREETING.description"),
    EYE_CONTACT("PersonalityQuirks.EYE_CONTACT.text", "PersonalityQuirks.EYE_CONTACT.description"),
    FASHION_CHOICES("PersonalityQuirks.FASHION_CHOICES.text", "PersonalityQuirks.FASHION_CHOICES.description"),
    FIDGETS("PersonalityQuirks.FIDGETS.text", "PersonalityQuirks.FIDGETS.description"),
    FITNESS("PersonalityQuirks.FITNESS.text", "PersonalityQuirks.FITNESS.description"),
    FIXATES("PersonalityQuirks.FIXATES.text", "PersonalityQuirks.FIXATES.description"),
    FLASK("PersonalityQuirks.FLASK.text", "PersonalityQuirks.FLASK.description"),
    FOOT_TAPPER("PersonalityQuirks.FOOT_TAPPER.text", "PersonalityQuirks.FOOT_TAPPER.description"),
    FORGETFUL("PersonalityQuirks.FORGETFUL.text", "PersonalityQuirks.FORGETFUL.description"),
    FORMAL_SPEECH("PersonalityQuirks.FORMAL_SPEECH.text", "PersonalityQuirks.FORMAL_SPEECH.description"),
    FURNITURE("PersonalityQuirks.FURNITURE.text", "PersonalityQuirks.FURNITURE.description"),
    GLASSES("PersonalityQuirks.GLASSES.text", "PersonalityQuirks.GLASSES.description"),
    GLOVES("PersonalityQuirks.GLOVES.text", "PersonalityQuirks.GLOVES.description"),
    HAND_GESTURES("PersonalityQuirks.HAND_GESTURES.text", "PersonalityQuirks.HAND_GESTURES.description"),
    HAND_WRINGER("PersonalityQuirks.HAND_WRINGER.text", "PersonalityQuirks.HAND_WRINGER.description"),
    HANDSHAKE("PersonalityQuirks.HANDSHAKE.text", "PersonalityQuirks.HANDSHAKE.description"),
    HEADPHONES("PersonalityQuirks.HEADPHONES.text", "PersonalityQuirks.HEADPHONES.description"),
    HEALTHY_SNACKS("PersonalityQuirks.HEALTHY_SNACKS.text", "PersonalityQuirks.HEALTHY_SNACKS.description"),
    HISTORIAN("PersonalityQuirks.HISTORIAN.text", "PersonalityQuirks.HISTORIAN.description"),
    HUMMER("PersonalityQuirks.HUMMER.text", "PersonalityQuirks.HUMMER.description"),
    HYGIENIC("PersonalityQuirks.HYGIENIC.text", "PersonalityQuirks.HYGIENIC.description"),
    IRREGULAR_SLEEPER("PersonalityQuirks.IRREGULAR_SLEEPER.text", "PersonalityQuirks.IRREGULAR_SLEEPER.description"),
    JOKER("PersonalityQuirks.JOKER.text", "PersonalityQuirks.JOKER.description"),
    LISTS("PersonalityQuirks.LISTS.text", "PersonalityQuirks.LISTS.description"),
    LITERAL("PersonalityQuirks.LITERAL.text", "PersonalityQuirks.LITERAL.description"),
    LOCKS("PersonalityQuirks.LOCKS.text", "PersonalityQuirks.LOCKS.description"),
    MEASURED_TALKER("PersonalityQuirks.MEASURED_TALKER.text", "PersonalityQuirks.MEASURED_TALKER.description"),
    MINIMALIST("PersonalityQuirks.MINIMALIST.text", "PersonalityQuirks.MINIMALIST.description"),
    MUG("PersonalityQuirks.MUG.text", "PersonalityQuirks.MUG.description"),
    NAIL_BITER("PersonalityQuirks.NAIL_BITER.text", "PersonalityQuirks.NAIL_BITER.description"),
    NICKNAMING("PersonalityQuirks.NICKNAMING.text", "PersonalityQuirks.NICKNAMING.description"),
    NIGHT_OWL("PersonalityQuirks.NIGHT_OWL.text", "PersonalityQuirks.NIGHT_OWL.description"),
    NOTE_TAKER("PersonalityQuirks.NOTE_TAKER.text", "PersonalityQuirks.NOTE_TAKER.description"),
    NOTEBOOK("PersonalityQuirks.NOTEBOOK.text", "PersonalityQuirks.NOTEBOOK.description"),
    OBJECT("PersonalityQuirks.OBJECT.text", "PersonalityQuirks.OBJECT.description"),
    ORGANIZATIONAL_TENDENCIES("PersonalityQuirks.ORGANIZATIONAL_TENDENCIES.text", "PersonalityQuirks.ORGANIZATIONAL_TENDENCIES.description"),
    ORGANIZER("PersonalityQuirks.ORGANIZER.text", "PersonalityQuirks.ORGANIZER.description"),
    ORIGAMI("PersonalityQuirks.ORIGAMI.text", "PersonalityQuirks.ORIGAMI.description"),
    OVER_PLANNER("PersonalityQuirks.OVER_PLANNER.text", "PersonalityQuirks.OVER_PLANNER.description"),
    OVEREXPLAINER("PersonalityQuirks.OVEREXPLAINER.text", "PersonalityQuirks.OVEREXPLAINER.description"),
    PEN_CLICKER("PersonalityQuirks.PEN_CLICKER.text", "PersonalityQuirks.PEN_CLICKER.description"),
    PEN_TWIRLER("PersonalityQuirks.PEN_TWIRLER.text", "PersonalityQuirks.PEN_TWIRLER.description"),
    PERSONIFICATION("PersonalityQuirks.PERSONIFICATION.text", "PersonalityQuirks.PERSONIFICATION.description"),
    PESSIMIST("PersonalityQuirks.PESSIMIST.text", "PersonalityQuirks.PESSIMIST.description"),
    PHRASES("PersonalityQuirks.PHRASES.text", "PersonalityQuirks.PHRASES.description"),
    PLANTS("PersonalityQuirks.PLANTS.text", "PersonalityQuirks.PLANTS.description"),
    POLITE("PersonalityQuirks.POLITE.text", "PersonalityQuirks.POLITE.description"),
    PRACTICAL_JOKER("PersonalityQuirks.PRACTICAL_JOKER.text", "PersonalityQuirks.PRACTICAL_JOKER.description"),
    PREPARED("PersonalityQuirks.PREPARED.text", "PersonalityQuirks.PREPARED.description"),
    PUNCTUAL("PersonalityQuirks.PUNCTUAL.text", "PersonalityQuirks.PUNCTUAL.description"),
    PUZZLES("PersonalityQuirks.PUZZLES.text", "PersonalityQuirks.PUZZLES.description"),
    QUOTES("PersonalityQuirks.QUOTES.text", "PersonalityQuirks.QUOTES.description"),
    RARELY_SLEEPS("PersonalityQuirks.RARELY_SLEEPS.text", "PersonalityQuirks.RARELY_SLEEPS.description"),
    ROUTINE("PersonalityQuirks.ROUTINE.text", "PersonalityQuirks.ROUTINE.description"),
    SEEKS_APPROVAL("PersonalityQuirks.SEEKS_APPROVAL.text", "PersonalityQuirks.SEEKS_APPROVAL.description"),
    SENTIMENTAL("PersonalityQuirks.SENTIMENTAL.text", "PersonalityQuirks.SENTIMENTAL.description"),
    SHARPENING("PersonalityQuirks.SHARPENING.text", "PersonalityQuirks.SHARPENING.description"),
    SINGS("PersonalityQuirks.SINGS.text", "PersonalityQuirks.SINGS.description"),
    SKEPTICAL("PersonalityQuirks.SKEPTICAL.text", "PersonalityQuirks.SKEPTICAL.description"),
    SLEEP_TALKER("PersonalityQuirks.SLEEP_TALKER.text", "PersonalityQuirks.SLEEP_TALKER.description"),
    SMILER("PersonalityQuirks.SMILER.text", "PersonalityQuirks.SMILER.description"),
    SNACKS("PersonalityQuirks.SNACKS.text", "PersonalityQuirks.SNACKS.description"),
    STORYTELLING("PersonalityQuirks.STORYTELLING.text", "PersonalityQuirks.STORYTELLING.description"),
    STRETCHING("PersonalityQuirks.STRETCHING.text", "PersonalityQuirks.STRETCHING.description"),
    SUPERSTITIOUS_RITUALS("PersonalityQuirks.SUPERSTITIOUS_RITUALS.text", "PersonalityQuirks.SUPERSTITIOUS_RITUALS.description"),
    SUPERVISED_HABITS("PersonalityQuirks.SUPERVISED_HABITS.text", "PersonalityQuirks.SUPERVISED_HABITS.description"),
    TECH_TALK("PersonalityQuirks.TECH_TALK.text", "PersonalityQuirks.TECH_TALK.description"),
    TECHNOPHOBIA("PersonalityQuirks.TECHNOPHOBIA.text", "PersonalityQuirks.TECHNOPHOBIA.description"),
    THESAURUS("PersonalityQuirks.THESAURUS.text", "PersonalityQuirks.THESAURUS.description"),
    THIRD_PERSON("PersonalityQuirks.THIRD_PERSON.text", "PersonalityQuirks.THIRD_PERSON.description"),
    TIME_MANAGEMENT("PersonalityQuirks.TIME_MANAGEMENT.text", "PersonalityQuirks.TIME_MANAGEMENT.description"),
    TINKERER("PersonalityQuirks.TINKERER.text", "PersonalityQuirks.TINKERER.description"),
    TRUTH_TELLER("PersonalityQuirks.TRUTH_TELLER.text", "PersonalityQuirks.TRUTH_TELLER.description"),
    UNNECESSARY_CAUTION("PersonalityQuirks.UNNECESSARY_CAUTION.text", "PersonalityQuirks.UNNECESSARY_CAUTION.description"),
    UNPREDICTABLE_SPEECH("PersonalityQuirks.UNPREDICTABLE_SPEECH.text", "PersonalityQuirks.UNPREDICTABLE_SPEECH.description"),
    UNUSUAL_HOBBIES("PersonalityQuirks.UNUSUAL_HOBBIES.text", "PersonalityQuirks.UNUSUAL_HOBBIES.description"),
    WATCH("PersonalityQuirks.WATCH.text", "PersonalityQuirks.WATCH.description"),
    WEATHERMAN("PersonalityQuirks.WEATHERMAN.text", "PersonalityQuirks.WEATHERMAN.description"),
    WHISTLER("PersonalityQuirks.WHISTLER.text", "PersonalityQuirks.WHISTLER.description"),
    WORRIER("PersonalityQuirks.WORRIER.text", "PersonalityQuirks.WORRIER.description"),
    WRITER("PersonalityQuirks.WRITER.text", "PersonalityQuirks.WRITER.description");
    //endregion Enum Declarations

    //region Variable Declarations
    private final String name;
    private final String description;
    //endregion Variable Declarations

    //region Constructors
    PersonalityQuirks(final String name, final String description) {
        final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Personnel",
                MekHQ.getMHQOptions().getLocale());
        this.name = resources.getString(name);
        this.description = resources.getString(description);
    }
    //endregion Constructors

    //region Getters
    @SuppressWarnings(value = "unused")
    public String getDescription() {
        return description;
    }
    //endregion Getters

    //region Boolean Comparison Methods
    @SuppressWarnings(value = "unused")
    public boolean isNone() {
        return this == NONE;
    }

    @SuppressWarnings(value = "unused")
    public boolean isAdjustsClothes() {
        return this == ADJUSTS_CLOTHES;
    }

    @SuppressWarnings(value = "unused")
    public boolean isAffectionate() {
        return this == AFFECTIONATE;
    }

    @SuppressWarnings(value = "unused")
    public boolean isApologetic() {
        return this == APOLOGETIC;
    }

    @SuppressWarnings(value = "unused")
    public boolean isBookworm() {
        return this == BOOKWORM;
    }

    @SuppressWarnings(value = "unused")
    public boolean isCalendar() {
        return this == CALENDAR;
    }

    @SuppressWarnings(value = "unused")
    public boolean isCandles() {
        return this == CANDLES;
    }

    @SuppressWarnings(value = "unused")
    public boolean isChewingGum() {
        return this == CHEWING_GUM;
    }

    @SuppressWarnings(value = "unused")
    public boolean isChronicLateness() {
        return this == CHRONIC_LATENESS;
    }

    @SuppressWarnings(value = "unused")
    public boolean isCleaner() {
        return this == CLEANER;
    }

    @SuppressWarnings(value = "unused")
    public boolean isCollector() {
        return this == COLLECTOR;
    }

    @SuppressWarnings(value = "unused")
    public boolean isCompetitiveNature() {
        return this == COMPETITIVE_NATURE;
    }

    @SuppressWarnings(value = "unused")
    public boolean isCompliments() {
        return this == COMPLIMENTS;
    }

    @SuppressWarnings(value = "unused")
    public boolean isDaydreamer() {
        return this == DAYDREAMER;
    }

    @SuppressWarnings(value = "unused")
    public boolean isDoodler() {
        return this == DOODLER;
    }

    @SuppressWarnings(value = "unused")
    public boolean isDoolittle() {
        return this == DOOLITTLE;
    }

    @SuppressWarnings(value = "unused")
    public boolean isDramatic() {
        return this == DRAMATIC;
    }

    @SuppressWarnings(value = "unused")
    public boolean isEatingHabits() {
        return this == EATING_HABITS;
    }

    @SuppressWarnings(value = "unused")
    public boolean isEnvironmentalSensitivity() {
        return this == ENVIRONMENTAL_SENSITIVITY;
    }

    @SuppressWarnings(value = "unused")
    public boolean isExcessiveCaution() {
        return this == EXCESSIVE_CAUTION;
    }

    @SuppressWarnings(value = "unused")
    public boolean isExcessiveGreeting() {
        return this == EXCESSIVE_GREETING;
    }

    @SuppressWarnings(value = "unused")
    public boolean isEyeContact() {
        return this == EYE_CONTACT;
    }

    @SuppressWarnings(value = "unused")
    public boolean isFashionChoices() {
        return this == FASHION_CHOICES;
    }

    @SuppressWarnings(value = "unused")
    public boolean isFidgets() {
        return this == FIDGETS;
    }

    @SuppressWarnings(value = "unused")
    public boolean isFitness() {
        return this == FITNESS;
    }

    @SuppressWarnings(value = "unused")
    public boolean isFixates() {
        return this == FIXATES;
    }

    @SuppressWarnings(value = "unused")
    public boolean isFlask() {
        return this == FLASK;
    }

    @SuppressWarnings(value = "unused")
    public boolean isFootTapper() {
        return this == FOOT_TAPPER;
    }

    @SuppressWarnings(value = "unused")
    public boolean isForgetful() {
        return this == FORGETFUL;
    }

    @SuppressWarnings(value = "unused")
    public boolean isFormalSpeech() {
        return this == FORMAL_SPEECH;
    }

    @SuppressWarnings(value = "unused")
    public boolean isFurniture() {
        return this == FURNITURE;
    }

    @SuppressWarnings(value = "unused")
    public boolean isGlasses() {
        return this == GLASSES;
    }

    @SuppressWarnings(value = "unused")
    public boolean isGloves() {
        return this == GLOVES;
    }

    @SuppressWarnings(value = "unused")
    public boolean isHandGestures() {
        return this == HAND_GESTURES;
    }

    @SuppressWarnings(value = "unused")
    public boolean isHandWringer() {
        return this == HAND_WRINGER;
    }

    @SuppressWarnings(value = "unused")
    public boolean isHandshake() {
        return this == HANDSHAKE;
    }

    @SuppressWarnings(value = "unused")
    public boolean isHeadphones() {
        return this == HEADPHONES;
    }

    @SuppressWarnings(value = "unused")
    public boolean isHealthySnacks() {
        return this == HEALTHY_SNACKS;
    }

    @SuppressWarnings(value = "unused")
    public boolean isHistorian() {
        return this == HISTORIAN;
    }

    @SuppressWarnings(value = "unused")
    public boolean isHummer() {
        return this == HUMMER;
    }

    @SuppressWarnings(value = "unused")
    public boolean isHygienic() {
        return this == HYGIENIC;
    }

    @SuppressWarnings(value = "unused")
    public boolean isIrregularSleeper() {
        return this == IRREGULAR_SLEEPER;
    }

    @SuppressWarnings(value = "unused")
    public boolean isJoker() {
        return this == JOKER;
    }

    @SuppressWarnings(value = "unused")
    public boolean isLists() {
        return this == LISTS;
    }

    @SuppressWarnings(value = "unused")
    public boolean isLiteral() {
        return this == LITERAL;
    }

    @SuppressWarnings(value = "unused")
    public boolean isLocks() {
        return this == LOCKS;
    }

    @SuppressWarnings(value = "unused")
    public boolean isMeasuredTalker() {
        return this == MEASURED_TALKER;
    }

    @SuppressWarnings(value = "unused")
    public boolean isMinimalist() {
        return this == MINIMALIST;
    }

    @SuppressWarnings(value = "unused")
    public boolean isMug() {
        return this == MUG;
    }

    @SuppressWarnings(value = "unused")
    public boolean isNailBiter() {
        return this == NAIL_BITER;
    }

    @SuppressWarnings(value = "unused")
    public boolean isNicknames() {
        return this == NICKNAMING;
    }

    @SuppressWarnings(value = "unused")
    public boolean isNightOwl() {
        return this == NIGHT_OWL;
    }

    @SuppressWarnings(value = "unused")
    public boolean isNoteTaker() {
        return this == NOTE_TAKER;
    }

    @SuppressWarnings(value = "unused")
    public boolean isNotebook() {
        return this == NOTEBOOK;
    }

    @SuppressWarnings(value = "unused")
    public boolean isObject() {
        return this == OBJECT;
    }

    @SuppressWarnings(value = "unused")
    public boolean isOrganizationalTendencies() {
        return this == ORGANIZATIONAL_TENDENCIES;
    }

    @SuppressWarnings(value = "unused")
    public boolean isOrganizer() {
        return this == ORGANIZER;
    }

    @SuppressWarnings(value = "unused")
    public boolean isOrigami() {
        return this == ORIGAMI;
    }

    @SuppressWarnings(value = "unused")
    public boolean isOverPlanner() {
        return this == OVER_PLANNER;
    }

    @SuppressWarnings(value = "unused")
    public boolean isOverExplainer() {
        return this == OVEREXPLAINER;
    }

    @SuppressWarnings(value = "unused")
    public boolean isPenClicker() {
        return this == PEN_CLICKER;
    }

    @SuppressWarnings(value = "unused")
    public boolean isPenTwirler() {
        return this == PEN_TWIRLER;
    }

    @SuppressWarnings(value = "unused")
    public boolean isPersonification() {
        return this == PERSONIFICATION;
    }

    @SuppressWarnings(value = "unused")
    public boolean isPessimist() {
        return this == PESSIMIST;
    }

    @SuppressWarnings(value = "unused")
    public boolean isPhrases() {
        return this == PHRASES;
    }

    @SuppressWarnings(value = "unused")
    public boolean isPlants() {
        return this == PLANTS;
    }

    @SuppressWarnings(value = "unused")
    public boolean isPolite() {
        return this == POLITE;
    }

    @SuppressWarnings(value = "unused")
    public boolean isPracticalJoker() {
        return this == PRACTICAL_JOKER;
    }

    @SuppressWarnings(value = "unused")
    public boolean isPrepared() {
        return this == PREPARED;
    }

    @SuppressWarnings(value = "unused")
    public boolean isPunctual() {
        return this == PUNCTUAL;
    }

    @SuppressWarnings(value = "unused")
    public boolean isPuzzles() {
        return this == PUZZLES;
    }

    @SuppressWarnings(value = "unused")
    public boolean isQuotes() {
        return this == QUOTES;
    }

    @SuppressWarnings(value = "unused")
    public boolean isRarelySleeps() {
        return this == RARELY_SLEEPS;
    }

    @SuppressWarnings(value = "unused")
    public boolean isRoutine() {
        return this == ROUTINE;
    }

    @SuppressWarnings(value = "unused")
    public boolean isSeeksApproval() {
        return this == SEEKS_APPROVAL;
    }

    @SuppressWarnings(value = "unused")
    public boolean isSentimental() {
        return this == SENTIMENTAL;
    }

    @SuppressWarnings(value = "unused")
    public boolean isSharpening() {
        return this == SHARPENING;
    }

    @SuppressWarnings(value = "unused")
    public boolean isSings() {
        return this == SINGS;
    }

    @SuppressWarnings(value = "unused")
    public boolean isSkeptical() {
        return this == SKEPTICAL;
    }

    @SuppressWarnings(value = "unused")
    public boolean isSleepTalker() {
        return this == SLEEP_TALKER;
    }

    @SuppressWarnings(value = "unused")
    public boolean isSmiler() {
        return this == SMILER;
    }

    @SuppressWarnings(value = "unused")
    public boolean isSnacks() {
        return this == SNACKS;
    }

    @SuppressWarnings(value = "unused")
    public boolean iStoryteller() {
        return this == STORYTELLING;
    }

    @SuppressWarnings(value = "unused")
    public boolean isStretching() {
        return this == STRETCHING;
    }

    @SuppressWarnings(value = "unused")
    public boolean isSuperstitiousRituals() {
        return this == SUPERSTITIOUS_RITUALS;
    }

    @SuppressWarnings(value = "unused")
    public boolean isSupervisedHabits() {
        return this == SUPERVISED_HABITS;
    }

    @SuppressWarnings(value = "unused")
    public boolean isTechTalk() {
        return this == TECH_TALK;
    }

    @SuppressWarnings(value = "unused")
    public boolean isTechnophobia() {
        return this == TECHNOPHOBIA;
    }

    @SuppressWarnings(value = "unused")
    public boolean isThesaurus() {
        return this == THESAURUS;
    }

    @SuppressWarnings(value = "unused")
    public boolean isThirdPerson() {
        return this == THIRD_PERSON;
    }

    @SuppressWarnings(value = "unused")
    public boolean isTimeManagement() {
        return this == TIME_MANAGEMENT;
    }

    @SuppressWarnings(value = "unused")
    public boolean isTinkerer() {
        return this == TINKERER;
    }

    @SuppressWarnings(value = "unused")
    public boolean isTruthTeller() {
        return this == TRUTH_TELLER;
    }

    @SuppressWarnings(value = "unused")
    public boolean isUnnecessaryCaution() {
        return this == UNNECESSARY_CAUTION;
    }

    @SuppressWarnings(value = "unused")
    public boolean isUnpredictableSpeech() {
        return this == UNPREDICTABLE_SPEECH;
    }

    @SuppressWarnings(value = "unused")
    public boolean isUnusualHobbies() {
        return this == UNUSUAL_HOBBIES;
    }

    @SuppressWarnings(value = "unused")
    public boolean isWatch() {
        return this == WATCH;
    }

    @SuppressWarnings(value = "unused")
    public boolean isWeatherman() {
        return this == WEATHERMAN;
    }

    @SuppressWarnings(value = "unused")
    public boolean isWhistler() {
        return this == WHISTLER;
    }

    @SuppressWarnings(value = "unused")
    public boolean isWorrier() {
        return this == WORRIER;
    }

    @SuppressWarnings(value = "unused")
    public boolean isWriter() {
        return this == WRITER;
    }
    //endregion Boolean Comparison Methods

    //region File I/O
    /**
     * Parses a given string and returns the corresponding Quirk enum.
     * Accepts either the ENUM ordinal value, or its name
     *
     * @param quirk the string to be parsed
     * @return the Greed enum that corresponds to the given string
     * @throws IllegalStateException if the given string does not match any valid Quirk
     */
    @SuppressWarnings(value = "unused")
    public static PersonalityQuirks parseFromString(final String quirk) {
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
            default ->
                    throw new IllegalStateException("Unexpected value in mekhq/campaign/personnel/enums/randomEvents/personalities/PersonalityQuirks.java/parseFromString: "
                            + quirk);
        };
    }

    @Override
    public String toString() {
        return name;
    }
}
