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

                // these checks are necessary as, if the person is unskilled, the check generates an exception
                if (person.hasSkill(SkillType.S_PILOT_MECH)) {
                    pilotingLevels[0] = person.getSkill(SkillType.S_PILOT_MECH).getLevel();
                }

                if (person.hasSkill(SkillType.S_PILOT_AERO)) {
                    pilotingLevels[1] = person.getSkill(SkillType.S_PILOT_AERO).getLevel();
                }

                if (person.hasSkill(SkillType.S_PILOT_GVEE)) {
                    pilotingLevels[2] = person.getSkill(SkillType.S_PILOT_GVEE).getLevel();
                }

                if (person.hasSkill(SkillType.S_PILOT_VTOL)) {
                    pilotingLevels[3] = person.getSkill(SkillType.S_PILOT_VTOL).getLevel();
                }

                if (person.hasSkill(SkillType.S_PILOT_NVEE)) {
                    pilotingLevels[4] = person.getSkill(SkillType.S_PILOT_NVEE).getLevel();
                }

                if (person.hasSkill(SkillType.S_PILOT_JET)) {
                    pilotingLevels[5] = person.getSkill(SkillType.S_PILOT_JET).getLevel();
                }

                if (person.hasSkill(SkillType.S_PILOT_SPACE)) {
                pilotingLevels[6] = person.getSkill(SkillType.S_PILOT_SPACE).getLevel();
                }

                return Arrays.stream(pilotingLevels).max().getAsInt();

            case "Accuracy":
                int[] gunneryLevels = new int[10];

                if (person.hasSkill(SkillType.S_GUN_MECH)) {
                    gunneryLevels[0] = person.getSkill(SkillType.S_GUN_MECH).getLevel();
                }

                if (person.hasSkill(SkillType.S_GUN_AERO)) {
                    gunneryLevels[1] = person.getSkill(SkillType.S_GUN_AERO).getLevel();
                }

                if (person.hasSkill(SkillType.S_GUN_VEE)) {
                    gunneryLevels[2] = person.getSkill(SkillType.S_GUN_VEE).getLevel();
                }

                if (person.hasSkill(SkillType.S_GUN_JET)) {
                    gunneryLevels[3] = person.getSkill(SkillType.S_GUN_JET).getLevel();
                }

                if (person.hasSkill(SkillType.S_GUN_SPACE)) {
                    gunneryLevels[4] = person.getSkill(SkillType.S_GUN_SPACE).getLevel();
                }

                if (person.hasSkill(SkillType.S_GUN_BA)) {
                    gunneryLevels[5] = person.getSkill(SkillType.S_GUN_BA).getLevel();
                }

                if (person.hasSkill(SkillType.S_GUN_PROTO)) {
                    gunneryLevels[6] = person.getSkill(SkillType.S_GUN_PROTO).getLevel();
                }

                if (person.hasSkill(SkillType.S_ARTILLERY)) {
                    gunneryLevels[7] = person.getSkill(SkillType.S_ARTILLERY).getLevel();
                }

                if (person.hasSkill(SkillType.S_SMALL_ARMS)) {
                    gunneryLevels[8] = person.getSkill(SkillType.S_SMALL_ARMS).getLevel();
                }

                if (person.hasSkill(SkillType.S_ANTI_MECH)) {
                    gunneryLevels[9] = person.getSkill(SkillType.S_ANTI_MECH).getLevel();
                }

                return Arrays.stream(gunneryLevels).max().getAsInt();

            case "Command":
                int[] commandLevels = new int[3];


                if (person.hasSkill(SkillType.S_LEADER)) {
                    commandLevels[0] = person.getSkill(SkillType.S_LEADER).getLevel();
                }

                if (person.hasSkill(SkillType.S_TACTICS)) {
                    commandLevels[1] = person.getSkill(SkillType.S_TACTICS).getLevel();
                }

                if (person.hasSkill(SkillType.S_STRATEGY)) {
                    commandLevels[2] = person.getSkill(SkillType.S_STRATEGY).getLevel();
                }

                return Arrays.stream(commandLevels).max().getAsInt();

            case "TechWithMedical":
                int[] TechWithMedicalLevels = new int[8];

                if (person.hasSkill(SkillType.S_TECH_MECH)) {
                    TechWithMedicalLevels[0] = person.getSkill(SkillType.S_TECH_MECH).getLevel();
                }

                if (person.hasSkill(SkillType.S_TECH_AERO)) {
                    TechWithMedicalLevels[1] = person.getSkill(SkillType.S_TECH_AERO).getLevel();
                }

                if (person.hasSkill(SkillType.S_TECH_MECHANIC)) {
                    TechWithMedicalLevels[2] = person.getSkill(SkillType.S_TECH_MECHANIC).getLevel();
                }

                if (person.hasSkill(SkillType.S_TECH_VESSEL)) {
                    TechWithMedicalLevels[3] = person.getSkill(SkillType.S_TECH_VESSEL).getLevel();
                }
                if (person.hasSkill(SkillType.S_TECH_BA)) {
                    TechWithMedicalLevels[4] = person.getSkill(SkillType.S_TECH_BA).getLevel();
                }

                if (person.hasSkill(SkillType.S_ASTECH)) {
                    TechWithMedicalLevels[5] = person.getSkill(SkillType.S_ASTECH).getLevel();
                }

                if (person.hasSkill(SkillType.S_DOCTOR)) {
                    TechWithMedicalLevels[6] = person.getSkill(SkillType.S_DOCTOR).getLevel();
                }

                if (person.hasSkill(SkillType.S_MEDTECH)) {
                    TechWithMedicalLevels[7] = person.getSkill(SkillType.S_MEDTECH).getLevel();
                }

                return Arrays.stream(TechWithMedicalLevels).max().getAsInt();

            case "Tech":
                int[] TechLevels = new int[6];


                if (person.hasSkill(SkillType.S_TECH_MECH)) {
                    TechLevels[0] = person.getSkill(SkillType.S_TECH_MECH).getLevel();
                }

                if (person.hasSkill(SkillType.S_TECH_AERO)) {
                    TechLevels[1] = person.getSkill(SkillType.S_TECH_AERO).getLevel();
                }

                if (person.hasSkill(SkillType.S_TECH_MECHANIC)) {
                    TechLevels[2] = person.getSkill(SkillType.S_TECH_MECHANIC).getLevel();
                }

                if (person.hasSkill(SkillType.S_TECH_VESSEL)) {
                    TechLevels[3] = person.getSkill(SkillType.S_TECH_VESSEL).getLevel();
                }

                if (person.hasSkill(SkillType.S_TECH_BA)) {
                    TechLevels[4] = person.getSkill(SkillType.S_TECH_BA).getLevel();
                }

                if (person.hasSkill(SkillType.S_ASTECH)) {
                    TechLevels[5] = person.getSkill(SkillType.S_ASTECH).getLevel();
                }

                return Arrays.stream(TechLevels).max().getAsInt();

            case "Medical":
                int[] MedicalLevels = new int[2];

                if (person.hasSkill(SkillType.S_DOCTOR)) {
                    MedicalLevels[0] = person.getSkill(SkillType.S_DOCTOR).getLevel();
                }

                if (person.hasSkill(SkillType.S_MEDTECH)) {
                    MedicalLevels[1] = person.getSkill(SkillType.S_MEDTECH).getLevel();
                }

                return Arrays.stream(MedicalLevels).max().getAsInt();

            case "Assistant":
                int[] assistantLevels = new int[2];

                if (person.hasSkill(SkillType.S_ASTECH)) {
                    assistantLevels[0] = person.getSkill(SkillType.S_ASTECH).getLevel();
                }

                if (person.hasSkill(SkillType.S_MEDTECH)) {
                    assistantLevels[1] = person.getSkill(SkillType.S_MEDTECH).getLevel();
                }

                return Arrays.stream(assistantLevels).max().getAsInt();
            default:
                if (skill.contains("Piloting")) {
                    switch (skill) {
                        case "PilotingMech":
                            if (person.hasSkill(SkillType.S_PILOT_MECH)) {
                                return person.getSkill(SkillType.S_PILOT_MECH).getLevel();
                            } else {
                                return -1;
                            }
                        case "PilotingAerospace":
                            if (person.hasSkill(SkillType.S_PILOT_AERO)) {
                                return person.getSkill(SkillType.S_PILOT_AERO).getLevel();
                            } else {
                                return -1;
                            }
                        case "PilotingGroundVehicle":
                            if (person.hasSkill(SkillType.S_PILOT_GVEE)) {
                                return person.getSkill(SkillType.S_PILOT_GVEE).getLevel();
                            } else {
                                return -1;
                            }
                        case "PilotingVTOL":
                            if (person.hasSkill(SkillType.S_PILOT_MECH)) {
                                return person.getSkill(SkillType.S_PILOT_VTOL).getLevel();
                            } else {
                                return -1;
                            }
                        case "PilotingNaval":
                            if (person.hasSkill(SkillType.S_PILOT_NVEE)) {
                                return person.getSkill(SkillType.S_PILOT_NVEE).getLevel();
                            } else {
                                return -1;
                            }
                        case "PilotingAircraft":
                            if (person.hasSkill(SkillType.S_PILOT_JET)) {
                                return person.getSkill(SkillType.S_PILOT_JET).getLevel();
                            } else {
                                return -1;
                            }
                        case "PilotingSpacecraft":
                            if (person.hasSkill(SkillType.S_PILOT_SPACE)) {
                                return person.getSkill(SkillType.S_PILOT_SPACE).getLevel();
                            } else {
                                return -1;
                            }
                        default:
                            LogManager.getLogger().warn("Award {} from the {} set has invalid skill {}", award.getName(), award.getSet(), skill);

                            // this allows for an int to be returned, but ensures it's impossible to beat the required test.
                            return -1;
                    }
                } else if (skill.contains("Gunnery")) {
                    switch (skill) {
                        case "GunneryMech":
                            if (person.hasSkill(SkillType.S_GUN_MECH)) {
                                return person.getSkill(SkillType.S_GUN_MECH).getLevel();
                            } else {
                                return -1;
                            }
                        case "GunneryAerospace":
                            if (person.hasSkill(SkillType.S_GUN_AERO)) {
                                return person.getSkill(SkillType.S_GUN_AERO).getLevel();
                            } else {
                                return -1;
                            }
                        case "GunneryVehicle":
                            if (person.hasSkill(SkillType.S_GUN_VEE)) {
                                return person.getSkill(SkillType.S_GUN_VEE).getLevel();
                            } else {
                                return -1;
                            }
                        case "GunneryAircraft":
                            if (person.hasSkill(SkillType.S_GUN_JET)) {
                                return person.getSkill(SkillType.S_GUN_JET).getLevel();
                            } else {
                                return -1;
                            }
                        case "GunnerySpacecraft":
                            if (person.hasSkill(SkillType.S_GUN_SPACE)) {
                                return person.getSkill(SkillType.S_GUN_SPACE).getLevel();
                            } else {
                                return -1;
                            }
                        case "GunneryBattlesuit":
                            if (person.hasSkill(SkillType.S_GUN_BA)) {
                                return person.getSkill(SkillType.S_GUN_BA).getLevel();
                            } else {
                                return -1;
                            }
                        case "GunneryProtoMech":
                            if (person.hasSkill(SkillType.S_GUN_PROTO)) {
                                return person.getSkill(SkillType.S_GUN_PROTO).getLevel();
                            } else {
                                return -1;
                            }
                        default:
                            LogManager.getLogger().warn("Award {} from the {} set has invalid skill {}", award.getName(), award.getSet(), skill);

                            return -1;
                    }
                } else if (skill.contains("Tech")) {
                    switch (skill) {
                        case "TechMech":
                            if (person.hasSkill(SkillType.S_TECH_MECH)) {
                                return person.getSkill(SkillType.S_TECH_MECH).getLevel();
                            } else {
                                return -1;
                            }
                        case "TechMechanic":
                            if (person.hasSkill(SkillType.S_TECH_MECHANIC)) {
                                return person.getSkill(SkillType.S_TECH_MECHANIC).getLevel();
                            } else {
                                return -1;
                            }
                        case "TechAero":
                            if (person.hasSkill(SkillType.S_TECH_AERO)) {
                                return person.getSkill(SkillType.S_TECH_AERO).getLevel();
                            } else {
                                return -1;
                            }
                        case "TechBA":
                            if (person.hasSkill(SkillType.S_TECH_BA)) {
                                return person.getSkill(SkillType.S_TECH_BA).getLevel();
                            } else {
                                return -1;
                            }
                        case "TechVessel":
                            if (person.hasSkill(SkillType.S_TECH_VESSEL)) {
                                return person.getSkill(SkillType.S_TECH_VESSEL).getLevel();
                            } else {
                                return -1;
                            }
                        default:
                            LogManager.getLogger().warn("Award {} from the {} set has invalid skill {}", award.getName(), award.getSet(), skill);

                            return -1;
                    }
                } else {
                    switch (skill) {
                        case "Artillery":
                            if (person.hasSkill(SkillType.S_ARTILLERY)) {
                                return person.getSkill(SkillType.S_ARTILLERY).getLevel();
                            } else {
                                return -1;
                            }
                        case "SmallArms":
                            if (person.hasSkill(SkillType.S_SMALL_ARMS)) {
                                return person.getSkill(SkillType.S_SMALL_ARMS).getLevel();
                            } else {
                                return -1;
                            }
                        case "AntiMech":
                            if (person.hasSkill(SkillType.S_ANTI_MECH)) {
                                return person.getSkill(SkillType.S_ANTI_MECH).getLevel();
                            } else {
                                return -1;
                            }
                        case "Astech":
                            if (person.hasSkill(SkillType.S_ASTECH)) {
                                return person.getSkill(SkillType.S_ASTECH).getLevel();
                            } else {
                                return -1;
                            }
                        case "Doctor":
                            if (person.hasSkill(SkillType.S_DOCTOR)) {
                                return person.getSkill(SkillType.S_DOCTOR).getLevel();
                            } else {
                                return -1;
                            }
                        case "Medtech":
                            if (person.hasSkill(SkillType.S_MEDTECH)) {
                                return person.getSkill(SkillType.S_MEDTECH).getLevel();
                            } else {
                                return -1;
                            }
                        case "HyperspaceNavigation":
                            if (person.hasSkill(SkillType.S_NAV)) {
                                return person.getSkill(SkillType.S_NAV).getLevel();
                            } else {
                                return -1;
                            }
                        case "Administration":
                            if (person.hasSkill(SkillType.S_ADMIN)) {
                                return person.getSkill(SkillType.S_ADMIN).getLevel();
                            } else {
                                return -1;
                            }
                        case "Tactics":
                            if (person.hasSkill(SkillType.S_TACTICS)) {
                                return person.getSkill(SkillType.S_TACTICS).getLevel();
                            } else {
                                return -1;
                            }
                        case "Strategy":
                            if (person.hasSkill(SkillType.S_STRATEGY)) {
                                return person.getSkill(SkillType.S_STRATEGY).getLevel();
                            } else {
                                return -1;
                            }
                        case "Negotiation":
                            if (person.hasSkill(SkillType.S_NEG)) {
                                return person.getSkill(SkillType.S_NEG).getLevel();
                            } else {
                                return -1;
                            }
                        case "Leadership":
                            if (person.hasSkill(SkillType.S_LEADER)) {
                                return person.getSkill(SkillType.S_LEADER).getLevel();
                            } else {
                                return -1;
                            }
                        case "Scrounge":
                            if (person.hasSkill(SkillType.S_SCROUNGE)) {
                                return person.getSkill(SkillType.S_SCROUNGE).getLevel();
                            } else {
                                return -1;
                            }
                        default:
                            LogManager.getLogger().warn("Award {} from the {} set has invalid skill {}", award.getName(), award.getSet(), skill);

                            return -1;
                    }
                }
        }
    }
}