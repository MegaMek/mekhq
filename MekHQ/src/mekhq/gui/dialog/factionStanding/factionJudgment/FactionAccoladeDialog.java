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

import static mekhq.campaign.universe.factionStanding.FactionAccoladeLevel.ADOPTION_OR_MEKS;
import static mekhq.campaign.universe.factionStanding.FactionAccoladeLevel.APPEARING_IN_SEARCHES;
import static mekhq.campaign.universe.factionStanding.FactionAccoladeLevel.CASH_BONUS;
import static mekhq.campaign.universe.factionStanding.FactionAccoladeLevel.TRIUMPH_OR_REMEMBRANCE;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.util.ArrayList;
import java.util.List;

import megamek.common.annotations.Nullable;
import megamek.common.enums.Gender;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Bloodname;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.enums.Phenotype;
import mekhq.campaign.personnel.ranks.RankSystem;
import mekhq.campaign.personnel.ranks.RankValidator;
import mekhq.campaign.personnel.ranks.Ranks;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Factions;
import mekhq.campaign.universe.factionStanding.FactionAccoladeLevel;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogSimple;
import mekhq.utilities.MHQInternationalization;

public class FactionAccoladeDialog {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.FactionAccoladeDialog";

    private final static String BUTTON_KEY = "FactionAccoladeDialog.button.";
    private final static String BUTTON_AFFIX_POSITIVE = "positive.";
    private final static String BUTTON_AFFIX_NEUTRAL = "neutral.";
    private final static String BUTTON_AFFIX_NEGATIVE = "negative.";

    private final static String DIALOG_KEY_MESSAGE = "FactionAccoladeDialog.message.";
    private final static String DIALOG_AFFIX_INNER_SPHERE = "innerSphere";
    private final static String DIALOG_AFFIX_CLAN = "clan";
    private final static String DIALOG_AFFIX_PERIPHERY = "periphery";
    private final static String DIALOG_AFFIX_ADOPTION = ".adoption";
    private final static String DIALOG_AFFIX_COMPANY = ".company";

    private final static String MOC_SPECIAL_CASE_FIRST_NAME = "Mia";
    private final static String MOC_SPECIAL_CASE_SURNAME = "Meklove";

    private final static int DIALOG_CHOICE_REFUSE = 2;

    private final Campaign campaign;
    private final Faction faction;
    private final boolean wasRefused;

    public boolean wasRefused() {
        return wasRefused;
    }

    public FactionAccoladeDialog(Campaign campaign, Faction faction, FactionAccoladeLevel accoladeLevel) {
        this.campaign = campaign;
        this.faction = faction;

        boolean isSameFaction = campaign.getFaction().equals(faction);

        ImmersiveDialogSimple dialog = new ImmersiveDialogSimple(campaign,
              getSpeaker(accoladeLevel, isSameFaction),
                null,
                getInCharacterMessage(accoladeLevel, isSameFaction),
                getButtons(accoladeLevel, isSameFaction),
              null,
              null,
              true);

        wasRefused = dialog.getDialogChoice() == DIALOG_CHOICE_REFUSE;
    }

    private @Nullable Person getSpeaker(FactionAccoladeLevel accoladeLevel, boolean isSameFaction) {
        if (accoladeLevel.is(APPEARING_IN_SEARCHES)) {
            return campaign.getSecondInCommand();
        }

        String factionCode = faction.getShortName();

        PersonnelRole primaryRole = faction.isClan()
                                          ? PersonnelRole.MEKWARRIOR
                                          : (accoladeLevel.is(CASH_BONUS)
                                                   ? PersonnelRole.MILITARY_LIAISON
                                                   : PersonnelRole.NOBLE);
        boolean isMOCSpecialCase = faction.getShortName().equals("MOC")
                                         && accoladeLevel.is(ADOPTION_OR_MEKS) ||
                                         accoladeLevel.is(TRIUMPH_OR_REMEMBRANCE)
                                               && isSameFaction;

        if (isMOCSpecialCase) {
            primaryRole = PersonnelRole.HOLO_STAR;
        }

        Person speaker = campaign.newPerson(primaryRole, factionCode, Gender.RANDOMIZE);
        if (isMOCSpecialCase) {
            speaker.setGivenName(MOC_SPECIAL_CASE_FIRST_NAME);
            speaker.setSurname(MOC_SPECIAL_CASE_SURNAME);
        }

        // Clan-specific attributes
        if (faction.isClan()) {
            Bloodname bloodname = Bloodname.randomBloodname(factionCode, Phenotype.MEKWARRIOR, campaign.getGameYear());
            if (bloodname != null) {
                speaker.setBloodname(bloodname.getName());
            }
        }

        // Determine rank system
        RankSystem rankSystem;
        if (faction.isClan()) {
            rankSystem = Ranks.getRankSystemFromCode("CLAN");
        } else {
            rankSystem = faction.getRankSystem();
        }

        // Validate and set the rank system
        RankValidator rankValidator = new RankValidator();
        if (rankValidator.validate(rankSystem, false)) {
            speaker.setRankSystem(rankValidator, rankSystem);
            speaker.setRank(38);
        }

        return speaker;
    }

    private String getInCharacterMessage(FactionAccoladeLevel accoladeLevel, boolean isSameFaction) {
        String messageKey = getMessageKey(accoladeLevel, isSameFaction);
        return getFormattedTextAt(RESOURCE_BUNDLE, messageKey, campaign.getCommanderAddress(false),
              campaign.getName(), faction.getFullName(campaign.getGameYear()),
              campaign.getLocation().getPlanet().getName(campaign.getLocalDate()));
    }

    private String getMessageKey(FactionAccoladeLevel accoladeLevel, boolean isSameFaction) {
        String factionCode = faction.getShortName();

        String key = DIALOG_KEY_MESSAGE + accoladeLevel.name() + '.' + factionCode;
        if (accoladeLevel.is(ADOPTION_OR_MEKS)) {
            key += isSameFaction ? DIALOG_AFFIX_COMPANY : DIALOG_AFFIX_ADOPTION;
        }

        String testReturn = getTextAt(RESOURCE_BUNDLE, key);
        if (MHQInternationalization.isResourceKeyValid(testReturn)) {
            return key;
        }

        Faction faction = Factions.getInstance().getFaction(factionCode);
        String affix = DIALOG_AFFIX_INNER_SPHERE;
        if (faction != null) {
            if (faction.isClan()) {
                affix = DIALOG_AFFIX_CLAN;
            } else if (faction.isPeriphery()) {
                affix = DIALOG_AFFIX_PERIPHERY;
            }
        }

        return key.replace(accoladeLevel.name() + '.' + factionCode,
              accoladeLevel.name() + '.' + affix);
    }

    private List<String> getButtons(FactionAccoladeLevel accoladeLevel, boolean isSameFaction) {
        List<String> buttonLabels = new ArrayList<>();

        String affix = "";
        if (accoladeLevel.is(ADOPTION_OR_MEKS)) {
            affix = isSameFaction ? DIALOG_AFFIX_COMPANY : DIALOG_AFFIX_ADOPTION;
        }

        buttonLabels.add(getTextAt(RESOURCE_BUNDLE, BUTTON_KEY +
                                                          BUTTON_AFFIX_POSITIVE +
                                                          accoladeLevel.name() +
                                                          affix));
        buttonLabels.add(getTextAt(RESOURCE_BUNDLE, BUTTON_KEY +
                                                          BUTTON_AFFIX_NEUTRAL +
                                                          accoladeLevel.name() +
                                                          affix));
        buttonLabels.add(getTextAt(RESOURCE_BUNDLE, BUTTON_KEY +
                                                          BUTTON_AFFIX_NEGATIVE +
                                                          accoladeLevel.name() +
                                                          affix));

        return buttonLabels;
    }
}
