/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.dialog.factionStanding.factionJudgment;

import static megamek.common.enums.Gender.FEMALE;
import static megamek.common.enums.Gender.RANDOMIZE;
import static mekhq.campaign.personnel.enums.PersonnelRole.MEKWARRIOR;
import static mekhq.campaign.personnel.enums.PersonnelRole.MILITARY_LIAISON;
import static mekhq.campaign.personnel.enums.PersonnelRole.NOBLE;
import static mekhq.campaign.universe.factionStanding.FactionAccoladeLevel.*;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.util.ArrayList;
import java.util.List;

import megamek.common.annotations.Nullable;
import megamek.common.universe.FactionLeaderData;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Bloodname;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.PronounData;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.enums.Phenotype;
import mekhq.campaign.personnel.ranks.RankSystem;
import mekhq.campaign.personnel.ranks.RankValidator;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Factions;
import mekhq.campaign.universe.factionStanding.FactionAccoladeLevel;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogSimple;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogWidth;
import mekhq.gui.dialog.NewsDialog;
import mekhq.utilities.MHQInternationalization;

public class FactionAccoladeDialog {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.FactionAccoladeDialog";

    private final static String BUTTON_KEY = "FactionAccoladeDialog.button.";
    private final static String AFFIX_POSITIVE = "positive.";
    private final static String AFFIX_NEUTRAL = "neutral.";
    private final static String AFFIX_NEGATIVE = "negative.";

    private final static String MESSAGE_KEY = "FactionAccoladeDialog.message.";
    private final static String AFFIX_OOC = ".ooc";
    private final static String AFFIX_INNER_SPHERE = ".innerSphere";
    private final static String AFFIX_CLAN = ".clan";
    private final static String AFFIX_PERIPHERY = ".periphery";
    private final static String AFFIX_ADOPTION = ".adoption";
    private final static String AFFIX_COMPANY = ".company";
    private final static String AFFIX_INTRO = ".intro";

    private final static String MOC_SPECIAL_CASE_FIRST_NAME = "Mia";
    private final static String MOC_SPECIAL_CASE_SURNAME = "Meklove";

    private final static int CASH_MULTIPLIER = 5;
    private final static int DIALOG_CHOICE_REFUSE = 2;

    private final Campaign campaign;
    private final Faction faction;
    private boolean wasRefused = false;

    public boolean wasRefused() {
        return wasRefused;
    }

    public FactionAccoladeDialog(Campaign campaign, Faction faction, FactionAccoladeLevel accoladeLevel,
          Person commander) {
        this.campaign = campaign;
        this.faction = faction;

        // First we check for an introduction message
        processMessageIntroductionIfApplicable(accoladeLevel);

        boolean isSameFaction = campaign.getFaction().equals(faction);

        // Some accolades use a news article format
        if (processNewsArticleIfApplicable(accoladeLevel, isSameFaction, commander)) {
            return;
        }

        // Otherwise, we display a normal communication
        Person speaker = getSpeaker(accoladeLevel, isSameFaction);
        String inCharacterMessage = getInCharacterMessage(accoladeLevel, isSameFaction, commander);
        List<String> buttons = getButtons(accoladeLevel, isSameFaction);
        String outOfCharacterMessage = getOutOfCharacterMessage(accoladeLevel, isSameFaction);
        boolean isUseNarrowDisplay = accoladeLevel.is(APPEARING_IN_SEARCHES)
                                           || accoladeLevel.is(CASH_BONUS_0)
                                           || accoladeLevel.is(CASH_BONUS_1)
                                           || accoladeLevel.is(CASH_BONUS_2)
                                           || accoladeLevel.is(CASH_BONUS_3)
                                           || accoladeLevel.is(CASH_BONUS_4);
        ImmersiveDialogWidth width = isUseNarrowDisplay ? ImmersiveDialogWidth.MEDIUM : ImmersiveDialogWidth.LARGE;

        ImmersiveDialogSimple accoladeDialog = new ImmersiveDialogSimple(campaign, speaker, null, inCharacterMessage,
              buttons, outOfCharacterMessage, null, true, width);

        wasRefused = accoladeDialog.getDialogChoice() == DIALOG_CHOICE_REFUSE;
    }

    private boolean processNewsArticleIfApplicable(FactionAccoladeLevel accoladeLevel, boolean isSameFaction,
          Person commander) {
        boolean isClan = faction.isClan();
        boolean isPressRecognition = accoladeLevel.is(PRESS_RECOGNITION);
        boolean isPropagandaReel = accoladeLevel.is(PROPAGANDA_REEL);
        boolean isTriumphOrRemembrance = accoladeLevel.is(TRIUMPH_OR_REMEMBRANCE);
        boolean isStatueOrSibko = accoladeLevel.is(STATUE_OR_SIBKO);
        boolean isMOC = faction.getShortName().equals("MOC");
        boolean isComStarOrWoB = faction.isComStarOrWoB();

        boolean triggerNewsArticle =
              (!isClan && isPressRecognition)
                    || isPropagandaReel
                    || (isTriumphOrRemembrance && !isMOC)
                    || (isStatueOrSibko && !isMOC && !isComStarOrWoB);

        if (triggerNewsArticle) {
            String message = getInCharacterMessage(accoladeLevel, isSameFaction, commander);
            new NewsDialog(campaign, message);
            return true;
        }

        return false;
    }

    private void processMessageIntroductionIfApplicable(FactionAccoladeLevel accoladeLevel) {
        List<FactionAccoladeLevel> accoladeLevelsWithIntroductions = List.of(PRESS_RECOGNITION, PROPAGANDA_REEL,
              TRIUMPH_OR_REMEMBRANCE, STATUE_OR_SIBKO);
        if (accoladeLevelsWithIntroductions.contains(accoladeLevel)) {
            String dialogKey = MESSAGE_KEY +
                                     accoladeLevel.getLookupName() +
                                     AFFIX_INTRO +
                                     (faction.isClan() ? AFFIX_CLAN : AFFIX_INNER_SPHERE);
            String message = getFormattedTextAt(RESOURCE_BUNDLE, dialogKey, campaign.getCommanderAddress(false),
                  faction.getFullName(campaign.getGameYear()));

            String buttonKey = BUTTON_KEY + accoladeLevel.getLookupName() + AFFIX_INTRO;
            String buttonLabel = getTextAt(RESOURCE_BUNDLE, buttonKey);

            new ImmersiveDialogSimple(campaign, campaign.getSecondInCommand(), null, message, List.of(buttonLabel),
                  null, null, false);
        }
    }

    private @Nullable Person getSpeaker(FactionAccoladeLevel accoladeLevel, boolean isSameFaction) {
        boolean isClan = faction.isClan();

        return switch (accoladeLevel) {
            case NO_ACCOLADE, TAKING_NOTICE_0, TAKING_NOTICE_1, PRESS_RECOGNITION, PROPAGANDA_REEL -> null;
            case APPEARING_IN_SEARCHES -> campaign.getSecondInCommand();
            case CASH_BONUS_0, CASH_BONUS_1, CASH_BONUS_2, CASH_BONUS_3, CASH_BONUS_4, ADOPTION_OR_MEKS -> {
                if (isClan) {
                    yield generateClanCharacter(true, true);
                } else {
                    yield generateInnerSphereCharacter(accoladeLevel, isSameFaction, true, false);
                }
            }
            case TRIUMPH_OR_REMEMBRANCE, STATUE_OR_SIBKO -> {
                if (isClan) {
                    yield generateClanCharacter(true, true);
                } else {
                    if (faction.getShortName().equals("MOC")) {
                        yield generateInnerSphereCharacter(accoladeLevel, isSameFaction, true, false);
                    } else {
                        yield null;
                    }
                }
            }
            case LETTER_FROM_HEAD_OF_STATE -> {
                Person speaker;
                if (faction.isClan()) {
                    speaker = generateClanCharacter(false, false);
                } else {
                    speaker = generateInnerSphereCharacter(accoladeLevel, isSameFaction, false, true);
                }

                FactionLeaderData leaderData = faction.getLeaderForYear(campaign.getGameYear());
                if (leaderData == null) {
                    RankSystem rankSystem = faction.getRankSystem();

                    RankValidator rankValidator = new RankValidator();
                    if (rankValidator.validate(rankSystem, false)) {
                        speaker.setRankSystem(rankValidator, rankSystem);
                        speaker.setRank(48);
                    }
                } else {
                    speaker.setGivenName(leaderData.title() + ' ' + leaderData.firstName());

                    String surname = leaderData.surname() + ' ' + leaderData.honorific();
                    if (isClan) {
                        speaker.setBloodname(surname);
                    } else {
                        speaker.setSurname(surname);
                    }

                    speaker.setGender(leaderData.gender());
                }

                yield speaker;
            }
        };
    }

    private Person generateInnerSphereCharacter(FactionAccoladeLevel accoladeLevel, boolean isSameFaction,
          boolean includeRank, boolean isNoble) {
        String factionCode = faction.getShortName();

        boolean isMOCSpecialCase = factionCode.equals("MOC") &&
                                         isSameFaction &&
                                         (accoladeLevel.is(ADOPTION_OR_MEKS) ||
                                                accoladeLevel.is(TRIUMPH_OR_REMEMBRANCE) ||
                                                accoladeLevel.is(STATUE_OR_SIBKO));

        PersonnelRole personnelRole = isMOCSpecialCase ? MILITARY_LIAISON : MEKWARRIOR;
        if (isNoble) {
            personnelRole = NOBLE;
        }

        Person speaker = campaign.newPerson(personnelRole, factionCode, isMOCSpecialCase ? FEMALE : RANDOMIZE);

        if (isMOCSpecialCase) {
            speaker.setGivenName(MOC_SPECIAL_CASE_FIRST_NAME);
            speaker.setSurname(MOC_SPECIAL_CASE_SURNAME);
        }

        if (includeRank) {
            RankSystem rankSystem = faction.getRankSystem();

            RankValidator rankValidator = new RankValidator();
            if (rankValidator.validate(rankSystem, false)) {
                speaker.setRankSystem(rankValidator, rankSystem);
                speaker.setRank(38);
            }
        }

        return speaker;
    }

    private Person generateClanCharacter(boolean includeBloodname, boolean includeRank) {
        String factionCode = faction.getShortName();
        Person character = campaign.newPerson(MEKWARRIOR, factionCode, RANDOMIZE);

        if (includeBloodname) {
            Bloodname bloodname = Bloodname.randomBloodname(factionCode, Phenotype.MEKWARRIOR, campaign.getGameYear());
            if (bloodname != null) {
                character.setBloodname(bloodname.getName());
            }
        }

        if (includeRank) {
            RankSystem rankSystem = faction.getRankSystem();

            RankValidator rankValidator = new RankValidator();
            if (rankValidator.validate(rankSystem, false)) {
                character.setRankSystem(rankValidator, rankSystem);
                character.setRank(38);
            }
        }

        return character;
    }

    private String getInCharacterMessage(FactionAccoladeLevel accoladeLevel, boolean isSameFaction, Person commander) {
        String messageKey = getMessageKey(accoladeLevel, isSameFaction);

        final PronounData commanderPronounData = new PronounData(commander.getGender());

        // {0} hyperlinked full title
        final String commanderHyperlinkedFullTitle = commander.getHyperlinkedFullTitle();
        // {1} first name
        final String commanderFirstName = commander.getGivenName();
        // {2} = He/She/They
        final String commanderHeSheTheyCapitalized = commanderPronounData.subjectPronoun();
        // {3} = he/she/they
        final String commanderHeSheTheyLowercase = commanderPronounData.subjectPronounLowerCase();
        // {4} = Him/Her/Them
        final String commanderHimHerThemCapitalized = commanderPronounData.objectPronoun();
        // {5} = him/her/them
        final String commanderHimHerThemLowercase = commanderPronounData.objectPronounLowerCase();
        // {6} = His/Her/Their
        final String commanderHisHerTheirCapitalized = commanderPronounData.possessivePronoun();
        // {7} = his/her/their
        final String commanderHisHerTheirLowercase = commanderPronounData.possessivePronounLowerCase();
        // {8} = Gender Neutral = 0, Otherwise 1 (used to determine whether to use a plural case)
        final int commanderPluralizer = commanderPronounData.pluralizer();
        // {9} = Commander Address
        final String commanderAddress = campaign.getCommanderAddress(false);
        // {10} = Faction Name
        final String factionName = faction.getFullName(campaign.getGameYear());
        // {11} = Current System
        final String currentSystem = campaign.getLocation().getPlanet().getName(campaign.getLocalDate());
        // {12} = Campaign Name
        final String campaignName = campaign.getName();
        // {13} = Cash Value
        final int cashValue = accoladeLevel.getRecognition() * CASH_MULTIPLIER;

        return getFormattedTextAt(RESOURCE_BUNDLE, messageKey, commanderHyperlinkedFullTitle, commanderFirstName,
              commanderHeSheTheyCapitalized, commanderHeSheTheyLowercase, commanderHimHerThemCapitalized,
              commanderHimHerThemLowercase, commanderHisHerTheirCapitalized, commanderHisHerTheirLowercase,
              commanderPluralizer, commanderAddress, factionName, currentSystem, campaignName, cashValue);
    }

    private String getMessageKey(FactionAccoladeLevel accoladeLevel, boolean isSameFaction) {
        String factionCode = faction.getShortName();

        String key = MESSAGE_KEY + accoladeLevel.getLookupName() + '.' + factionCode;
        if (accoladeLevel.is(ADOPTION_OR_MEKS)) {
            key += isSameFaction ? AFFIX_COMPANY : AFFIX_ADOPTION;
        }

        String testReturn = getTextAt(RESOURCE_BUNDLE, key);
        if (MHQInternationalization.isResourceKeyValid(testReturn)) {
            return key;
        }

        Faction faction = Factions.getInstance().getFaction(factionCode);
        String affix = AFFIX_INNER_SPHERE;
        if (faction != null) {
            if (faction.isClan()) {
                affix = AFFIX_CLAN;
            } else if (faction.isPeriphery()) {
                affix = AFFIX_PERIPHERY;
            }
        }

        return key.replace(accoladeLevel.getLookupName() + '.' + factionCode,
              accoladeLevel.getLookupName() + affix);
    }

    private List<String> getButtons(FactionAccoladeLevel accoladeLevel, boolean isSameFaction) {
        List<String> buttonLabels = new ArrayList<>();

        String affix = "";
        if (accoladeLevel.is(ADOPTION_OR_MEKS)) {
            affix = isSameFaction ? AFFIX_COMPANY : AFFIX_ADOPTION;
        }

        buttonLabels.add(getTextAt(RESOURCE_BUNDLE, BUTTON_KEY +
                                                          AFFIX_POSITIVE +
                                                          accoladeLevel.getLookupName() +
                                                          affix));
        buttonLabels.add(getTextAt(RESOURCE_BUNDLE, BUTTON_KEY +
                                                          AFFIX_NEUTRAL +
                                                          accoladeLevel.getLookupName() +
                                                          affix));
        buttonLabels.add(getTextAt(RESOURCE_BUNDLE, BUTTON_KEY +
                                                          AFFIX_NEGATIVE +
                                                          accoladeLevel.getLookupName() +
                                                          affix));

        return buttonLabels;
    }

    private String getOutOfCharacterMessage(FactionAccoladeLevel accoladeLevel, boolean isSameFaction) {
        String messageKey = MESSAGE_KEY + accoladeLevel.getLookupName() + AFFIX_OOC;

        if (accoladeLevel.is(ADOPTION_OR_MEKS)) {
            messageKey += isSameFaction ? AFFIX_COMPANY : AFFIX_ADOPTION;
        }

        return getFormattedTextAt(RESOURCE_BUNDLE, messageKey, faction.getFullName(campaign.getGameYear()));
    }
}
