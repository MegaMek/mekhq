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

public enum PersonalityQuirk {
    //region Enum Declarations
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
    ENVIRONMENTAL_SENSITIVITY("PersonalityQuirk.ENVIRONMENTAL_SENSITIVITY.text", "PersonalityQuirk.ENVIRONMENTAL_SENSITIVITY.description"),
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
    ORGANIZATIONAL_TENDENCIES("PersonalityQuirk.ORGANIZATIONAL_TENDENCIES.text", "PersonalityQuirk.ORGANIZATIONAL_TENDENCIES.description"),
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
    SUPERSTITIOUS_RITUALS("PersonalityQuirk.SUPERSTITIOUS_RITUALS.text", "PersonalityQuirk.SUPERSTITIOUS_RITUALS.description"),
    SUPERVISED_HABITS("PersonalityQuirk.SUPERVISED_HABITS.text", "PersonalityQuirk.SUPERVISED_HABITS.description"),
    TECH_TALK("PersonalityQuirk.TECH_TALK.text", "PersonalityQuirk.TECH_TALK.description"),
    TECHNOPHOBIA("PersonalityQuirk.TECHNOPHOBIA.text", "PersonalityQuirk.TECHNOPHOBIA.description"),
    THESAURUS("PersonalityQuirk.THESAURUS.text", "PersonalityQuirk.THESAURUS.description"),
    THIRD_PERSON("PersonalityQuirk.THIRD_PERSON.text", "PersonalityQuirk.THIRD_PERSON.description"),
    TIME_MANAGEMENT("PersonalityQuirk.TIME_MANAGEMENT.text", "PersonalityQuirk.TIME_MANAGEMENT.description"),
    TINKERER("PersonalityQuirk.TINKERER.text", "PersonalityQuirk.TINKERER.description"),
    TRUTH_TELLER("PersonalityQuirk.TRUTH_TELLER.text", "PersonalityQuirk.TRUTH_TELLER.description"),
    UNNECESSARY_CAUTION("PersonalityQuirk.UNNECESSARY_CAUTION.text", "PersonalityQuirk.UNNECESSARY_CAUTION.description"),
    UNPREDICTABLE_SPEECH("PersonalityQuirk.UNPREDICTABLE_SPEECH.text", "PersonalityQuirk.UNPREDICTABLE_SPEECH.description"),
    UNUSUAL_HOBBIES("PersonalityQuirk.UNUSUAL_HOBBIES.text", "PersonalityQuirk.UNUSUAL_HOBBIES.description"),
    WATCH("PersonalityQuirk.WATCH.text", "PersonalityQuirk.WATCH.description"),
    WEATHERMAN("PersonalityQuirk.WEATHERMAN.text", "PersonalityQuirk.WEATHERMAN.description"),
    WHISTLER("PersonalityQuirk.WHISTLER.text", "PersonalityQuirk.WHISTLER.description"),
    WORRIER("PersonalityQuirk.WORRIER.text", "PersonalityQuirk.WORRIER.description"),
    WRITER("PersonalityQuirk.WRITER.text", "PersonalityQuirk.WRITER.description");
    //endregion Enum Declarations

    //region Variable Declarations
    private final String name;
    private final String description;
    //endregion Variable Declarations

    //region Constructors
    PersonalityQuirk(final String name, final String description) {
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
            default ->
                    throw new IllegalStateException("Unexpected value in mekhq/campaign/personnel/enums/randomEvents/personalities/PersonalityQuirk.java/parseFromString: "
                            + quirk);
        };
    }

    @Override
    public String toString() {
        return name;
    }
}
