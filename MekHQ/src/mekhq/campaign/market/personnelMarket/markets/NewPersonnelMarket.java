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

/**
 * Represents the Personnel Market system for managing the recruitment, listing, and data persistence of potential
 * recruits within a campaign.
 *
 * <p>Handles the generation, filtration, and display of available applicants, as well as reading and writing
 * applicant data from/to XML. Integrates campaign state, planetary context, reputation effects, market style, and
 * market-specific applicant pools for a flexible personnel recruitment experience.</p>
 *
 * @author Illiani
 * @since 0.50.06
 */
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

    /**
     * Creates a new Personnel Market instance bound to a campaign.
     *
     * @param campaign the parent {@link Campaign} instance to associate with this personnel market
     *
     * @author Illiani
     * @since 0.50.06
     */
    public NewPersonnelMarket(Campaign campaign) {
        this.campaign = campaign;

        logger.debug("Initializing NewPersonnelMarket");

        clanMarketEntries = new HashMap<>();
        innerSphereMarketEntries = new HashMap<>();
    }

    /**
     * Generates Personnel Market data by loading it from an XML document node.
     *
     * @param campaign   The relevant campaign save
     * @param parentNode XML node parent containing market data
     * @param version    Version the save was last made in
     * @return Loaded Personnel Market instance
     * @author Illiani
     * @since 0.50.06
     */
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

    /**
     * Gathers new applicant data and updates the internal applicant list based on current campaign state and settings.
     *
     * @author Illiani
     * @since 0.50.06
     */
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

    /**
     * Generates new applicants and adds them to the market's applicant pool.
     *
     * @author Illiani
     * @since 0.50.06
     */
    public void generateApplicants() {
    }

    /**
     * Launches the user interface dialog displaying the current personnel market.
     *
     * @author Illiani
     * @since 0.50.06
     */
    public void showPersonnelMarketDialog() {
        new PersonnelMarketDialog(this);
    }

    /**
     * Serializes the Personnel Market data to XML output.
     *
     * @param writer Output PrintWriter
     * @param indent XML indentation level
     * @author Illiani
     * @since 0.50.06
     */
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

    /**
     * Gets the associated personnel market style used by this market.
     *
     * @return the personnel market style
     * @author Illiani
     * @since 0.50.06
     */
    public PersonnelMarketStyle getAssociatedPersonnelMarketStyle() {
        return associatedPersonnelMarketStyle;
    }

    /**
     * Sets the market style for the personnel market.
     *
     * @param associatedPersonnelMarketStyle the personnel market style to use
     * @author Illiani
     * @since 0.50.06
     */
    public void setAssociatedPersonnelMarketStyle(PersonnelMarketStyle associatedPersonnelMarketStyle) {
        this.associatedPersonnelMarketStyle = associatedPersonnelMarketStyle;
    }

    /**
     * Gets the recruitment divider value for low population systems.
     *
     * @return recruitment divider integer
     * @author Illiani
     * @since 0.50.06
     */
    public int getLowPopulationRecruitmentDivider() {
        return LOW_POPULATION_RECRUITMENT_DIVIDER;
    }

    /**
     * Sets the recruitment divider for low population systems.
     *
     * @param lowPopulationRecruitmentDivider recruitment divider to set
     * @author Illiani
     * @since 0.50.06
     */
    public void setLowPopulationRecruitmentDivider(int lowPopulationRecruitmentDivider) {
        LOW_POPULATION_RECRUITMENT_DIVIDER = lowPopulationRecruitmentDivider;
    }

    /**
     * Gets the unit reputation cutoff value for recruitment.
     *
     * @return reputation cutoff threshold
     * @author Illiani
     * @since 0.50.06
     */
    public int getUnitReputationRecruitmentCutoff() {
        return UNIT_REPUTATION_RECRUITMENT_CUTOFF;
    }

    /**
     * Sets the cutoff value for unit reputation in recruitment eligibility.
     *
     * @param unitReputationRecruitmentCutoff reputation cutoff to set
     * @author Illiani
     * @since 0.50.06
     */
    public void setUnitReputationRecruitmentCutoff(int unitReputationRecruitmentCutoff) {
        UNIT_REPUTATION_RECRUITMENT_CUTOFF = unitReputationRecruitmentCutoff;
    }

    /**
     * Returns the map of available clan market entries and their role distributions.
     *
     * @return map of personnel roles to market entries (Clan)
     * @author Illiani
     * @since 0.50.06
     */
    public Map<PersonnelRole, PersonnelMarketEntry> getClanMarketEntries() {
        return clanMarketEntries;
    }

    /**
     * Sets the clan market entries data.
     *
     * @param clanMarketEntries map of personnel roles to market entries
     * @author Illiani
     * @since 0.50.06
     */
    public void setClanMarketEntries(Map<PersonnelRole, PersonnelMarketEntry> clanMarketEntries) {
        this.clanMarketEntries = clanMarketEntries;
    }

    /**
     * Returns the map of available Inner Sphere market entries and their role distributions.
     *
     * @return map of personnel roles to market entries (Inner Sphere)
     * @author Illiani
     * @since 0.50.06
     */
    public Map<PersonnelRole, PersonnelMarketEntry> getInnerSphereMarketEntries() {
        return innerSphereMarketEntries;
    }

    /**
     * Sets the Inner Sphere market entries data.
     *
     * @param innerSphereMarketEntries map of personnel roles to market entries
     * @author Illiani
     * @since 0.50.06
     */
    public void setInnerSphereMarketEntries(Map<PersonnelRole, PersonnelMarketEntry> innerSphereMarketEntries) {
        this.innerSphereMarketEntries = innerSphereMarketEntries;
    }

    /**
     * Returns the campaign associated with this Personnel Market.
     *
     * @return campaign instance
     * @author Illiani
     * @since 0.50.06
     */
    public Campaign getCampaign() {
        return campaign;
    }

    /**
     * Returns the faction associated with the campaign for recruitment filtering.
     *
     * @return campaign's faction
     * @author Illiani
     * @since 0.50.06
     */
    public Faction getCampaignFaction() {
        if (campaignFaction == null) {
            campaignFaction = campaign.getFaction();
        }
        return campaignFaction;
    }

    /**
     * Sets the campaign's faction, influencing applicant origins.
     *
     * @param campaignFaction the new campaign faction
     * @author Illiani
     * @since 0.50.06
     */
    public void setCampaignFaction(Faction campaignFaction) {
        this.campaignFaction = campaignFaction;
    }

    /**
     * Returns the in-game date currently used for market evaluation.
     *
     * @return current date
     * @author Illiani
     * @since 0.50.06
     */
    public LocalDate getToday() {
        if (today == null) {
            today = campaign.getLocalDate();
        }
        return today;
    }

    /**
     * Sets the in-game date used to determine market status.
     *
     * @param today new current date
     * @author Illiani
     * @since 0.50.06
     */
    public void setToday(LocalDate today) {
        this.today = today;
    }

    /**
     * Returns the campaign year for market behavior.
     *
     * @return game year
     * @author Illiani
     * @since 0.50.06
     */
    public int getGameYear() {
        if (gameYear == 0) {
            gameYear = today.getYear();
        }
        return gameYear;
    }

    /**
     * Returns the factions from which applicants may originate.
     *
     * @return list of applicant origin factions
     * @author Illiani
     * @since 0.50.06
     */
    public ArrayList<Faction> getApplicantOriginFactions() {
        return new ArrayList<>();
    }

    /**
     * Sets the list of origin factions for new applicants.
     *
     * @param applicantOriginFactions list to set
     * @author Illiani
     * @since 0.50.06
     */
    public void setApplicantOriginFactions(List<Faction> applicantOriginFactions) {
        this.applicantOriginFactions = applicantOriginFactions;
    }

    /**
     * Returns the current list of market applicants.
     *
     * @return list of applicants
     * @author Illiani
     * @since 0.50.06
     */
    public List<Person> getCurrentApplicants() {
        if (currentApplicants == null) {
            return new ArrayList<>();
        }
        return currentApplicants;
    }

    /**
     * Returns the index or filter value last used for applicant selection.
     *
     * @return filter index
     * @author Illiani
     * @since 0.50.06
     */
    public int getLastSelectedFilter() {
        return lastSelectedFilter;
    }

    /**
     * Sets the last-used selection filter value.
     *
     * @param lastSelectedFilter filter index
     * @author Illiani
     * @since 0.50.06
     */
    public void setLastSelectedFilter(int lastSelectedFilter) {
        this.lastSelectedFilter = lastSelectedFilter;
    }

    /**
     * Adds a new applicant to the current applicant pool.
     *
     * @param applicant the new Personnel applicant
     * @author Illiani
     * @since 0.50.06
     */
    public void addApplicant(Person applicant) {
        if (applicant != null && !currentApplicants.contains(applicant)) {
            currentApplicants.add(applicant);
        }
    }

    /**
     * Clears all current applicants from the market.
     *
     * @author Illiani
     * @since 0.50.06
     */
    public void clearCurrentApplicants() {
        setCurrentApplicants(new ArrayList<>());
    }

    /**
     * Sets the list of current market applicants.
     *
     * @param currentApplicants list of applicants to set
     * @author Illiani
     * @since 0.50.06
     */
    public void setCurrentApplicants(List<Person> currentApplicants) {
        this.currentApplicants = currentApplicants;
    }

    /**
     * Returns whether a golden hello (recruitment incentive) is being offered.
     *
     * @return {@code true} if a golden hello is offered, otherwise {@code false}
     * @author Illiani
     * @since 0.50.06
     */
    public boolean isOfferingGoldenHello() {
        return offeringGoldenHello;
    }

    /**
     * Sets whether a golden hello (recruitment incentive) is being offered.
     *
     * @param offeringGoldenHello {@code true} to offer, {@code false} otherwise
     * @author Illiani
     * @since 0.50.06
     */
    public void setOfferingGoldenHello(boolean offeringGoldenHello) {
        this.offeringGoldenHello = offeringGoldenHello;
    }

    /**
     * Returns whether rare personnel are available on the market.
     *
     * @return {@code true} if rare personnel are present, otherwise {@code false}
     * @author Illiani
     * @since 0.50.06
     */
    public boolean getHasRarePersonnel() {
        return hasRarePersonnel;
    }

    /**
     * Sets the status of rare personnel availability.
     *
     * @param hasRarePersonnel {@code true} if present, {@code false} otherwise
     * @author Illiani
     * @since 0.50.06
     */
    public void setHasRarePersonnel(boolean hasRarePersonnel) {
        this.hasRarePersonnel = hasRarePersonnel;
    }

    /**
     * Returns the number of recruitment rolls performed for the current market evaluation.
     *
     * @return recruitment rolls value
     * @author Illiani
     * @since 0.50.06
     */
    public int getRecruitmentRolls() {
        return recruitmentRolls;
    }

    /**
     * Sets the number of recruitment rolls for the market.
     *
     * @param recruitmentRolls number of rolls to set
     * @author Illiani
     * @since 0.50.06
     */
    public void setRecruitmentRolls(int recruitmentRolls) {
        this.recruitmentRolls = recruitmentRolls;
    }

    /**
     * Returns the planetary system currently used for market context.
     *
     * @return planetary system
     * @author Illiani
     * @since 0.50.06
     */
    public PlanetarySystem getCurrentSystem() {
        if (currentSystem == null) {
            currentSystem = campaign.getCurrentSystem();
        }
        return currentSystem;
    }

    /**
     * Sets the planetary system for the current personnel market.
     *
     * @param currentSystem planetary system to set
     * @author Illiani
     * @since 0.50.06
     */
    public void setCurrentSystem(PlanetarySystem currentSystem) {
        this.currentSystem = currentSystem;
    }

    /**
     * Gets an availability message describing the market's state or conditions.
     *
     * @return market availability message
     * @author Illiani
     * @since 0.50.06
     */
    public String getAvailabilityMessage() {
        return "";
    }

    /**
     * Reinitializes market state and key internal data.
     *
     * @author Illiani
     * @since 0.50.06
     */
    void reinitializeKeyData() {
        campaignFaction = campaign.getFaction();
        today = campaign.getLocalDate();
        gameYear = today.getYear();
        currentSystem = campaign.getCurrentSystem();
    }

    /**
     * Generates a new single applicant using the specified market entry data.
     *
     * @param marketEntries market entries for role selection
     *
     * @return The generated Person object, or null if generation failed
     *
     * @author Illiani
     * @since 0.50.06
     */
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

    /**
     * Generates a personnel recruitment report for the specified campaign.
     *
     * @param campaign the campaign to report on
     * @author Illiani
     * @since 0.50.06
     */
    void generatePersonnelReport(Campaign campaign) {
        campaign.addReport("<a href='PERSONNEL_MARKET'>Personnel Market Updated</a>");
    }

    /**
     * Picks a personnel market entry from the given entry set, typically based on role and weighting.
     *
     * @param marketEntries map of personnel roles to entries
     *
     * @return selected market entry, or null if none available
     *
     * @author Illiani
     * @since 0.50.06
     */
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

    /**
     * Processes an XML node containing applicant data and loads it into the supplied personnel market.
     *
     * @param personnelMarket the personnel market instance being loaded
     * @param wn              the XML node representing applicant data
     * @param version         serialization version
     * @author Illiani
     * @since 0.50.06
     */
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
