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
                int requiredSkillLevel = award.getQty();

                // this allows the user to specify multiple skills to be checked against
                List<String> skills = Arrays.asList(award.getRange().split(","));

                boolean hasRequiredSkillLevel = true;

                if (!skills.isEmpty()) {
                    for (String skill : skills) {
                        if (getSkillLevel(award, person, skill) < requiredSkillLevel) {
                            LogManager.getLogger().info("Skill {} vs. Required Skill {}", getSkillLevel(award, person, skill), requiredSkillLevel);
                            hasRequiredSkillLevel = false;
                            LogManager.getLogger().info(hasRequiredSkillLevel);
                            // this break ensures that all required skills must be met/exceeded for Award eligibility
                            break;
                        }
                    }

                    if (hasRequiredSkillLevel) {
                        // we have to include ' ' as hyperlinked names lose their hyperlink if used within resource.getString()
                        campaign.addReport(person.getHyperlinkedName() + ' ' +
                                MessageFormat.format(resource.getString("EligibleForAwardReport.format"),
                                        award.getName(), award.getSet()));
                    }
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

                pilotingLevels[0] = person.getSkill(SkillType.S_PILOT_MECH).getLevel();
                pilotingLevels[1] = person.getSkill(SkillType.S_PILOT_AERO).getLevel();
                pilotingLevels[2] = person.getSkill(SkillType.S_PILOT_GVEE).getLevel();
                pilotingLevels[3] = person.getSkill(SkillType.S_PILOT_VTOL).getLevel();
                pilotingLevels[4] = person.getSkill(SkillType.S_PILOT_NVEE).getLevel();
                pilotingLevels[5] = person.getSkill(SkillType.S_PILOT_JET).getLevel();
                pilotingLevels[6] = person.getSkill(SkillType.S_PILOT_SPACE).getLevel();

                return Arrays.stream(pilotingLevels).max().getAsInt();

            case "Accuracy":
                int[] gunneryLevels = new int[10];

                gunneryLevels[0] = person.getSkill(SkillType.S_GUN_MECH).getLevel();
                gunneryLevels[1] = person.getSkill(SkillType.S_GUN_AERO).getLevel();
                gunneryLevels[2] = person.getSkill(SkillType.S_GUN_VEE).getLevel();
                gunneryLevels[3] = person.getSkill(SkillType.S_GUN_JET).getLevel();
                gunneryLevels[4] = person.getSkill(SkillType.S_GUN_SPACE).getLevel();
                gunneryLevels[5] = person.getSkill(SkillType.S_GUN_BA).getLevel();
                gunneryLevels[6] = person.getSkill(SkillType.S_GUN_PROTO).getLevel();
                gunneryLevels[7] = person.getSkill(SkillType.S_ARTILLERY).getLevel();
                gunneryLevels[8] = person.getSkill(SkillType.S_SMALL_ARMS).getLevel();
                gunneryLevels[9] = person.getSkill(SkillType.S_ANTI_MECH).getLevel();

                return Arrays.stream(gunneryLevels).max().getAsInt();

            case "Command":
                int[] commandLevels = new int[3];

                commandLevels[0] = person.getSkill(SkillType.S_LEADER).getLevel();
                commandLevels[1] = person.getSkill(SkillType.S_TACTICS).getLevel();
                commandLevels[2] = person.getSkill(SkillType.S_STRATEGY).getLevel();

                return Arrays.stream(commandLevels).max().getAsInt();

            case "TechWithMedical":
                int[] TechWithMedicalLevels = new int[8];

                TechWithMedicalLevels[0] = person.getSkill(SkillType.S_TECH_MECH).getLevel();
                TechWithMedicalLevels[1] = person.getSkill(SkillType.S_TECH_AERO).getLevel();
                TechWithMedicalLevels[2] = person.getSkill(SkillType.S_TECH_MECHANIC).getLevel();
                TechWithMedicalLevels[3] = person.getSkill(SkillType.S_TECH_VESSEL).getLevel();
                TechWithMedicalLevels[4] = person.getSkill(SkillType.S_TECH_BA).getLevel();
                TechWithMedicalLevels[5] = person.getSkill(SkillType.S_ASTECH).getLevel();
                TechWithMedicalLevels[6] = person.getSkill(SkillType.S_DOCTOR).getLevel();
                TechWithMedicalLevels[7] = person.getSkill(SkillType.S_MEDTECH).getLevel();

                return Arrays.stream(TechWithMedicalLevels).max().getAsInt();

            case "Tech":
                int[] TechLevels = new int[6];

                TechLevels[0] = person.getSkill(SkillType.S_TECH_MECH).getLevel();
                TechLevels[1] = person.getSkill(SkillType.S_TECH_AERO).getLevel();
                TechLevels[2] = person.getSkill(SkillType.S_TECH_MECHANIC).getLevel();
                TechLevels[3] = person.getSkill(SkillType.S_TECH_VESSEL).getLevel();
                TechLevels[4] = person.getSkill(SkillType.S_TECH_BA).getLevel();
                TechLevels[5] = person.getSkill(SkillType.S_ASTECH).getLevel();

                return Arrays.stream(TechLevels).max().getAsInt();

            case "Medical":
                int[] MedicalLevels = new int[2];

                MedicalLevels[0] = person.getSkill(SkillType.S_DOCTOR).getLevel();
                MedicalLevels[1] = person.getSkill(SkillType.S_MEDTECH).getLevel();

                return Arrays.stream(MedicalLevels).max().getAsInt();

            case "Assistant":
                int[] assistantLevels = new int[2];

                assistantLevels[0] = person.getSkill(SkillType.S_ASTECH).getLevel();
                assistantLevels[1] = person.getSkill(SkillType.S_MEDTECH).getLevel();

                return Arrays.stream(assistantLevels).max().getAsInt();
            default:
                if (skill.contains("Piloting")) {
                    switch (skill) {
                        case "PilotingMech":
                            return person.getSkill(SkillType.S_PILOT_MECH).getLevel();
                        case "PilotingAerospace":
                            return person.getSkill(SkillType.S_PILOT_AERO).getLevel();
                        case "PilotingGroundVehicle":
                            return person.getSkill(SkillType.S_PILOT_GVEE).getLevel();
                        case "PilotingVTOL":
                            return person.getSkill(SkillType.S_PILOT_VTOL).getLevel();
                        case "PilotingNaval":
                            return person.getSkill(SkillType.S_PILOT_NVEE).getLevel();
                        case "PilotingAircraft":
                            return person.getSkill(SkillType.S_PILOT_JET).getLevel();
                        case "PilotingSpacecraft":
                            return person.getSkill(SkillType.S_PILOT_SPACE).getLevel();
                        default:
                            LogManager.getLogger().warn("Award {} from the {} set has invalid skill {}", award.getName(), award.getSet(), skill);

                            // this allows for an int to be returned, but ensures it's impossible to beat.
                            return 999;
                    }
                } else if (skill.contains("Gunnery")) {
                    switch (skill) {
                        case "GunneryMech":
                            return person.getSkill(SkillType.S_GUN_MECH).getLevel();
                        case "GunneryAerospace":
                            return person.getSkill(SkillType.S_GUN_AERO).getLevel();
                        case "GunneryVehicle":
                            return person.getSkill(SkillType.S_GUN_VEE).getLevel();
                        case "GunneryAircraft":
                            return person.getSkill(SkillType.S_GUN_JET).getLevel();
                        case "GunnerySpacecraft":
                            return person.getSkill(SkillType.S_GUN_SPACE).getLevel();
                        case "GunneryBattlesuit":
                            return person.getSkill(SkillType.S_GUN_BA).getLevel();
                        case "GunneryProtoMech":
                            return person.getSkill(SkillType.S_GUN_PROTO).getLevel();
                        default:
                            LogManager.getLogger().warn("Award {} from the {} set has invalid skill {}", award.getName(), award.getSet(), skill);

                            return 999;
                    }
                } else if (skill.contains("Tech")) {
                    switch (skill) {
                        case "TechMech":
                            return person.getSkill(SkillType.S_TECH_MECH).getLevel();
                        case "TechMechanic":
                            return person.getSkill(SkillType.S_TECH_MECHANIC).getLevel();
                        case "TechAero":
                            return person.getSkill(SkillType.S_TECH_AERO).getLevel();
                        case "TechBA":
                            return person.getSkill(SkillType.S_TECH_BA).getLevel();
                        case "TechVessel":
                            return person.getSkill(SkillType.S_TECH_VESSEL).getLevel();
                        default:
                            LogManager.getLogger().warn("Award {} from the {} set has invalid skill {}", award.getName(), award.getSet(), skill);

                            return 999;
                    }
                } else {
                    switch (skill) {
                        case "Artillery":
                            return person.getSkill(SkillType.S_ARTILLERY).getLevel();
                        case "SmallArms":
                            return person.getSkill(SkillType.S_SMALL_ARMS).getLevel();
                        case "AntiMech":
                            return person.getSkill(SkillType.S_ANTI_MECH).getLevel();
                        case "Astech":
                            return person.getSkill(SkillType.S_ASTECH).getLevel();
                        case "Doctor":
                            return person.getSkill(SkillType.S_DOCTOR).getLevel();
                        case "Medtech":
                            return person.getSkill(SkillType.S_MEDTECH).getLevel();
                        case "HyperspaceNavigation":
                            return person.getSkill(SkillType.S_NAV).getLevel();
                        case "Administration":
                            return person.getSkill(SkillType.S_ADMIN).getLevel();
                        case "Tactics":
                            return person.getSkill(SkillType.S_TACTICS).getLevel();
                        case "Strategy":
                            return person.getSkill(SkillType.S_STRATEGY).getLevel();
                        case "Negotiation":
                            return person.getSkill(SkillType.S_NEG).getLevel();
                        case "Leadership":
                            return person.getSkill(SkillType.S_LEADER).getLevel();
                        case "Scrounge":
                            return person.getSkill(SkillType.S_SCROUNGE).getLevel();
                        default:
                            LogManager.getLogger().warn("Award {} from the {} set has invalid skill {}", award.getName(), award.getSet(), skill);

                            return 999;
                    }
                }
        }
    }
}