package mekhq.gui.dialog.factionStanding.factionJudgment;

import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JLabel;
import javax.swing.JPanel;

import megamek.client.ui.comboBoxes.MMComboBox;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.FactionHints;
import mekhq.campaign.universe.Factions;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogCore;

public class FactionCensureGoingRogueDialog {
    static MMLogger LOGGER = MMLogger.create(FactionCensureGoingRogueDialog.class);
    private static final String RESOURCE_BUNDLE = "mekhq.resources.FactionCensureDialog";

    private static final int CONFIRMED_DIALOG_INDEX = 1;

    private final Campaign campaign;
    private final List<Faction> possibleFactions = new ArrayList<>();
    private final boolean wasConfirmed;
    private final Faction chosenFaction;

    public boolean wasConfirmed() {
        return wasConfirmed;
    }

    public Faction getChosenFaction() {
        return chosenFaction;
    }

    public FactionCensureGoingRogueDialog(Campaign campaign) {
        this.campaign = campaign;

        getPossibleFactions();

        ImmersiveDialogCore dialog = new ImmersiveDialogCore(campaign,
              getSpeaker(),
              null,
              getInCharacterText(),
              getButtons(),
              getOutOfCharacterText(),
              null,
              false,
              getFactionPanel(),
              null,
              true);

        wasConfirmed = dialog.getDialogChoice() == CONFIRMED_DIALOG_INDEX;
        chosenFaction = possibleFactions.get(dialog.getComboBoxChoiceIndex());
    }

    private Person getSpeaker() {
        return campaign.getSeniorAdminPerson(Campaign.AdministratorSpecialization.COMMAND);
    }

    private String getInCharacterText() {
        return getFormattedTextAt(RESOURCE_BUNDLE, "FactionCensureGoingRogueDialog.inCharacter",
              campaign.getCommanderAddress(false));
    }

    private List<ImmersiveDialogCore.ButtonLabelTooltipPair> getButtons() {
        return List.of(
              new ImmersiveDialogCore.ButtonLabelTooltipPair(
                    getTextAt(RESOURCE_BUNDLE, "FactionCensureDialog.button.cancel"), null),
              new ImmersiveDialogCore.ButtonLabelTooltipPair(
                    getTextAt(RESOURCE_BUNDLE, "FactionCensureDialog.button.confirm"), null)
        );
    }

    private String getOutOfCharacterText() {
        return getTextAt(RESOURCE_BUNDLE, "FactionCensureGoingRogueDialog.outOfCharacter");
    }

    private void getPossibleFactions() {
        Faction campaignFaction = campaign.getFaction();
        LocalDate today = campaign.getLocalDate();

        Factions factions = Factions.getInstance();
        Collection<Faction> activeFactions = factions.getActiveFactions(today);

        Faction mercenaries = factions.getFaction("MERC");
        if (!campaignFaction.equals(mercenaries)) {
            possibleFactions.add(mercenaries);
        }

        Faction pirates = factions.getFaction("PIR");
        if (!campaignFaction.equals(pirates)) {
            possibleFactions.add(pirates);
        }

        FactionHints factionHints = FactionHints.defaultFactionHints();
        for (Faction otherFaction : activeFactions) {
            if (factionHints.isAtWarWith(campaignFaction, otherFaction, today)) {
                possibleFactions.add(otherFaction);
                LOGGER.debug("Faction {} is at war with {}",
                      otherFaction.getFullName(campaign.getGameYear()),
                      campaignFaction.getFullName(campaign.getGameYear()));
                continue;
            }

            if (factionHints.isRivalOf(otherFaction, campaignFaction, today)) {
                LOGGER.debug("Faction {} is rival of {}",
                      otherFaction.getFullName(campaign.getGameYear()),
                      campaignFaction.getFullName(campaign.getGameYear()));
                possibleFactions.add(otherFaction);
            }

            LOGGER.debug("Faction {} is not at war with {}",
                  otherFaction.getFullName(campaign.getGameYear()),
                  campaignFaction.getFullName(campaign.getGameYear()));
        }

        LOGGER.debug("Possible factions: {}", possibleFactions);
    }

    private JPanel getFactionPanel() {
        JPanel factionPanel = new JPanel();
        JLabel lblFactions = new JLabel("Possible Factions:");
        MMComboBox<String> cmboFactions = new MMComboBox<>("choicePerson", createPersonGroupModel());

        factionPanel.add(lblFactions);
        factionPanel.add(cmboFactions);

        return factionPanel;
    }

    private DefaultComboBoxModel<String> createPersonGroupModel() {
        final int gameYear = campaign.getGameYear();
        final DefaultComboBoxModel<String> factionModel = new DefaultComboBoxModel<>();
        for (Faction faction : possibleFactions) {
            factionModel.addElement(faction.getFullName(gameYear));
        }
        return factionModel;
    }
}
