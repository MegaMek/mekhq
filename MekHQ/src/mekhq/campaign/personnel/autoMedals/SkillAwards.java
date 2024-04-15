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
                int requiredSkillLevel;

                try {
                    requiredSkillLevel = award.getQty();
                } catch (Exception e) {
                    LogManager.getLogger().warn("Award {} from the {} set has invalid qty value {}",
                            award.getName(), award.getSet(), award.getQty());
                    break;
                }

                // this allows the user to specify multiple skills to be checked against, where all skill levels need to be met
                List<String> skills = Arrays.asList(award.getRange().split(","));

                boolean hasRequiredSkillLevel = true;

                if (!skills.isEmpty()) {
                    for (String skill : skills) {
                        if (processSkills(award, person, skill) < requiredSkillLevel) {
                            hasRequiredSkillLevel = false;
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
     * This function uses switches to feed the relevant skill/s into getSkillLevel()
     *
     * @param award the award being processed, this is for error logging
     * @param person the person whose skill levels we want
     * @param skill the skill we're checking
     */
    private int processSkills(Award award, Person person, String skill) {
        List<String> relevantSkills;

        switch (skill) {
            // These first couple of cases are for those instances where the users wants to check against multiple skills, but only needs one passing grade
            case "Piloting":
                relevantSkills = Arrays.asList(SkillType.S_PILOT_MECH, SkillType.S_PILOT_AERO, SkillType.S_PILOT_GVEE, SkillType.S_PILOT_VTOL, SkillType.S_PILOT_NVEE, SkillType.S_PILOT_JET, SkillType.S_PILOT_SPACE);
                break;

            case "Accuracy":
                relevantSkills = Arrays.asList(SkillType.S_GUN_MECH, SkillType.S_GUN_AERO, SkillType.S_GUN_VEE, SkillType.S_GUN_JET, SkillType.S_GUN_SPACE, SkillType.S_GUN_BA, SkillType.S_GUN_PROTO, SkillType.S_ARTILLERY, SkillType.S_SMALL_ARMS, SkillType.S_ANTI_MECH);
                break;

            case "Command":
                relevantSkills = Arrays.asList(SkillType.S_LEADER, SkillType.S_TACTICS, SkillType.S_STRATEGY);
                break;

            case "TechWithMedical":
                relevantSkills = Arrays.asList(SkillType.S_TECH_MECH, SkillType.S_TECH_AERO, SkillType.S_TECH_MECHANIC, SkillType.S_TECH_VESSEL, SkillType.S_TECH_BA, SkillType.S_ASTECH, SkillType.S_DOCTOR, SkillType.S_MEDTECH);
                break;

            case "Tech":
                relevantSkills = Arrays.asList(SkillType.S_TECH_MECH, SkillType.S_TECH_AERO, SkillType.S_TECH_MECHANIC, SkillType.S_TECH_VESSEL, SkillType.S_TECH_BA, SkillType.S_ASTECH);
                break;

            case "Medical":
                relevantSkills = Arrays.asList(SkillType.S_DOCTOR, SkillType.S_MEDTECH);
                break;

            case "Assistant":
                relevantSkills = Arrays.asList(SkillType.S_ASTECH, SkillType.S_MEDTECH);
                break;

            case "PilotingMech":
                relevantSkills = List.of(SkillType.S_PILOT_MECH);
                break;

            case "PilotingAerospace":
                relevantSkills = List.of(SkillType.S_PILOT_AERO);
                break;

            case "PilotingGroundVehicle":
                relevantSkills = List.of(SkillType.S_PILOT_GVEE);
                break;

            case "PilotingVTOL":
                relevantSkills = List.of(SkillType.S_PILOT_VTOL);
                break;

            case "PilotingNaval":
                relevantSkills = List.of(SkillType.S_PILOT_NVEE);
                break;

            case "PilotingAircraft":
                relevantSkills = List.of(SkillType.S_PILOT_JET);
                break;

            case "PilotingSpacecraft":
                relevantSkills = List.of(SkillType.S_PILOT_SPACE);
                break;

            case "GunneryMech":
                relevantSkills = List.of(SkillType.S_GUN_MECH);
                break;

            case "GunneryAerospace":
                relevantSkills = List.of(SkillType.S_GUN_AERO);
                break;

            case "GunneryVehicle":
                relevantSkills = List.of(SkillType.S_GUN_VEE);
                break;

            case "GunneryAircraft":
                relevantSkills = List.of(SkillType.S_GUN_JET);
                break;

            case "GunnerySpacecraft":
                relevantSkills = List.of(SkillType.S_GUN_SPACE);
                break;

            case "GunneryBattlesuit":
                relevantSkills = List.of(SkillType.S_GUN_BA);
                break;

            case "GunneryProtoMech":
                relevantSkills = List.of(SkillType.S_GUN_PROTO);
                break;

            case "TechMech":
                relevantSkills = List.of(SkillType.S_TECH_MECH);
                break;

            case "TechMechanic":
                relevantSkills = List.of(SkillType.S_TECH_MECHANIC);
                break;

            case "TechAero":
                relevantSkills = List.of(SkillType.S_TECH_AERO);
                break;

            case "TechBA":
                relevantSkills = List.of(SkillType.S_TECH_BA);
                break;

            case "TechVessel":
                relevantSkills = List.of(SkillType.S_TECH_VESSEL);
                break;

            case "Artillery":
                relevantSkills = List.of(SkillType.S_ARTILLERY);
                break;

            case "SmallArms":
                relevantSkills = List.of(SkillType.S_SMALL_ARMS);
                break;

            case "AntiMech":
                relevantSkills = List.of(SkillType.S_ANTI_MECH);
                break;
            case "Astech":
                relevantSkills = List.of(SkillType.S_ASTECH);
                break;

            case "Doctor":
                relevantSkills = List.of(SkillType.S_DOCTOR);
                break;

            case "Medtech":
                relevantSkills = List.of(SkillType.S_MEDTECH);
                break;

            case "HyperspaceNavigation":
                relevantSkills = List.of(SkillType.S_NAV);
                break;

            case "Administration":
                relevantSkills = List.of(SkillType.S_ADMIN);
                break;

            case "Tactics":
                relevantSkills = List.of(SkillType.S_TACTICS);
                break;

            case "Strategy":
                relevantSkills = List.of(SkillType.S_STRATEGY);
                break;

            case "Negotiation":
                relevantSkills = List.of(SkillType.S_NEG);
                break;

            case "Leadership":
                relevantSkills = List.of(SkillType.S_LEADER);
                break;

            case "Scrounge":
                relevantSkills = List.of(SkillType.S_SCROUNGE);
                break;

            default:
                LogManager.getLogger().warn("Award {} from the {} set has invalid skill {}", award.getName(), award.getSet(), skill);

                return -1;
        }

        return getSkillLevel(relevantSkills, person);
    }

    /**
     * This function loops through all relevant skills, calculating the max skill level. If all skills are untrained
     * the function will default to -1.
     *
     * @param relevantSkills the list of Skills to check
     * @param person the person whose Skill Levels are being checked
     */
    private int getSkillLevel(List<String> relevantSkills, Person person) {
        int[] skillLevels = new int[relevantSkills.size()];

        for (int i = 0; i < relevantSkills.size(); i++) {
            if (person.hasSkill(relevantSkills.get(i))) {
                skillLevels[i] = person.getSkill(relevantSkills.get(i)).getLevel();
            } else {
                skillLevels[i] = -1;
            }
        }

        // IntelliJ's NPE warning here is a false-positive. The code doesn't allow skillLevels to be empty.
        return Arrays.stream(skillLevels).max().getAsInt();
    }
}