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
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.io.PrintWriter;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
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
    private static final String RESOURCE_BUNDLE = "mekhq.resources.PersonnelMarket";
    private static final MMLogger logger = MMLogger.create(NewPersonnelMarket.class);

    @SuppressWarnings(value = "FieldCanBeLocal")
    private static int RARE_PROFESSION_WEIGHT = 20;
    private static int LOW_POPULATION_RECRUITMENT_DIVIDER = 1;
    private static int UNIT_REPUTATION_RECRUITMENT_CUTOFF = Integer.MIN_VALUE;
    @SuppressWarnings(value = "FieldCanBeLocal")
    private static int PROFESSION_EXTINCTION_IGNORE_VALUE = -1;

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
    List<PersonnelRole> rareProfessions = new ArrayList<>();
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

        logger.info("Loading Personnel Market Nodes from XML...");

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
                } else if (nodeName.equalsIgnoreCase("rareProfessions")) {
                    processRareProfessionNodes(personnelMarket, childNode);
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
        setApplicantOriginFactions(getApplicantOriginFactions());

        String isZeroAvailability = getAvailabilityMessage();

        if (!isZeroAvailability.isBlank()) {
            logger.debug("No applicants will be generated due to {}", isZeroAvailability);
            return;
        }

        generateApplicants();

        if (currentApplicants.isEmpty()) {
            logger.debug("No applicants were generated.");
        } else {
            logger.debug("Generated {} applicants for the campaign.", currentApplicants.size());

            if (campaign.getCampaignOptions().isPersonnelMarketReportRefresh()) {
                campaign.addReport(generatePersonnelReport());
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
        MHQXMLUtility.writeSimpleXMLOpenTag(writer, indent++, "rareProfessions");
        for (PersonnelRole profession : rareProfessions) {
            MHQXMLUtility.writeSimpleXMLTag(writer, indent, "rareProfession", profession.name());
        }
        MHQXMLUtility.writeSimpleXMLCloseTag(writer, --indent, "rareProfessions");
        MHQXMLUtility.writeSimpleXMLTag(writer, indent, "recruitmentRolls", recruitmentRolls);
        MHQXMLUtility.writeSimpleXMLTag(writer, indent, "lastSelectedFilter", lastSelectedFilter);
        MHQXMLUtility.writeSimpleXMLOpenTag(writer, indent++, "currentApplicants");
        for (final Person person : currentApplicants) {
            person.writeToXML(writer, indent, campaign);
        }
        MHQXMLUtility.writeSimpleXMLCloseTag(writer, --indent, "currentApplicants");
    }

    /**
     * @return the resource bundle name as a string
     */
    String getResourceBundle() {
        return RESOURCE_BUNDLE;
    }

    /**
     * @return the {@link MMLogger} instance
     */
    MMLogger getLogger() {
        return logger;
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
     * Sets the current game year for the personnel market.
     *
     * @param gameYear the game year to set
     *
     * @author Illiani
     * @since 0.50.06
     */
    public void setGameYear(int gameYear) {
        this.gameYear = gameYear;
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
     * Returns a list of rare professions currently stored.
     *
     * @return a list of {@link PersonnelRole} representing rare professions
     *
     * @author Illiani
     * @since 0.50.06
     */
    public List<PersonnelRole> getRareProfessions() {
        return rareProfessions;
    }

    /**
     * Adds a profession to the list of rare professions.
     *
     * @param rareProfession the {@link PersonnelRole} to mark as rare and add to the list
     *
     * @author Illiani
     * @since 0.50.06
     */
    public void addRareProfession(PersonnelRole rareProfession) {
        rareProfessions.add(rareProfession);
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

        hasRarePersonnel = false;
        rareProfessions = new ArrayList<>();
        recruitmentRolls = 0;
        applicantOriginFactions = new ArrayList<>();
        currentApplicants = new ArrayList<>();
    }

    /**
     * Removes entries with non-positive weights or counts from the supplied personnel market entries map.
     *
     * <p>Iterates over the existing map and removes any {@link PersonnelMarketEntry} whose {@code weight()} or
     * {@code count()} is less than or equal to zero.</p>
     *
     * @param marketEntries the map of personnel roles to market entries to sanitize; modified in place
     * @return the sanitized map, containing only entries with positive weights
     * @author Illiani
     * @since 0.50.06
     */
    public Map<PersonnelRole, PersonnelMarketEntry> sanitizeMarketEntries(
          Map<PersonnelRole, PersonnelMarketEntry> marketEntries) {
        List<PersonnelRole> roles = new ArrayList<>(marketEntries.keySet());
        for (PersonnelRole role : roles) {
            PersonnelMarketEntry entry = marketEntries.get(role);
            // These roles can't be generated and will throw off the total weight
            if (entry.weight() <= 0) {
                logger.warn("Removing {} from market entries as it has a weight of < 1 ({}).",
                      entry.profession(),
                      entry.weight());
                marketEntries.remove(role);
                continue;
            }

            // These roles will throw an exception as you can't call randomInt() with a value < 1
            if (entry.count() <= 0) {
                logger.warn("Removing {} from market entries as it has a count of < 1 ({}).",
                      entry.profession(),
                      entry.count());
                marketEntries.remove(role);
            }
        }

        return marketEntries;
    }

    /**
     * Returns a list of {@link PersonnelMarketEntry} objects sorted alphabetically by the string value
     * of each entry's profession. This ensures a deterministic order for processing or testing, regardless of the
     * original iteration order of the provided map.
     *
     * @param marketEntries a map containing personnel roles as keys and their corresponding market entries as values
     * @return a list of market entries sorted alphabetically by profession
     * @author Illiani
     * @since 0.50.06
     */
    public List<PersonnelMarketEntry> getMarketEntriesAsList(Map<PersonnelRole, PersonnelMarketEntry> marketEntries) {
        // Maps are inherently non-deterministic in their order, however, we want to be able to both use a map (for
        // the key:value pairs, but also we need a deterministic list to ease testing. So we create a list here and
        // sort alphabetically based on the profession tied to each entry. This makes testing substantially easier
        // without removing from the randomness of the pick
        ArrayList<PersonnelMarketEntry> marketEntryList = new ArrayList<>(marketEntries.values());
        marketEntryList.sort(Comparator.comparing(entry -> entry.profession().toString()));

        return marketEntryList;
    }

    /**
     * Generates a single applicant person based on the provided personnel market entries.
     *
     * <p>This method selects an entry from the ordered market entries, iterates through available fallback
     * professions if necessary, and creates a new {@link Person}.</p>
     *
     * @param unorderedMarketEntries a mapping of {@link PersonnelRole} to {@link PersonnelMarketEntry} representing
     *                              all personnel market entries.
     * @param orderedMarketEntries a list of {@link PersonnelMarketEntry} objects, ordered by profession.
     * @return a newly generated {@link Person} representing the applicant, or {@code null} if no suitable
     *         applicant could be generated.
     */
    @Nullable
    Person generateSingleApplicant(Map<PersonnelRole, PersonnelMarketEntry> unorderedMarketEntries,
          List<PersonnelMarketEntry> orderedMarketEntries) {
        PersonnelMarketEntry entry = pickEntry(orderedMarketEntries);
        if (entry == null) {
            logger.error("No personnel market entries found. Check the data folder for the appropriate YAML file.");
            return null;
        }

        PersonnelMarketEntry originalEntry = entry;
        entry = vetEntryForIntroductionAndExtinctionYears(unorderedMarketEntries, entry);
        if (entry == null) {
            logger.error("Could not find a suitable fallback profession for {} game year {}. This suggests the " +
                               "fallback structure of the YAML file is incorrect.",
                  originalEntry.profession(),
                  gameYear);
            return null;
        }

        // If we have a valid entry, we now need to generate the applicant
        Faction applicantOriginFaction = getRandomItem(applicantOriginFactions);
        if (applicantOriginFaction == null) {
            logger.error("Could not find a valid applicant origin faction for game year {}.", gameYear);
            return null;
        }
        String originFactionCode = applicantOriginFaction.getShortName();

        Person applicant = campaign.newPerson(entry.profession(), originFactionCode, Gender.RANDOMIZE);
        if (applicant == null) {
            logger.warn("Could not create person for {} game year {} from faction {}",
                  originalEntry.profession(),
                  gameYear,
                  applicantOriginFaction);
            return null;
        }

        if (!hasRarePersonnel && (entry.weight() <= RARE_PROFESSION_WEIGHT)) {
            hasRarePersonnel = true;

            PersonnelRole profession = entry.profession();
            if (!rareProfessions.contains(profession)) {
                rareProfessions.add(profession);
            }
        }

        logger.debug("Generated applicant {} ({}) game year {} from faction {}",
              applicant.getFullName(),
              applicant.getPrimaryRole(),
              gameYear,
              applicantOriginFaction);

        return applicant;
    }

    /**
     * Attempts to find a valid {@link PersonnelMarketEntry} for the current game year.
     *
     * <p>Starting from the provided entry, checks whether its introduction and extinction years encompass the
     * current game year. If not, it repeatedly looks up fallback professions from the provided map, up to three
     * attempts, until a suitable entry is found or no more attempts remain.</p>
     *
     * @param unorderedMarketEntries a map of personnel roles to market entries, used to look up fallbacks
     * @param entry                  the initial {@link PersonnelMarketEntry} to validate and potentially fallback from
     *
     * @return a valid {@link PersonnelMarketEntry} for the current game year, or {@code null} if none can be found
     *
     * @author Illiani
     * @since 0.50.06
     */
    PersonnelMarketEntry vetEntryForIntroductionAndExtinctionYears(
          Map<PersonnelRole, PersonnelMarketEntry> unorderedMarketEntries, PersonnelMarketEntry entry) {
        int remainingIterations = 3;

        int introductionYear = entry.introductionYear();
        int extinctionYear = entry.extinctionYear();
        while (gameYear < introductionYear || (gameYear >= extinctionYear)) {
            PersonnelRole fallbackProfession = entry.fallbackProfession();
            entry = unorderedMarketEntries.get(fallbackProfession);

            remainingIterations--;
            if (entry == null || remainingIterations <= 0) {
                return null;
            }

            introductionYear = entry.introductionYear();
            extinctionYear = entry.extinctionYear();
            if (extinctionYear == PROFESSION_EXTINCTION_IGNORE_VALUE) {
                extinctionYear = Integer.MAX_VALUE;
            }
        }
        return entry;
    }

    /**
     * Generates a personnel recruitment report for the specified campaign.
     *
     * @author Illiani
     * @since 0.50.06
     */
    private String generatePersonnelReport() {
        return getTextAt(RESOURCE_BUNDLE, "hyperlink.personnelMarket.report");
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
    PersonnelMarketEntry pickEntry(List<PersonnelMarketEntry> marketEntries) {
        int totalWeight = 0;
        for (PersonnelMarketEntry entry : marketEntries) {
            // These entries should have already been filtered out, but a little insurance goes a long way
            if (entry.weight() > 0 && entry.count() > 0) {
                totalWeight += entry.weight();
            }
        }
        if (totalWeight <= 0) {
            return null;
        }

        int roll = randomInt(totalWeight);
        int cumulative = 0;
        for (PersonnelMarketEntry entry : marketEntries) {
            cumulative += entry.weight();
            if (roll < cumulative) {
                return entry;
            }
        }
        return null; // Should never hit here if weights > 0
    }

    /**
     * Processes an XML node containing applicant data and loads it into the supplied personnel market.
     *
     * @param personnelMarket the personnel market instance being loaded
     * @param parentNode              the XML node representing applicant data
     * @param version         serialization version
     * @author Illiani
     * @since 0.50.06
     */
    private static void processApplicantNodes(NewPersonnelMarket personnelMarket, Node parentNode, Version version) {
        logger.info("Loading Applicant Nodes from XML...");

        NodeList childNodes = parentNode.getChildNodes();

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

        logger.info("Load Applicant Nodes Complete!");
    }

    /**
     * Processes and loads "rareProfession" nodes from the given XML parent node into the provided
     * {@link NewPersonnelMarket}.
     *
     * <p>This method iterates over the child nodes of {@code parentNode}. For every child node named
     * "rareProfession", it tries to parse the node's text content into a {@link PersonnelRole}. If successful, the role
     * is added to the personnel market as a rare profession. </p>
     *
     * <p>Nodes that are not element nodes or do not have the correct name are skipped, with unknown node types
     * logged as errors.</p>
     *
     * @param personnelMarket the target market to which rare professions will be added
     * @param parentNode      the XML node whose children will be processed for rare profession entries
     *
     * @author Illiani
     * @since 0.50.06
     */
    private static void processRareProfessionNodes(NewPersonnelMarket personnelMarket, Node parentNode) {
        logger.info("Loading Rare Profession Nodes from XML...");

        NodeList childNodes = parentNode.getChildNodes();

        for (int x = 0; x < childNodes.getLength(); x++) {
            Node currentChild = childNodes.item(x);

            // If it's not an element node, we ignore it.
            if (currentChild.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            if (!currentChild.getNodeName().equalsIgnoreCase("rareProfession")) {
                logger.error("Unknown node type not loaded in Rare Profession nodes: {}", currentChild.getNodeName());
                continue;
            }

            PersonnelRole primaryRole = PersonnelRole.fromString(currentChild.getTextContent().trim());
            if (primaryRole != null) {
                personnelMarket.addRareProfession(primaryRole);
            }
        }

        logger.info("Load Kill Nodes Complete!");
    }
}
