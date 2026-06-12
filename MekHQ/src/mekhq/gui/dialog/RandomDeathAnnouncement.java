/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.dialog;

import static mekhq.MHQConstants.NAG_SOMEONE_RANDOMLY_DIED_CAMP_FOLLOWER;
import static mekhq.MHQConstants.NAG_SOMEONE_RANDOMLY_DIED_CIVILIAN;
import static mekhq.MHQConstants.NAG_SOMEONE_RANDOMLY_DIED_COMBAT;
import static mekhq.MHQConstants.NAG_SOMEONE_RANDOMLY_DIED_OTHER_SUPPORT;
import static mekhq.MHQConstants.NAG_SOMEONE_RANDOMLY_DIED_RETIREE;
import static mekhq.MHQConstants.NAG_SOMEONE_RANDOMLY_DIED_TECH;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

import java.time.LocalDate;

import megamek.common.enums.SkillLevel;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.events.InterruptAdvanceMultipleDaysEvent;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.enums.PersonnelStatus;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogCore;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogNag;
import org.jspecify.annotations.NonNull;

public class RandomDeathAnnouncement extends ImmersiveDialogNag {
    private final static String RESOURCE_BUNDLE = "mekhq.resources.RandomDeathAnnouncement";

    private final Campaign campaign;

    public RandomDeathAnnouncement(Campaign campaign, Person deceased, PersonnelStatus causeOfDeath,
          String nagConstant) {
        super();
        this.campaign = campaign;

        ImmersiveDialogCore dialog = constructDialog(campaign, deceased, causeOfDeath, nagConstant);
        processDialogChoice(dialog.getDialogChoice(), nagConstant);
    }

    private ImmersiveDialogCore constructDialog(Campaign campaign, Person deceased, PersonnelStatus causeOfDeath,
          String nagConstant) {
        return new ImmersiveDialogCore(campaign,
              deceased,
              null,
              getInCharacterText(campaign, deceased, causeOfDeath),
              createButtons(),
              getOutOfCharacterText(nagConstant),
              null,
              true,
              null,
              null,
              true);
    }

    private String getInCharacterText(Campaign campaign, Person deceased, PersonnelStatus causeOfDeath) {
        LocalDate today = campaign.getLocalDate();
        String causeOfDeathLabel = causeOfDeath.getLogText();

        String primaryProfessionLabel = getPrimaryProfessionLabel(deceased, campaign);
        String secondaryProfessionLabel = getSecondaryProfessionLabel(deceased, campaign);

        String professions = primaryProfessionLabel +
                                   (secondaryProfessionLabel.isBlank() ? "" : ", " + secondaryProfessionLabel);

        if (deceased.getStatus().isCampFollower()) {
            return getCampFollowerDeathReport(deceased, campaign, causeOfDeathLabel, professions, today);
        } else {
            return getEmployeeDeathReport(deceased, campaign, causeOfDeathLabel, professions, today);
        }
    }

    private static @NonNull String getCampFollowerDeathReport(Person deceased, Campaign campaign,
          String causeOfDeathLabel,
          String professions, LocalDate today) {
        return getFormattedTextAt(RESOURCE_BUNDLE, "RandomDeathAnnouncement.inCharacter.unemployed",
              deceased.getHyperlinkedFullTitle(),
              causeOfDeathLabel,
              professions,
              deceased.getAge(today),
              deceased.getYearsSinceJoiningCampaign(campaign));
    }

    private static @NonNull String getEmployeeDeathReport(Person deceased, Campaign campaign, String causeOfDeathLabel,
          String professions, LocalDate today) {
        return getFormattedTextAt(RESOURCE_BUNDLE, "RandomDeathAnnouncement.inCharacter.employed",
              deceased.getHyperlinkedFullTitle(),
              causeOfDeathLabel,
              professions,
              deceased.getAge(today),
              deceased.getYearsSinceJoiningCampaign(campaign),
              deceased.getYearsInService(campaign),
              deceased.getTotalXPEarnings(),
              deceased.getTotalEarnings().toAmountString());
    }

    private static @NonNull String getPrimaryProfessionLabel(Person deceased, Campaign campaign) {
        String primaryProfession = deceased.getPrimaryRoleDesc();
        SkillLevel primarySkillLevel = deceased.getSkillLevel(campaign, false, true);
        String primarySkillLabel = SkillType.getColoredExperienceLevelName(primarySkillLevel);
        return primaryProfession + " (" + primarySkillLabel + ')';
    }

    private static String getSecondaryProfessionLabel(Person deceased, Campaign campaign) {
        PersonnelRole secondaryRole = deceased.getSecondaryRole();
        if (secondaryRole.isNone()) {
            return "";
        }

        String secondaryProfession = deceased.getSecondaryRoleDesc();
        SkillLevel secondarySkillLevel = deceased.getSkillLevel(campaign, true, true);
        String secondarySkillLabel = SkillType.getColoredExperienceLevelName(secondarySkillLevel);

        return secondaryProfession + " (" + secondarySkillLabel + ')';
    }

    private String getOutOfCharacterText(String nagConstant) {
        String messageKey = "RandomDeathAnnouncement.outOfCharacter";
        String classificationKey = messageKey + '.' + nagConstant;
        return getFormattedTextAt(RESOURCE_BUNDLE, "RandomDeathAnnouncement.outOfCharacter", classificationKey);
    }

    public static boolean checkNag(String nagConstant) {
        return !MekHQ.getMHQOptions().getNagDialogIgnore(nagConstant);
    }

    @Override
    protected void processDialogChoice(int choiceIndex, String nagConstant) {
        DialogChoice choice = DialogChoice.fromIndex(choiceIndex);

        switch (choice) {
            case CHOICE_CANCEL -> MekHQ.triggerEvent(new InterruptAdvanceMultipleDaysEvent(campaign));
            case CHOICE_CONTINUE -> cancelAdvanceDay = false;
            case CHOICE_SUPPRESS -> {
                MekHQ.getMHQOptions().setNagDialogIgnore(nagConstant, true);
                cancelAdvanceDay = false;
            }
            default -> throw new IllegalStateException("Unexpected value in ImmersiveDialogNag/processDialogChoice: " +
                                                             choiceIndex);
        }
    }

    public static String getRandomDeathAnnouncementNagConstant(Person deceased) {
        return switch (deceased) {
            case Person d when d.getStatus().isCampFollower() -> NAG_SOMEONE_RANDOMLY_DIED_CAMP_FOLLOWER;
            case Person d when d.getStatus().isRetired() -> NAG_SOMEONE_RANDOMLY_DIED_RETIREE;
            case Person d when d.isCombat() -> NAG_SOMEONE_RANDOMLY_DIED_COMBAT;
            case Person d when d.isTechExpanded() -> NAG_SOMEONE_RANDOMLY_DIED_TECH;
            case Person d when d.isSupport() -> NAG_SOMEONE_RANDOMLY_DIED_OTHER_SUPPORT;
            default -> NAG_SOMEONE_RANDOMLY_DIED_CIVILIAN;
        };
    }
}
