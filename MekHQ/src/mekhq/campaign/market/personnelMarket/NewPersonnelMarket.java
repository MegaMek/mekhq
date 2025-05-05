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
package mekhq.campaign.market.personnelMarket;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static megamek.codeUtilities.ObjectUtility.getRandomItem;
import static megamek.common.Compute.d6;
import static megamek.common.Compute.randomInt;
import static mekhq.campaign.personnel.enums.PersonnelRole.DEPENDENT;

import java.io.PrintWriter;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import megamek.Version;
import megamek.codeUtilities.MathUtility;
import megamek.common.annotations.Nullable;
import megamek.common.enums.Gender;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.CurrentLocation;
import mekhq.campaign.market.personnelMarket.yaml.PersonnelMarketLibraries;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.rating.CamOpsReputation.ReputationController;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.FactionHints;
import mekhq.campaign.universe.Factions;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.campaign.universe.enums.HiringHallLevel;
import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class NewPersonnelMarket {
    private static final MMLogger logger = MMLogger.create(NewPersonnelMarket.class);

    static int RARE_PROFESSION_WEIGHT = 10;
    static int LOW_POPULATION_RECRUITMENT_DIVIDER = 10000000;
    static int UNIT_REPUTATION_RECRUITMENT_CUTOFF = -25;

    final private Campaign campaign;
    private Faction campaignFaction;
    private LocalDate today;
    private int gameYear;
    private PlanetarySystem currentSystem;
    private List<Faction> applicantOriginFactions = new ArrayList<>();
    boolean offeringGoldenHello;
    boolean hasRarePersonnel;
    int recruitmentRolls;
    private List<Person> currentApplicants = new ArrayList<>();

    // These values should be generated during object initialization only so that any changes to the underlying YAML
    // can be accounted for. Otherwise, we will end up with a situation where bugs or other variables get 'locked'
    // into the user's campaign save.
    final private transient Map<PersonnelRole, PersonnelMarketEntry> clanMarketEntries;
    final private transient Map<PersonnelRole, PersonnelMarketEntry> innerSphereMarketEntries;

    public NewPersonnelMarket(Campaign campaign) {
        this.campaign = campaign;

        PersonnelMarketLibraries personnelMarketLibraries = new PersonnelMarketLibraries();
        clanMarketEntries = personnelMarketLibraries.getClanMarketMekHQ();
        innerSphereMarketEntries = personnelMarketLibraries.getInnerSphereMarketMekHQ();
    }

    public void gatherApplications() {
        reinitializeKeyData();

        currentApplicants = new ArrayList<>(); // clear old applicants
        applicantOriginFactions = getApplicantOriginFactions();

        String isZeroAvailability = getAvailabilityMessage();

        if (!isZeroAvailability.isEmpty()) {
            logger.debug("No applicants will be generated due to {}", isZeroAvailability);
            return;
        }

        generateApplicants();

        if (currentApplicants.isEmpty()) {
            logger.debug("No applicants were generated.");
        } else {
            logger.debug("Generated {} applicants for the campaign.", currentApplicants.size());
        }
    }

    private void reinitializeKeyData() {
        campaignFaction = campaign.getFaction();
        today = campaign.getLocalDate();
        gameYear = today.getYear();
        currentSystem = campaign.getCurrentSystem();
    }

    public Campaign getCampaign() {
        return campaign;
    }

    public List<Person> getCurrentApplicants() {
        if (currentApplicants == null) {
            return new ArrayList<>();
        }
        return currentApplicants;
    }

    public void setCurrentApplicants(List<Person> currentApplicants) {
        this.currentApplicants = currentApplicants;
    }

    public boolean isOfferingGoldenHello() {
        return offeringGoldenHello;
    }

    public void setOfferingGoldenHello(boolean offeringGoldenHello) {
        this.offeringGoldenHello = offeringGoldenHello;
    }

    public boolean hasRarePersonnel() {
        return hasRarePersonnel;
    }

    public int getRecruitmentRolls() {
        return recruitmentRolls;
    }

    public PlanetarySystem getCurrentSystem() {
        if (currentSystem == null) {
            currentSystem = campaign.getCurrentSystem();
        }
        return currentSystem;
    }

    ArrayList<Faction> getApplicantOriginFactions() {
        Set<Faction> systemFactions = getCurrentSystem().getFactionSet(today);
        ArrayList<Faction> interestedFactions = new ArrayList<>();

        boolean filterOutLegalFactions = false;
        if (campaign.getCampaignOptions().getUnitRatingMethod().isCampaignOperations()) {
            if (campaign.getReputation().getReputationRating() < UNIT_REPUTATION_RECRUITMENT_CUTOFF) {
                logger.info("Only pirates & mercenaries will be considered for applicants, as the campaign's unit " +
                                  "rating is below the cutoff.");
                filterOutLegalFactions = true;
            }
        }

        for (Faction faction : systemFactions) {
            if (filterOutLegalFactions) {
                if (!faction.isPirate() && !faction.isMercenary()) {
                    continue;
                }
            }

            if (FactionHints.defaultFactionHints().isAtWarWith(campaignFaction, faction, today)) {
                continue;
            }

            // Allies are three times as likely to join the campaign as non-allies
            if (FactionHints.defaultFactionHints().isAlliedWith(campaignFaction, faction, today)) {
                interestedFactions.add(faction);
                interestedFactions.add(faction);
            }
            interestedFactions.add(faction);
        }

        Faction mercenaryFaction = Factions.getInstance().getFaction("MERC");
        if (mercenaryFaction != null &&
                  !interestedFactions.isEmpty() &&
                  !interestedFactions.contains(mercenaryFaction)) {
            interestedFactions.add(mercenaryFaction);
        }

        return interestedFactions;
    }


    String getAvailabilityMessage() {
        CurrentLocation location = campaign.getLocation();

        if (!location.isOnPlanet()) {
            return "Not on a planet.";
        }

        if (getApplicantOriginFactions().isEmpty()) {
            return "Locals warring with your faction.";
        }

        return "";
    }

    private void generateApplicants() {
        ReputationController reputation = campaign.getReputation();
        int averageSkillLevel = reputation.getAverageSkillLevel().getExperienceLevel();

        calculateNumberOfRecruitmentRolls();

        // Applicants will only try to join the campaign if their experience level is below the average skill level
        // of the campaign minus 2 to a minimum of 2 (Green).
        averageSkillLevel = max(averageSkillLevel - (isOfferingGoldenHello() ? 1 : 2), 2);

        Map<PersonnelRole, PersonnelMarketEntry> marketEntries = campaign.isClanCampaign() ?
                                                                       clanMarketEntries :
                                                                       innerSphereMarketEntries;

        for (int roll = 0; roll < recruitmentRolls; roll++) {
            PersonnelMarketEntry entry = pickEntry(marketEntries);
            if (entry == null) {
                logger.error("No personnel market entries found. Check the data folder for the appropriate YAML file.");
                return;
            }

            // If the entry's introduction year is after the current game year, pick the fallback profession
            // Keep going until we find an entry that is before the current game year.
            int remainingIterations = 3;
            PersonnelMarketEntry originalEntry = entry;
            while (entry != null && entry.introductionYear() > gameYear) {
                PersonnelRole fallbackProfession = entry.fallbackProfession();
                entry = marketEntries.get(fallbackProfession);
                remainingIterations--;
                if (remainingIterations <= 0) {
                    entry = null;
                }
            }

            if (entry == null) {
                logger.error("Could not find a suitable fallback profession for {} game year {}. This suggests the " +
                                   "fallback structure of the YAML file is incorrect.",
                      originalEntry.profession(),
                      gameYear);
                return;
            }

            // If we have a valid entry, we now need to generate the applicant
            String applicantOriginFaction = getRandomItem(applicantOriginFactions).getShortName();
            Person applicant = campaign.newPerson(entry.profession(), applicantOriginFaction, Gender.RANDOMIZE);
            if (applicant == null) {
                logger.debug("Could not create person for {} game year {} from faction {}",
                      originalEntry.profession(),
                      gameYear,
                      applicantOriginFaction);
                return;
            }

            int applicantSkill = applicant.getSkillLevel(campaign, false).getExperienceLevel();

            if (applicantSkill > averageSkillLevel) {
                logger.debug("Applicant is too experienced for the campaign, skipping.");
                continue;
            }

            currentApplicants.add(applicant);
            if (!hasRarePersonnel && entry.weight() <= RARE_PROFESSION_WEIGHT) {
                hasRarePersonnel = true;
            }
        }

        int dependentsCount = d6();

        for (int roll = 0; roll < dependentsCount; roll++) {
            String applicantOriginFaction = getRandomItem(applicantOriginFactions).getShortName();
            Person applicant = campaign.newPerson(DEPENDENT, applicantOriginFaction, Gender.RANDOMIZE);
            if (applicant == null) {
                continue;
            }

            currentApplicants.add(applicant);
        }
    }

    private void calculateNumberOfRecruitmentRolls() {
        int lengthOfMonth = today.getMonth().length(today.isLeapYear());
        logger.debug("Base rolls: {}", lengthOfMonth);

        recruitmentRolls = lengthOfMonth * getSystemStatusRecruitmentMultiplier();
        logger.debug("Rolls modified for location: {}", recruitmentRolls);

        recruitmentRolls = max(1, (int) Math.floor(recruitmentRolls * getSystemPopulationRecruitmentMultiplier()));
        logger.debug("Rolls modified for population: {}", recruitmentRolls);
    }

    private double getSystemPopulationRecruitmentMultiplier() {
        long currentSystemPopulation = currentSystem.getPopulation(today);
        double populationRatio = (double) currentSystemPopulation / LOW_POPULATION_RECRUITMENT_DIVIDER;
        return min(populationRatio, 1.0);
    }

    public int getSystemStatusRecruitmentMultiplier() {
        CurrentLocation location = campaign.getLocation();
        PlanetarySystem currentSystem = location.getCurrentSystem();

        LocalDate today = campaign.getLocalDate();
        HiringHallLevel hiringHallLevel = currentSystem.getHiringHallLevel(today);

        int rolls = switch (hiringHallLevel) {
            case STANDARD -> 2;
            case GREAT -> 3;
            default -> 1;
        };

        logger.debug("Rolls based on hiring hall status: {}", rolls);

        for (Faction faction : currentSystem.getFactionSet(today)) {
            if (currentSystem.equals(faction.getStartingPlanet(campaign, today))) {
                if (faction.isMajorOrSuperPower()) {
                    rolls++;
                    break;
                }
            }
        }

        logger.debug("Rolls including capital status: {}", rolls);

        return rolls;
    }

    private @Nullable PersonnelMarketEntry pickEntry(Map<PersonnelRole, PersonnelMarketEntry> marketEntries) {
        int totalWeight = marketEntries.values().stream().mapToInt(PersonnelMarketEntry::weight).sum();
        if (totalWeight <= 0) {
            return null;
        }

        int roll = randomInt(totalWeight) + 1; // 1-based, so every entry with positive weight has a chance
        int cumulative = 0;
        for (PersonnelMarketEntry entry : marketEntries.values()) {
            cumulative += entry.weight();
            if (roll <= cumulative) {
                return entry;
            }
        }
        return null; // Should never hit here if weights > 0
    }

    public void showPersonnelMarketDialog() {
        new NewPersonnelMarketGUI(this);
    }

    public void writePersonnelMarketDataToXML(final PrintWriter writer, int indent) {
        MHQXMLUtility.writeSimpleXMLTag(writer, indent, "offeringGoldenHello", offeringGoldenHello);
        MHQXMLUtility.writeSimpleXMLTag(writer, indent, "hasRarePersonnel", hasRarePersonnel);
        MHQXMLUtility.writeSimpleXMLTag(writer, indent, "recruitmentRolls", recruitmentRolls);
        MHQXMLUtility.writeSimpleXMLOpenTag(writer, indent++, "currentApplicants");
        for (final Person person : currentApplicants) {
            person.writeToXML(writer, indent, campaign);
        }
        MHQXMLUtility.writeSimpleXMLCloseTag(writer, --indent, "currentApplicants");
    }

    public NewPersonnelMarket generatePersonnelMarketDataFromXML(final Node parentNode, Version version) {
        NodeList newLine = parentNode.getChildNodes();

        logger.info("Loading Personnel Market Nodes from XML...");

        try {
            for (int i = 0; i < newLine.getLength(); i++) {
                Node childNode = newLine.item(i);
                String nodeName = childNode.getNodeName();
                String nodeContents = childNode.getTextContent().trim();
                if (nodeName.equalsIgnoreCase("offeringGoldenHello")) {
                    this.offeringGoldenHello = Boolean.parseBoolean(nodeContents);
                } else if (nodeName.equalsIgnoreCase("hasRarePersonnel")) {
                    this.hasRarePersonnel = Boolean.parseBoolean(nodeContents);
                } else if (nodeName.equalsIgnoreCase("recruitmentRolls")) {
                    this.recruitmentRolls = MathUtility.parseInt(nodeContents);
                } else if (nodeName.equalsIgnoreCase("currentApplicants")) {
                    processApplicantNodes(childNode, version);
                }
            }
        } catch (Exception ex) {
            logger.error("Could not parse Personnel Market: ", ex);
        }

        return this;
    }

    private void processApplicantNodes(Node wn, Version version) {
        logger.info("Loading Applicant Nodes from XML...");

        NodeList childNodes = wn.getChildNodes();

        // Okay, let's iterate through the children, eh?
        for (int x = 0; x < childNodes.getLength(); x++) {
            Node currentChild = childNodes.item(x);

            // If it's not an element node, we ignore it.
            if (currentChild.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            if (!currentChild.getNodeName().equalsIgnoreCase("person")) {
                logger.error("Unknown node type not loaded in Applicant nodes: {}", currentChild.getNodeName());
                continue;
            }

            Person applicant = Person.generateInstanceFromXML(currentChild, campaign, version);

            if (applicant != null) {
                this.currentApplicants.add(applicant);
            }
        }

        logger.info("Load Applicant Nodes Complete!");
    }
}
