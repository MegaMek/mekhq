package mekhq.campaign.personnel.autoMedals;

import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Award;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.SkillType;
import org.apache.logging.log4j.LogManager;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

public class SkillAwards {
    /**
     * This function loops through Skill Awards, checking whether the person is eligible to receive each type of award
     *
     * @param campaign the campaign to be processed
     * @param awards   the awards to be processed (should only include awards where item == Skill)
     * @param person   the person to check award eligibility for
     */
    public SkillAwards(Campaign campaign, List<Award> awards, Person person) {
        final ResourceBundle resource = ResourceBundle.getBundle("mekhq.resources.AutoAwards",
                MekHQ.getMHQOptions().getLocale());

        for (Award award : awards) {
            if (award.canBeAwarded(person)) {
                if (getSkillLevel(award, person, award.getSize()) > award.getQty()) {
                    // we have to include ' ' as hyperlinked names lose their hyperlink if used within resource.getString
                    campaign.addReport(person.getHyperlinkedName() + ' ' +
                            MessageFormat.format(resource.getString("EligibleForAwardReport.format"),
                                    award.getName(), award.getSet()));
                }
            }
        }
    }

    /**
     * This function uses switches to cycle through skills to find the relevant skill level
     *
     * @param award the award being processed, this is for error logging
     * @param person the person whose skill levels we want
     * @param skill the skill we're checking
     */
    private int getSkillLevel(Award award, Person person, String skill) {
        switch (skill) {
            case "Piloting":
                int[] pilotingLevels = new int[7];

                pilotingLevels[0] = person.getSkillLevel(SkillType.S_PILOT_MECH);
                pilotingLevels[1] = person.getSkillLevel(SkillType.S_PILOT_AERO);
                pilotingLevels[2] = person.getSkillLevel(SkillType.S_PILOT_GVEE);
                pilotingLevels[3] = person.getSkillLevel(SkillType.S_PILOT_VTOL);
                pilotingLevels[4] = person.getSkillLevel(SkillType.S_PILOT_NVEE);
                pilotingLevels[5] = person.getSkillLevel(SkillType.S_PILOT_JET);
                pilotingLevels[6] = person.getSkillLevel(SkillType.S_PILOT_SPACE);

                return Arrays.stream(pilotingLevels).max().getAsInt();

            case "Accuracy":
                int[] gunneryLevels = new int[10];

                gunneryLevels[0] = person.getSkillLevel(SkillType.S_GUN_MECH);
                gunneryLevels[1] = person.getSkillLevel(SkillType.S_GUN_AERO);
                gunneryLevels[2] = person.getSkillLevel(SkillType.S_GUN_VEE);
                gunneryLevels[3] = person.getSkillLevel(SkillType.S_GUN_JET);
                gunneryLevels[4] = person.getSkillLevel(SkillType.S_GUN_SPACE);
                gunneryLevels[5] = person.getSkillLevel(SkillType.S_GUN_BA);
                gunneryLevels[6] = person.getSkillLevel(SkillType.S_GUN_PROTO);
                gunneryLevels[7] = person.getSkillLevel(SkillType.S_ARTILLERY);
                gunneryLevels[8] = person.getSkillLevel(SkillType.S_SMALL_ARMS);
                gunneryLevels[9] = person.getSkillLevel(SkillType.S_ANTI_MECH);

                return Arrays.stream(gunneryLevels).max().getAsInt();

            case "Command":
                int[] commandLevels = new int[3];

                commandLevels[0] = person.getSkillLevel(SkillType.S_LEADER);
                commandLevels[1] = person.getSkillLevel(SkillType.S_TACTICS);
                commandLevels[2] = person.getSkillLevel(SkillType.S_STRATEGY);

                return Arrays.stream(commandLevels).max().getAsInt();

            case "TechWithMedical":
                int[] TechWithMedicalLevels = new int[8];

                TechWithMedicalLevels[0] = person.getSkillLevel(SkillType.S_TECH_MECH);
                TechWithMedicalLevels[1] = person.getSkillLevel(SkillType.S_TECH_AERO);
                TechWithMedicalLevels[2] = person.getSkillLevel(SkillType.S_TECH_MECHANIC);
                TechWithMedicalLevels[3] = person.getSkillLevel(SkillType.S_TECH_VESSEL);
                TechWithMedicalLevels[4] = person.getSkillLevel(SkillType.S_TECH_BA);
                TechWithMedicalLevels[5] = person.getSkillLevel(SkillType.S_ASTECH);
                TechWithMedicalLevels[6] = person.getSkillLevel(SkillType.S_DOCTOR);
                TechWithMedicalLevels[7] = person.getSkillLevel(SkillType.S_MEDTECH);

                return Arrays.stream(TechWithMedicalLevels).max().getAsInt();

            case "Tech":
                int[] TechLevels = new int[6];

                TechLevels[0] = person.getSkillLevel(SkillType.S_TECH_MECH);
                TechLevels[1] = person.getSkillLevel(SkillType.S_TECH_AERO);
                TechLevels[2] = person.getSkillLevel(SkillType.S_TECH_MECHANIC);
                TechLevels[3] = person.getSkillLevel(SkillType.S_TECH_VESSEL);
                TechLevels[4] = person.getSkillLevel(SkillType.S_TECH_BA);
                TechLevels[5] = person.getSkillLevel(SkillType.S_ASTECH);

                return Arrays.stream(TechLevels).max().getAsInt();

            case "Medical":
                int[] MedicalLevels = new int[2];

                MedicalLevels[0] = person.getSkillLevel(SkillType.S_DOCTOR);
                MedicalLevels[1] = person.getSkillLevel(SkillType.S_MEDTECH);

                return Arrays.stream(MedicalLevels).max().getAsInt();

            case "Assistant":
                int[] assistantLevels = new int[2];

                assistantLevels[0] = person.getSkillLevel(SkillType.S_ASTECH);
                assistantLevels[1] = person.getSkillLevel(SkillType.S_MEDTECH);

                return Arrays.stream(assistantLevels).max().getAsInt();
            default:
                if (skill.contains("Piloting")) {
                    switch (skill) {
                        case "PilotingMech":
                            return person.getSkillLevel(SkillType.S_PILOT_MECH);
                        case "PilotingAerospace":
                            return person.getSkillLevel(SkillType.S_PILOT_AERO);
                        case "PilotingGroundVehicle":
                            return person.getSkillLevel(SkillType.S_PILOT_GVEE);
                        case "PilotingVTOL":
                            return person.getSkillLevel(SkillType.S_PILOT_VTOL);
                        case "PilotingNaval":
                            return person.getSkillLevel(SkillType.S_PILOT_NVEE);
                        case "PilotingAircraft":
                            return person.getSkillLevel(SkillType.S_PILOT_JET);
                        case "PilotingSpacecraft":
                            return person.getSkillLevel(SkillType.S_PILOT_SPACE);
                        default:
                            LogManager.getLogger().warn("Award {} from the {} set has invalid skill {}", award.getName(), award.getSet(), skill);

                            // this allows for an int to be returned, but ensures it's impossible to beat.
                            return 999;
                    }
                } else if (skill.contains("Gunnery")) {
                    switch (skill) {
                        case "GunneryMech":
                            return person.getSkillLevel(SkillType.S_GUN_MECH);
                        case "GunneryAerospace":
                            return person.getSkillLevel(SkillType.S_GUN_AERO);
                        case "GunneryVehicle":
                            return person.getSkillLevel(SkillType.S_GUN_VEE);
                        case "GunneryAircraft":
                            return person.getSkillLevel(SkillType.S_GUN_JET);
                        case "GunnerySpacecraft":
                            return person.getSkillLevel(SkillType.S_GUN_SPACE);
                        case "GunneryBattlesuit":
                            return person.getSkillLevel(SkillType.S_GUN_BA);
                        case "GunneryProtoMech":
                            return person.getSkillLevel(SkillType.S_GUN_PROTO);
                        default:
                            LogManager.getLogger().warn("Award {} from the {} set has invalid skill {}", award.getName(), award.getSet(), skill);

                            return 999;
                    }
                } else if (skill.contains("Tech")) {
                    switch (skill) {
                        case "TechMech":
                            return person.getSkillLevel(SkillType.S_TECH_MECH);
                        case "TechMechanic":
                            return person.getSkillLevel(SkillType.S_TECH_MECHANIC);
                        case "TechAero":
                            return person.getSkillLevel(SkillType.S_TECH_AERO);
                        case "TechBA":
                            return person.getSkillLevel(SkillType.S_TECH_BA);
                        case "TechVessel":
                            return person.getSkillLevel(SkillType.S_TECH_VESSEL);
                        default:
                            LogManager.getLogger().warn("Award {} from the {} set has invalid skill {}", award.getName(), award.getSet(), skill);

                            return 999;
                    }
                } else {
                    switch (skill) {
                        case "Artillery":
                            return person.getSkillLevel(SkillType.S_ARTILLERY);
                        case "SmallArms":
                            return person.getSkillLevel(SkillType.S_SMALL_ARMS);
                        case "AntiMech":
                            return person.getSkillLevel(SkillType.S_ANTI_MECH);
                        case "Astech":
                            return person.getSkillLevel(SkillType.S_ASTECH);
                        case "Doctor":
                            return person.getSkillLevel(SkillType.S_DOCTOR);
                        case "Medtech":
                            return person.getSkillLevel(SkillType.S_MEDTECH);
                        case "HyperspaceNavigation":
                            return person.getSkillLevel(SkillType.S_NAV);
                        case "Administration":
                            return person.getSkillLevel(SkillType.S_ADMIN);
                        case "Tactics":
                            return person.getSkillLevel(SkillType.S_TACTICS);
                        case "Strategy":
                            return person.getSkillLevel(SkillType.S_STRATEGY);
                        case "Negotiation":
                            return person.getSkillLevel(SkillType.S_NEG);
                        case "Leadership":
                            return person.getSkillLevel(SkillType.S_LEADER);
                        case "Scrounge":
                            return person.getSkillLevel(SkillType.S_SCROUNGE);
                        default:
                            LogManager.getLogger().warn("Award {} from the {} set has invalid skill {}", award.getName(), award.getSet(), skill);

                            return 999;
                    }
                }
        }
    }
}