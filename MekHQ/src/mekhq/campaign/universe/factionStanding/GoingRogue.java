package mekhq.campaign.universe.factionStanding;

import static megamek.common.Compute.randomInt;
import static mekhq.campaign.universe.factionStanding.FactionCensureEvent.POLITICAL_ROLES;
import static mekhq.campaign.universe.factionStanding.FactionCensureEvent.isExempt;
import static mekhq.campaign.universe.factionStanding.FactionCensureEvent.processMassLoyaltyChange;

import java.time.LocalDate;
import java.util.Collection;

import megamek.common.Compute;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelStatus;
import mekhq.campaign.universe.Faction;
import mekhq.gui.dialog.factionStanding.factionJudgment.FactionCensureGoingRogueDialog;

public class GoingRogue {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.FactionCensureDialog";

    private final static int LOYALTY_TARGET_NUMBER = 6;
    private final static int MURDER_DIE_SIZE = 10;

    private final Campaign campaign;
    private final boolean wasConfirmed;

    public boolean isWasConfirmed() {
        return wasConfirmed;
    }

    public GoingRogue(Campaign campaign, Person commander, Person second) {
        this.campaign = campaign;

        FactionCensureGoingRogueDialog dialog = new FactionCensureGoingRogueDialog(campaign);
        wasConfirmed = dialog.wasConfirmed();
        if (!wasConfirmed) {
            return;
        }

        Faction chosenFaction = dialog.getChosenFaction();
        processGoingRogue(chosenFaction, commander, second);
    }

    private void processGoingRogue(Faction chosenFaction, Person commander, Person second) {
        processPersonnel(commander, second);
        processMassLoyaltyChange(campaign, true, true);

        processFactionStandingChangeForOldFaction();
        processFactionStandingChangeForNewFaction(chosenFaction);

        campaign.setFaction(chosenFaction);
    }

    private void processPersonnel(Person commander, Person second) {
        final LocalDate today = campaign.getLocalDate();
        Collection<Person> allPersonnel = campaign.getPersonnel();
        for (Person person : allPersonnel) {
            if (isExempt(person, today)) {
                continue;
            }

            if (person.equals(commander) || person.equals(second)) {
                continue;
            }

            if (POLITICAL_ROLES.contains(person.getPrimaryRole())
                      || POLITICAL_ROLES.contains(person.getSecondaryRole())) {
                person.changeStatus(campaign, today, PersonnelStatus.HOMICIDE);
                continue;
            }

            int loyalty = person.getLoyalty();
            int modifier = person.getLoyaltyModifier(loyalty);
            int roll = Compute.d6(2);

            if (roll < (LOYALTY_TARGET_NUMBER + modifier)) {
                person.changeStatus(campaign, today, PersonnelStatus.LEFT);
            } else {
                roll = randomInt(MURDER_DIE_SIZE);
                if (roll == 0) {
                    person.changeStatus(campaign, today, PersonnelStatus.HOMICIDE);
                }
            }
        }
    }

    private void processFactionStandingChangeForOldFaction() {
        Faction faction = campaign.getFaction();

        if (faction.isAggregate()) {
            return;
        }

        String factionCode = faction.getShortName();
        FactionStandings factionStandings = campaign.getFactionStandings();

        double targetRegard = FactionStandingLevel.STANDING_LEVEL_1.getMinimumRegard();
        double currentRegard = factionStandings.getRegardForFaction(factionCode, false);
        if (currentRegard < targetRegard) {
            return;
        }

        String report = factionStandings.setRegardForFaction(factionCode, targetRegard, campaign.getGameYear(), true);
        campaign.addReport(report);
    }

    private void processFactionStandingChangeForNewFaction(Faction newFaction) {
        if (newFaction.isAggregate()) {
            return;
        }

        String factionCode = newFaction.getShortName();
        FactionStandings factionStandings = campaign.getFactionStandings();

        double targetRegard = FactionStandingLevel.STANDING_LEVEL_4.getMaximumRegard();
        double currentRegard = factionStandings.getRegardForFaction(factionCode, false);
        if (currentRegard >= targetRegard) {
            return;
        }

        String report = factionStandings.setRegardForFaction(factionCode, targetRegard, campaign.getGameYear(), true);
        campaign.addReport(report);
    }
}
