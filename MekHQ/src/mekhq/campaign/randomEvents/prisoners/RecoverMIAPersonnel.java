package mekhq.campaign.randomEvents.prisoners;

import megamek.common.ITechnology;
import megamek.common.TargetRoll;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelStatus;
import mekhq.campaign.universe.Faction;

import static megamek.common.Compute.d6;
import static megamek.common.MiscType.createBeagleActiveProbe;
import static megamek.common.MiscType.createCLImprovedSensors;
import static megamek.common.MiscType.createISImprovedSensors;
import static mekhq.campaign.parts.enums.PartQuality.QUALITY_D;

public class RecoverMIAPersonnel {
    private final Campaign campaign;

    // SAR Modifiers (based on CamOps pg 223)
    final int SAR_CONTAINS_VTOL_OR_WIGE = 1;
    final int SAR_HAS_IMPROVED_SENSORS = 2; // largest only
    final int SAR_HAS_ACTIVE_PROBE = 1; // largest only
    private TargetRoll sarTargetNumber = new TargetRoll(8, "Base TN"); // Target Number (CamOps pg 223)

    public RecoverMIAPersonnel(Campaign campaign, Faction searchingFaction, Integer sarQuality) {
        this.campaign = campaign;

        int today = campaign.getLocalDate().getYear();
        boolean isClan = searchingFaction != null && searchingFaction.isClan();

        int techFaction = isClan ? ITechnology.getCodeFromMMAbbr("CLAN") : ITechnology.getCodeFromMMAbbr("IS");
        try {
            // searchingFaction being null is fine because we're just ignoring any exceptions
            techFaction = ITechnology.getCodeFromMMAbbr(searchingFaction.getShortName());
        } catch (Exception ignored) {
            // if we can't get the tech faction, we just use the fallbacks already assigned.
        }

        sarTargetNumber.addModifier(SAR_CONTAINS_VTOL_OR_WIGE, "SAR Contains VTOL or WIGE");

        final int isImprovedSensorsAvailability = createISImprovedSensors().calcYearAvailability(
            today, isClan, techFaction);
        final int clanImprovedSensorsAvailability = createCLImprovedSensors().calcYearAvailability(
            today, isClan, techFaction);

        final int improvedSensorsAvailability = isClan ? clanImprovedSensorsAvailability : isImprovedSensorsAvailability;

        final int activeProbeAvailability = createBeagleActiveProbe().calcYearAvailability(
            today, isClan, techFaction);

        if (sarQuality == null) {
            sarQuality = QUALITY_D.ordinal();
        }

        if (sarQuality >= improvedSensorsAvailability) {
            sarTargetNumber.addModifier(SAR_HAS_IMPROVED_SENSORS, "SAR has Improved Sensors");
        } else if (sarQuality >= activeProbeAvailability) {
            sarTargetNumber.addModifier(SAR_HAS_ACTIVE_PROBE, "SAR has Active Probe");
        }
    }

    public void attemptRescueOfPlayerCharacter(Person missingPerson) {
        int targetNumber = sarTargetNumber.getValue();
        int roll = d6(2);

        boolean wasRescued = roll >= targetNumber;

        if (wasRescued) {
            missingPerson.changeStatus(campaign, campaign.getLocalDate(), PersonnelStatus.POW);
        }
    }
}
