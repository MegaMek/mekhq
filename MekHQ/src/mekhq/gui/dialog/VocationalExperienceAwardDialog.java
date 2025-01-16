/*
 * Copyright (c) 2025 - The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.dialog;

import megamek.common.annotations.Nullable;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.gui.CampaignGUI;
import mekhq.gui.baseComponents.MHQDialogImmersive;

import java.util.List;
import java.util.ResourceBundle;
import java.util.UUID;

import static mekhq.campaign.Campaign.AdministratorSpecialization.HR;

public class VocationalExperienceAwardDialog extends MHQDialogImmersive {
    private static final String BUNDLE_KEY = "mekhq.resources.VocationalExperienceAwardDialog";
    private static final ResourceBundle resources = ResourceBundle.getBundle(
        BUNDLE_KEY, MekHQ.getMHQOptions().getLocale());

    public VocationalExperienceAwardDialog(Campaign campaign) {
        super(campaign, getSpeaker(campaign), null, createInCharacterMessage(campaign),
            createButtons(), createOutOfCharacterMessage(campaign), 0,
            null, null, null);

        setModal(false);
        setAlwaysOnTop(true);
    }

    @Override
    protected void handleHyperlinkClick(Campaign campaign, String href) {
        CampaignGUI campaignGUI = campaign.getApp().getCampaigngui();

        final UUID id = UUID.fromString(href.split(":")[1]);
        campaignGUI.focusOnPerson(id);
    }

    private static List<ButtonLabelTooltipPair> createButtons() {
        ButtonLabelTooltipPair btnConfirm = new ButtonLabelTooltipPair(
            resources.getString("confirm.button"), null);

        return List.of(btnConfirm);
    }

    private static @Nullable Person getSpeaker(Campaign campaign) {
        return campaign.getSeniorAdminPerson(HR);
    }

    private static String createInCharacterMessage(Campaign campaign) {
        List<Person> personnelWhoAdvanced = campaign.getPersonnelWhoAdvancedInXP();

        String commanderAddress = campaign.getCommanderAddress(false);

        StringBuilder message = new StringBuilder();
        message.append(commanderAddress);
        message.append(resources.getString("dialog.message"));

        for (Person person : personnelWhoAdvanced) {
            message.append("<br>- ").append(person.getHyperlinkedFullTitle());
        }

        return message.toString();
    }

    private static String createOutOfCharacterMessage(Campaign campaign) {
        int advancement = campaign.getCampaignOptions().getIdleXP();
        return String.format(resources.getString("dialog.ooc"), advancement);
    }
}
