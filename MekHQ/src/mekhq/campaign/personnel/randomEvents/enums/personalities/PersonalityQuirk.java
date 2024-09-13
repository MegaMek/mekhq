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

import java.util.ResourceBundle;

import mekhq.MekHQ;

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
    WRITER("PersonalityQuirk.WRITER.text", "PersonalityQuirk.WRITER.description");
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

    public boolean isAdjustsClothes() {
        return this == ADJUSTS_CLOTHES;
    }

    public boolean isAffectionate() {
        return this == AFFECTIONATE;
    }

    public boolean isApologetic() {
        return this == APOLOGETIC;
    }

    public boolean isBookworm() {
        return this == BOOKWORM;
    }

    public boolean isCalendar() {
        return this == CALENDAR;
    }

    public boolean isCandles() {
        return this == CANDLES;
    }

    public boolean isChewingGum() {
        return this == CHEWING_GUM;
    }

    public boolean isChronicLateness() {
        return this == CHRONIC_LATENESS;
    }

    public boolean isCleaner() {
        return this == CLEANER;
    }

    public boolean isCollector() {
        return this == COLLECTOR;
    }

    public boolean isCompetitiveNature() {
        return this == COMPETITIVE_NATURE;
    }

    public boolean isCompliments() {
        return this == COMPLIMENTS;
    }

    public boolean isDaydreamer() {
        return this == DAYDREAMER;
    }

    public boolean isDoodler() {
        return this == DOODLER;
    }

    public boolean isDoolittle() {
        return this == DOOLITTLE;
    }

    public boolean isDramatic() {
        return this == DRAMATIC;
    }

    public boolean isEatingHabits() {
        return this == EATING_HABITS;
    }

    public boolean isEnvironmentalSensitivity() {
        return this == ENVIRONMENTAL_SENSITIVITY;
    }

    public boolean isExcessiveCaution() {
        return this == EXCESSIVE_CAUTION;
    }

    public boolean isExcessiveGreeting() {
        return this == EXCESSIVE_GREETING;
    }

    public boolean isEyeContact() {
        return this == EYE_CONTACT;
    }

    public boolean isFashionChoices() {
        return this == FASHION_CHOICES;
    }

    public boolean isFidgets() {
        return this == FIDGETS;
    }

    public boolean isFitness() {
        return this == FITNESS;
    }

    public boolean isFixates() {
        return this == FIXATES;
    }

    public boolean isFlask() {
        return this == FLASK;
    }

    public boolean isFootTapper() {
        return this == FOOT_TAPPER;
    }

    public boolean isForgetful() {
        return this == FORGETFUL;
    }

    public boolean isFormalSpeech() {
        return this == FORMAL_SPEECH;
    }

    public boolean isFurniture() {
        return this == FURNITURE;
    }

    public boolean isGlasses() {
        return this == GLASSES;
    }

    public boolean isGloves() {
        return this == GLOVES;
    }

    public boolean isHandGestures() {
        return this == HAND_GESTURES;
    }

    public boolean isHandWringer() {
        return this == HAND_WRINGER;
    }

    public boolean isHandshake() {
        return this == HANDSHAKE;
    }

    public boolean isHeadphones() {
        return this == HEADPHONES;
    }

    public boolean isHealthySnacks() {
        return this == HEALTHY_SNACKS;
    }

    public boolean isHistorian() {
        return this == HISTORIAN;
    }

    public boolean isHummer() {
        return this == HUMMER;
    }

    public boolean isHygienic() {
        return this == HYGIENIC;
    }

    public boolean isIrregularSleeper() {
        return this == IRREGULAR_SLEEPER;
    }

    public boolean isJoker() {
        return this == JOKER;
    }

    public boolean isLists() {
        return this == LISTS;
    }

    public boolean isLiteral() {
        return this == LITERAL;
    }

    public boolean isLocks() {
        return this == LOCKS;
    }

    public boolean isMeasuredTalker() {
        return this == MEASURED_TALKER;
    }

    public boolean isMinimalist() {
        return this == MINIMALIST;
    }

    public boolean isMug() {
        return this == MUG;
    }

    public boolean isNailBiter() {
        return this == NAIL_BITER;
    }

    public boolean isNicknames() {
        return this == NICKNAMING;
    }

    public boolean isNightOwl() {
        return this == NIGHT_OWL;
    }

    public boolean isNoteTaker() {
        return this == NOTE_TAKER;
    }

    public boolean isNotebook() {
        return this == NOTEBOOK;
    }

    public boolean isObject() {
        return this == OBJECT;
    }

    public boolean isOrganizationalTendencies() {
        return this == ORGANIZATIONAL_TENDENCIES;
    }

    public boolean isOrganizer() {
        return this == ORGANIZER;
    }

    public boolean isOrigami() {
        return this == ORIGAMI;
    }

    public boolean isOverPlanner() {
        return this == OVER_PLANNER;
    }

    public boolean isOverExplainer() {
        return this == OVEREXPLAINER;
    }

    public boolean isPenClicker() {
        return this == PEN_CLICKER;
    }

    public boolean isPenTwirler() {
        return this == PEN_TWIRLER;
    }

    public boolean isPersonification() {
        return this == PERSONIFICATION;
    }

    public boolean isPessimist() {
        return this == PESSIMIST;
    }

    public boolean isPhrases() {
        return this == PHRASES;
    }

    public boolean isPlants() {
        return this == PLANTS;
    }

    public boolean isPolite() {
        return this == POLITE;
    }

    public boolean isPracticalJoker() {
        return this == PRACTICAL_JOKER;
    }

    public boolean isPrepared() {
        return this == PREPARED;
    }

    public boolean isPunctual() {
        return this == PUNCTUAL;
    }

    public boolean isPuzzles() {
        return this == PUZZLES;
    }

    public boolean isQuotes() {
        return this == QUOTES;
    }

    public boolean isRarelySleeps() {
        return this == RARELY_SLEEPS;
    }

    public boolean isRoutine() {
        return this == ROUTINE;
    }

    public boolean isSeeksApproval() {
        return this == SEEKS_APPROVAL;
    }

    public boolean isSentimental() {
        return this == SENTIMENTAL;
    }

    public boolean isSharpening() {
        return this == SHARPENING;
    }

    public boolean isSings() {
        return this == SINGS;
    }

    public boolean isSkeptical() {
        return this == SKEPTICAL;
    }

    public boolean isSleepTalker() {
        return this == SLEEP_TALKER;
    }

    public boolean isSmiler() {
        return this == SMILER;
    }

    public boolean isSnacks() {
        return this == SNACKS;
    }

    public boolean iStoryteller() {
        return this == STORYTELLING;
    }

    public boolean isStretching() {
        return this == STRETCHING;
    }

    public boolean isSuperstitiousRituals() {
        return this == SUPERSTITIOUS_RITUALS;
    }

    public boolean isSupervisedHabits() {
        return this == SUPERVISED_HABITS;
    }

    public boolean isTechTalk() {
        return this == TECH_TALK;
    }

    public boolean isTechnophobia() {
        return this == TECHNOPHOBIA;
    }

    public boolean isThesaurus() {
        return this == THESAURUS;
    }

    public boolean isThirdPerson() {
        return this == THIRD_PERSON;
    }

    public boolean isTimeManagement() {
        return this == TIME_MANAGEMENT;
    }

    public boolean isTinkerer() {
        return this == TINKERER;
    }

    public boolean isTruthTeller() {
        return this == TRUTH_TELLER;
    }

    public boolean isUnnecessaryCaution() {
        return this == UNNECESSARY_CAUTION;
    }

    public boolean isUnpredictableSpeech() {
        return this == UNPREDICTABLE_SPEECH;
    }

    public boolean isUnusualHobbies() {
        return this == UNUSUAL_HOBBIES;
    }

    public boolean isWatch() {
        return this == WATCH;
    }

    public boolean isWeatherman() {
        return this == WEATHERMAN;
    }

    public boolean isWhistler() {
        return this == WHISTLER;
    }

    public boolean isWorrier() {
        return this == WORRIER;
    }

    public boolean isWriter() {
        return this == WRITER;
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
                throw new IllegalStateException(
                        "Unexpected value in mekhq/campaign/personnel/enums/randomEvents/personalities/PersonalityQuirk.java/parseFromString: "
                                + quirk);
        };
    }

    @Override
    public String toString() {
        return name;
    }
}
