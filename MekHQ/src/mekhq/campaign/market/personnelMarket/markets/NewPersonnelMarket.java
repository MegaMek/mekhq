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
package mekhq.campaign.market.personnelMarket.markets;

import static megamek.codeUtilities.ObjectUtility.getRandomItem;
import static megamek.common.Compute.randomInt;
import static mekhq.campaign.market.personnelMarket.enums.PersonnelMarketStyle.PERSONNEL_MARKET_DISABLED;

import java.io.PrintWriter;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import megamek.Version;
import megamek.codeUtilities.MathUtility;
import megamek.common.annotations.Nullable;
import megamek.common.enums.Gender;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.event.MarketNewPersonnelEvent;
import mekhq.campaign.market.personnelMarket.enums.PersonnelMarketStyle;
import mekhq.campaign.market.personnelMarket.records.PersonnelMarketEntry;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.gui.dialog.markets.personnelMarket.PersonnelMarketDialog;
import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class NewPersonnelMarket {
    private static final MMLogger logger = MMLogger.create(NewPersonnelMarket.class);

    private static int RARE_PROFESSION_WEIGHT = 20;
    private static int LOW_POPULATION_RECRUITMENT_DIVIDER = 1;
    private static int UNIT_REPUTATION_RECRUITMENT_CUTOFF = Integer.MIN_VALUE;

    final private Campaign campaign;
    private PersonnelMarketStyle associatedPersonnelMarketStyle = PERSONNEL_MARKET_DISABLED;
    private Faction campaignFaction;
    private LocalDate today;
    private int gameYear;
    private PlanetarySystem currentSystem;
    @SuppressWarnings(value = "unused")
    private List<Faction> applicantOriginFactions = new ArrayList<>();
    boolean offeringGoldenHello = true;
    boolean hasRarePersonnel;
    int recruitmentRolls;
    private List<Person> currentApplicants = new ArrayList<>();
    private int lastSelectedFilter;

    // These values should be generated during object initialization only so that any changes to the underlying YAML
    // can be accounted for. Otherwise, we will end up with a situation where bugs or other variables get 'locked'
    // into the user's campaign save.
    private transient Map<PersonnelRole, PersonnelMarketEntry> clanMarketEntries;
    private transient Map<PersonnelRole, PersonnelMarketEntry> innerSphereMarketEntries;

    public NewPersonnelMarket(Campaign campaign) {
        this.campaign = campaign;

        logger.debug("Initializing NewPersonnelMarket");

        clanMarketEntries = new HashMap<>();
        innerSphereMarketEntries = new HashMap<>();
    }

    public static NewPersonnelMarket generatePersonnelMarketDataFromXML(final Campaign campaign, final Node parentNode,
          Version version) {
        NodeList newLine = parentNode.getChildNodes();

        logger.debug("Loading Personnel Market Nodes from XML...");

        NewPersonnelMarket personnelMarket = null;
        try {
            for (int i = 0; i < newLine.getLength(); i++) {
                Node childNode = newLine.item(i);
                String nodeName = childNode.getNodeName();
                String nodeContents = childNode.getTextContent().trim();
                if (nodeName.equalsIgnoreCase("associatedPersonnelMarketStyle")) {
                    PersonnelMarketStyle marketStyle = PersonnelMarketStyle.fromString(nodeContents);
                    personnelMarket = switch (marketStyle) {
                        case PERSONNEL_MARKET_DISABLED -> new NewPersonnelMarket(campaign);
                        case MEKHQ -> new PersonnelMarketMekHQ(campaign);
                        case CAMPAIGN_OPERATIONS_REVISED -> new PersonnelMarketCamOpsRevised(campaign);
                        case CAMPAIGN_OPERATIONS_STRICT -> new PersonnelMarketCamOpsStrict(campaign);
                    };
                }
            }
        } catch (Exception ex) {
            logger.error("Could not initialize Personnel Market: ", ex);
        }

        if (personnelMarket == null) {
            logger.error("Using fallback Personnel Market. This means we've failed to load the Personnel Market data " +
                               "from the campaign save. If this save predates 50.07 that's to be expected. Otherwise, " +
                               "please report this as a bug.");
            return new NewPersonnelMarket(campaign);
        }

        try {
            for (int i = 0; i < newLine.getLength(); i++) {
                Node childNode = newLine.item(i);
                String nodeName = childNode.getNodeName();
                String nodeContents = childNode.getTextContent().trim();
                if (nodeName.equalsIgnoreCase("associatedPersonnelMarketStyle")) {
                    personnelMarket.setAssociatedPersonnelMarketStyle(PersonnelMarketStyle.fromString(nodeContents));
                } else if (nodeName.equalsIgnoreCase("offeringGoldenHello")) {
                    personnelMarket.setOfferingGoldenHello(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("hasRarePersonnel")) {
                    personnelMarket.setHasRarePersonnel(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("recruitmentRolls")) {
                    personnelMarket.setRecruitmentRolls(MathUtility.parseInt(nodeContents));
                } else if (nodeName.equalsIgnoreCase("lastSelectedFilter")) {
                    personnelMarket.setLastSelectedFilter(MathUtility.parseInt(nodeContents));
                } else if (nodeName.equalsIgnoreCase("currentApplicants")) {
                    processApplicantNodes(personnelMarket, childNode, version);
                }
            }
        } catch (Exception ex) {
            logger.error("Could not parse Personnel Market: ", ex);
        }

        return personnelMarket;
    }

    public void gatherApplications() {
        reinitializeKeyData();

        setCurrentApplicants(new ArrayList<>()); // clear old applicants
        setApplicantOriginFactions(applicantOriginFactions);

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

            if (campaign.getCampaignOptions().isPersonnelMarketReportRefresh()) {
                generatePersonnelReport(campaign);
            }

            MekHQ.triggerEvent(new MarketNewPersonnelEvent(currentApplicants));
        }
    }

    public void generateApplicants() {
    }

    public void showPersonnelMarketDialog() {
        new PersonnelMarketDialog(this);
    }

    public void writePersonnelMarketDataToXML(final PrintWriter writer, int indent) {
        MHQXMLUtility.writeSimpleXMLTag(writer,
              indent,
              "associatedPersonnelMarketStyle",
              associatedPersonnelMarketStyle.name()); // this node must always be first
        MHQXMLUtility.writeSimpleXMLTag(writer, indent, "offeringGoldenHello", offeringGoldenHello);
        MHQXMLUtility.writeSimpleXMLTag(writer, indent, "hasRarePersonnel", hasRarePersonnel);
        MHQXMLUtility.writeSimpleXMLTag(writer, indent, "recruitmentRolls", recruitmentRolls);
        MHQXMLUtility.writeSimpleXMLTag(writer, indent, "lastSelectedFilter", lastSelectedFilter);
        MHQXMLUtility.writeSimpleXMLOpenTag(writer, indent++, "currentApplicants");
        for (final Person person : currentApplicants) {
            person.writeToXML(writer, indent, campaign);
        }
        MHQXMLUtility.writeSimpleXMLCloseTag(writer, --indent, "currentApplicants");
    }

    public PersonnelMarketStyle getAssociatedPersonnelMarketStyle() {
        return associatedPersonnelMarketStyle;
    }

    public void setAssociatedPersonnelMarketStyle(PersonnelMarketStyle associatedPersonnelMarketStyle) {
        this.associatedPersonnelMarketStyle = associatedPersonnelMarketStyle;
    }

    public int getLowPopulationRecruitmentDivider() {
        return LOW_POPULATION_RECRUITMENT_DIVIDER;
    }

    public void setLowPopulationRecruitmentDivider(int lowPopulationRecruitmentDivider) {
        LOW_POPULATION_RECRUITMENT_DIVIDER = lowPopulationRecruitmentDivider;
    }

    public int getUnitReputationRecruitmentCutoff() {
        return UNIT_REPUTATION_RECRUITMENT_CUTOFF;
    }

    public void setUnitReputationRecruitmentCutoff(int unitReputationRecruitmentCutoff) {
        UNIT_REPUTATION_RECRUITMENT_CUTOFF = unitReputationRecruitmentCutoff;
    }

    public Map<PersonnelRole, PersonnelMarketEntry> getClanMarketEntries() {
        return clanMarketEntries;
    }

    public void setClanMarketEntries(Map<PersonnelRole, PersonnelMarketEntry> clanMarketEntries) {
        this.clanMarketEntries = clanMarketEntries;
    }

    public Map<PersonnelRole, PersonnelMarketEntry> getInnerSphereMarketEntries() {
        return innerSphereMarketEntries;
    }

    public void setInnerSphereMarketEntries(Map<PersonnelRole, PersonnelMarketEntry> innerSphereMarketEntries) {
        this.innerSphereMarketEntries = innerSphereMarketEntries;
    }

    public Campaign getCampaign() {
        return campaign;
    }

    public Faction getCampaignFaction() {
        if (campaignFaction == null) {
            campaignFaction = campaign.getFaction();
        }
        return campaignFaction;
    }

    public void setCampaignFaction(Faction campaignFaction) {
        this.campaignFaction = campaignFaction;
    }

    public LocalDate getToday() {
        if (today == null) {
            today = campaign.getLocalDate();
        }
        return today;
    }

    public void setToday(LocalDate today) {
        this.today = today;
    }

    public int getGameYear() {
        if (gameYear == 0) {
            gameYear = today.getYear();
        }
        return gameYear;
    }

    public ArrayList<Faction> getApplicantOriginFactions() {
        return new ArrayList<>();
    }

    public void setApplicantOriginFactions(List<Faction> applicantOriginFactions) {
        this.applicantOriginFactions = applicantOriginFactions;
    }

    public List<Person> getCurrentApplicants() {
        if (currentApplicants == null) {
            return new ArrayList<>();
        }
        return currentApplicants;
    }

    public int getLastSelectedFilter() {
        return lastSelectedFilter;
    }

    public void setLastSelectedFilter(int lastSelectedFilter) {
        this.lastSelectedFilter = lastSelectedFilter;
    }

    public void addApplicant(Person applicant) {
        if (applicant != null && !currentApplicants.contains(applicant)) {
            currentApplicants.add(applicant);
        }
    }

    public void clearCurrentApplicants() {
        setCurrentApplicants(new ArrayList<>());
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

    public boolean getHasRarePersonnel() {
        return hasRarePersonnel;
    }

    public void setHasRarePersonnel(boolean hasRarePersonnel) {
        this.hasRarePersonnel = hasRarePersonnel;
    }

    public int getRecruitmentRolls() {
        return recruitmentRolls;
    }

    public void setRecruitmentRolls(int recruitmentRolls) {
        this.recruitmentRolls = recruitmentRolls;
    }

    public PlanetarySystem getCurrentSystem() {
        if (currentSystem == null) {
            currentSystem = campaign.getCurrentSystem();
        }
        return currentSystem;
    }

    public void setCurrentSystem(PlanetarySystem currentSystem) {
        this.currentSystem = currentSystem;
    }

    public String getAvailabilityMessage() {
        return "";
    }

    void reinitializeKeyData() {
        campaignFaction = campaign.getFaction();
        today = campaign.getLocalDate();
        gameYear = today.getYear();
        currentSystem = campaign.getCurrentSystem();
    }

    @Nullable
    Person generateSingleApplicant(Map<PersonnelRole, PersonnelMarketEntry> marketEntries) {
        PersonnelMarketEntry entry = pickEntry(marketEntries);
        if (entry == null) {
            logger.error("No personnel market entries found. Check the data folder for the appropriate YAML file.");
            return null;
        }

        // If the entry's introduction year is after the current game year, pick the fallback profession
        // Keep going until we find an entry that is before the current game year.
        int remainingIterations = 3;
        PersonnelMarketEntry originalEntry = entry;
        while (entry != null && (entry.introductionYear() > gameYear) && (entry.extinctionYear() <= gameYear)) {
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
            return null;
        }

        // If we have a valid entry, we now need to generate the applicant
        String applicantOriginFaction = getRandomItem(applicantOriginFactions).getShortName();
        Person applicant = campaign.newPerson(entry.profession(), applicantOriginFaction, Gender.RANDOMIZE);
        if (applicant == null) {
            logger.debug("Could not create person for {} game year {} from faction {}",
                  originalEntry.profession(),
                  gameYear,
                  applicantOriginFaction);
            return null;
        }

        if (!hasRarePersonnel && (entry.weight() <= RARE_PROFESSION_WEIGHT)) {
            hasRarePersonnel = true;
        }

        logger.debug("Generated applicant {} ({}) game year {} from faction {}",
              applicant.getFullName(),
              applicant.getPrimaryRole(),
              gameYear,
              applicantOriginFaction);

        return applicant;
    }

    void generatePersonnelReport(Campaign campaign) {
        campaign.addReport("<a href='PERSONNEL_MARKET'>Personnel Market Updated</a>");
    }

    @Nullable
    PersonnelMarketEntry pickEntry(Map<PersonnelRole, PersonnelMarketEntry> marketEntries) {
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

    private static void processApplicantNodes(NewPersonnelMarket personnelMarket, Node wn, Version version) {
        logger.debug("Loading Applicant Nodes from XML...");

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

            Person applicant = Person.generateInstanceFromXML(currentChild, personnelMarket.getCampaign(), version);

            if (applicant != null) {
                personnelMarket.addApplicant(applicant);
            }
        }

        logger.debug("Load Applicant Nodes Complete!");
    }
}
