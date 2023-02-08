/*
 * ContractMarket.java
 *
 * Copyright (c) 2014 Carl Spain. All rights reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.market;

import megamek.Version;
import megamek.codeUtilities.MathUtility;
import megamek.common.Compute;
import megamek.common.annotations.Nullable;
import megamek.common.enums.SkillLevel;
import mekhq.MekHQ;
import mekhq.utilities.MHQXMLUtility;
import mekhq.campaign.Campaign;
import mekhq.campaign.JumpPath;
import mekhq.campaign.market.enums.ContractMarketMethod;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.Contract;
import mekhq.campaign.mission.Mission;
import mekhq.campaign.mission.enums.AtBContractType;
import mekhq.campaign.mission.enums.ContractCommandRights;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.rating.IUnitRating;
import mekhq.campaign.universe.*;
import org.apache.logging.log4j.LogManager;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.PrintWriter;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Contract offers that are generated monthly under AtB rules.
 *
 * Based on PersonnelMarket
 *
 * @author Neoancient
 */
public class ContractMarket {
    // TODO: Implement a method that rolls each day to see whether a new contract appears or an offer disappears

    public final static int CLAUSE_COMMAND = 0;
    public final static int CLAUSE_SALVAGE = 1;
    public final static int CLAUSE_SUPPORT = 2;
    public final static int CLAUSE_TRANSPORT = 3;
    public final static int CLAUSE_NUM = 4;

    /**
     * An arbitrary maximum number of attempts to generate a contract.
     */
    private final static int MAXIMUM_GENERATION_RETRIES = 3;

    /**
     * An arbitrary maximum number of attempts to find a random employer faction that
     * is not a Mercenary.
     */
    private final static int MAXIMUM_ATTEMPTS_TO_FIND_NON_MERC_EMPLOYER = 20;

    private ContractMarketMethod method = ContractMarketMethod.ATB_MONTHLY;

    private List<Contract> contracts;
    private int lastId = 0;
    private Map<Integer, Contract> contractIds;
    private Map<Integer, ClauseMods> clauseMods;

    /* It is possible to call addFollowup more than once for the
     * same contract by canceling the dialog and running it again;
     * this is the easiest place to track it to prevent
     * multiple followup contracts.
     * key: followup id
     * value: main contract id
     */
    private HashMap<Integer, Integer> followupContracts;

    public ContractMarket() {
        contracts = new ArrayList<>();
        contractIds = new HashMap<>();
        clauseMods = new HashMap<>();
        followupContracts = new HashMap<>();
    }

    public List<Contract> getContracts() {
        return contracts;
    }

    public void removeContract(Contract c) {
        contracts.remove(c);
        contractIds.remove(c.getId());
        clauseMods.remove(c.getId());
        followupContracts.remove(c.getId());
    }

    public AtBContract addAtBContract(Campaign campaign) {
        AtBContract c = generateAtBContract(campaign, campaign.getUnitRatingMod());
        if (c != null) {
            contracts.add(c);
        }
        return c;
    }

    public int getRerollsUsed(Contract c, int clause) {
        if (null != clauseMods.get(c.getId())) {
            return clauseMods.get(c.getId()).rerollsUsed[clause];
        }
        return 0;
    }

    public void rerollClause(AtBContract c, int clause, Campaign campaign) {
        if (null != clauseMods.get(c.getId())) {
            switch (clause) {
                case CLAUSE_COMMAND:
                    rollCommandClause(c, clauseMods.get(c.getId()).mods[clause]);
                    break;
                case CLAUSE_SALVAGE:
                    rollSalvageClause(c, clauseMods.get(c.getId()).mods[clause]);
                    break;
                case CLAUSE_TRANSPORT:
                    rollTransportClause(c, clauseMods.get(c.getId()).mods[clause]);
                    break;
                case CLAUSE_SUPPORT:
                    rollSupportClause(c, clauseMods.get(c.getId()).mods[clause]);
                    break;
            }
            clauseMods.get(c.getId()).rerollsUsed[clause]++;
            c.calculateContract(campaign);
        }
    }

    public void generateContractOffers(Campaign campaign) {
        generateContractOffers(campaign, false);
    }

    public void generateContractOffers(Campaign campaign, boolean newCampaign) {
        if (((method == ContractMarketMethod.ATB_MONTHLY) && (campaign.getLocalDate().getDayOfMonth() == 1))
                || newCampaign) {
            // need to copy to prevent concurrent modification errors
            new ArrayList<>(contracts).forEach(this::removeContract);

            int unitRatingMod = campaign.getUnitRatingMod();

            for (AtBContract contract : campaign.getActiveAtBContracts()) {
                checkForSubcontracts(campaign, contract, unitRatingMod);
            }

            int numContracts = Compute.d6() - 4 + unitRatingMod;

            Set<Faction> currentFactions = campaign.getCurrentSystem().getFactionSet(campaign.getLocalDate());
            final boolean inMinorFaction = currentFactions.stream().noneMatch(faction ->
                    faction.isISMajorOrSuperPower() || faction.isClan());
            if (inMinorFaction) {
                numContracts--;
            }

            boolean inBackwater = true;
            if (currentFactions.size() > 1) {
                // More than one faction, if any is *not* periphery, we're not in backwater either
                for (Faction f : currentFactions) {
                    if (!f.isPeriphery()) {
                        inBackwater = false;
                    }
                }
            } else if (!currentFactions.isEmpty()) {
                // Just one faction. Are there any others nearby?
                Faction onlyFaction = currentFactions.iterator().next();
                if (!onlyFaction.isPeriphery()) {
                    for (PlanetarySystem key : Systems.getInstance().getNearbySystems(campaign.getCurrentSystem(), 30)) {
                        for (Faction f : key.getFactionSet(campaign.getLocalDate())) {
                            if (!onlyFaction.equals(f)) {
                                inBackwater = false;
                                break;
                            }
                        }
                        if (!inBackwater) {
                            break;
                        }
                    }
                }
            } else {
                LogManager.getLogger().warn(
                        "Unable to find any factions around "
                            + campaign.getCurrentSystem().getName(campaign.getLocalDate())
                            + " on "
                            + campaign.getLocalDate());
            }

            if (inBackwater) {
                numContracts--;
            }

            if (campaign.getFactionCode().equals("MERC") || campaign.getFactionCode().equals("PIR")) {
                if (campaign.getAtBConfig().isHiringHall(campaign.getCurrentSystem().getId(), campaign.getLocalDate())) {
                    numContracts++;
                    /* Though the rules do not state these modifiers are mutually exclusive, the fact that the
                     * distance of Galatea from a border means that it has no advantage for Mercs over border
                     * worlds. Common sense dictates that worlds with hiring halls should not be
                     * subject to the -1 for backwater/interior.
                     */
                    if (inBackwater) {
                        numContracts++;
                    }
                }
            } else {
                /* Per IOps Beta, government units determine number of contracts as on a system with a great hall */
                numContracts++;
            }

            /* If located on a faction's capital (interpreted as the starting planet for that faction),
             * generate one contract offer for that faction.
             */
            for (Faction f : campaign.getCurrentSystem().getFactionSet(campaign.getLocalDate())) {
                try {
                    if (f.getStartingPlanet(campaign.getLocalDate()).equals(campaign.getCurrentSystem().getId())
                            && RandomFactionGenerator.getInstance().getEmployerSet().contains(f.getShortName())) {
                        AtBContract c = generateAtBContract(campaign, f.getShortName(), unitRatingMod);
                        if (c != null) {
                            contracts.add(c);
                            break;
                        }
                    }
                } catch (ArrayIndexOutOfBoundsException e) {
                    //no starting planet in current era; continue to next faction
                }
            }

            if (newCampaign) {
                numContracts = Math.max(numContracts, 2);
            }

            for (int i = 0; i < numContracts; i++) {
                AtBContract c = generateAtBContract(campaign, unitRatingMod);
                if (c != null) {
                    contracts.add(c);
                }
            }
            if (campaign.getCampaignOptions().isContractMarketReportRefresh()) {
                campaign.addReport("<a href='CONTRACT_MARKET'>Contract market updated</a>");
            }
        }
    }

    private void checkForSubcontracts(Campaign campaign, AtBContract contract, int unitRatingMod) {
        if (contract.getContractType().isGarrisonDuty()) {
            int numSubcontracts = 0;
            for (AtBContract c : campaign.getAtBContracts()) {
                if (contract.equals(c.getParentContract())) {
                    numSubcontracts++;
                }
            }
            for (int i = numSubcontracts; i < unitRatingMod - 1; i++) {
                int roll = Compute.d6(2);
                if (roll >= 10) {
                    AtBContract sub = generateAtBSubcontract(campaign, contract, unitRatingMod);
                    if (sub.getEndingDate().isBefore(contract.getEndingDate())) {
                        contracts.add(sub);
                    }
                }
            }
        }
    }

    /* If no suitable planet can be found or no jump path to the planet can be calculated after
     * the indicated number of retries, this will return null.
     */
    private @Nullable AtBContract generateAtBContract(Campaign campaign, int unitRatingMod) {
        if (campaign.getFactionCode().equals("MERC")) {
            if (null == campaign.getRetainerEmployerCode()) {
                int retries = MAXIMUM_GENERATION_RETRIES;
                AtBContract retVal = null;
                while ((retries > 0) && (retVal == null)) {
                    // Send only 1 retry down because we're handling retries in our loop
                    retVal = generateAtBContract(campaign, RandomFactionGenerator.getInstance().getEmployer(),
                            unitRatingMod, 1);
                    retries--;
                }
                return retVal;
            } else {
                return generateAtBContract(campaign, campaign.getRetainerEmployerCode(), unitRatingMod);
            }
        } else {
            return generateAtBContract(campaign, campaign.getFactionCode(), unitRatingMod);
        }
    }

    private @Nullable AtBContract generateAtBContract(Campaign campaign, @Nullable String employer, int unitRatingMod) {
        return generateAtBContract(campaign, employer, unitRatingMod, MAXIMUM_GENERATION_RETRIES);
    }

    private @Nullable AtBContract generateAtBContract(Campaign campaign, @Nullable String employer, int unitRatingMod, int retries) {
        if (employer == null) {
            LogManager.getLogger().warn("Could not generate an AtB Contract because there was no employer!");
            return null;
        } else if (retries <= 0) {
            LogManager.getLogger().warn("Could not generate an AtB Contract because we ran out of retries!");
            return null;
        }

        AtBContract contract = new AtBContract("UnnamedContract");
        lastId++;
        contract.setId(lastId);
        contractIds.put(lastId, contract);

        if (employer.equals("MERC")) {
            contract.setMercSubcontract(true);
            for (int attempts = 0; attempts < MAXIMUM_ATTEMPTS_TO_FIND_NON_MERC_EMPLOYER; ++attempts) {
                employer = RandomFactionGenerator.getInstance().getEmployer();
                if ((employer != null) && !employer.equals("MERC")) {
                    break;
                }
            }

            if ((employer == null) || employer.equals("MERC")) {
                LogManager.getLogger().warn("Could not generate an AtB Contract because we could not find a non-MERC employer!");
                return null;
            }
        }
        contract.setEmployerCode(employer, campaign.getGameYear());
        contract.setContractType(findAtBMissionType(unitRatingMod,
                Factions.getInstance().getFaction(contract.getEmployerCode()).isISMajorOrSuperPower()));

        if (contract.getContractType().isPirateHunting()) {
            contract.setEnemyCode("PIR");
        } else if (contract.getContractType().isRiotDuty()) {
            contract.setEnemyCode("REB");
        } else {
            contract.setEnemyCode(RandomFactionGenerator.getInstance().getEnemy(contract.getEmployerCode(),
                    contract.getContractType().isGarrisonType()));
        }

        if (contract.getContractType().isGarrisonDuty() && contract.getEnemyCode().equals("REB")) {
            contract.setContractType(AtBContractType.RIOT_DUTY);
        }

        /* Addition to AtB rules: factions which are generally neutral
         * (ComStar, Mercs not under contract) are more likely to have garrison-type
         * contracts and less likely to have battle-type contracts unless at war.
         */
        if (RandomFactionGenerator.getInstance().getFactionHints().isNeutral(Factions.getInstance().getFaction(employer)) &&
                !RandomFactionGenerator.getInstance().getFactionHints().isAtWarWith(Factions.getInstance().getFaction(employer),
                        Factions.getInstance().getFaction(contract.getEnemyCode()), campaign.getLocalDate())) {
            if (contract.getContractType().isPlanetaryAssault()) {
                contract.setContractType(AtBContractType.GARRISON_DUTY);
            } else if (contract.getContractType().isReliefDuty()) {
                contract.setContractType(AtBContractType.SECURITY_DUTY);
            }
        }

        // FIXME : Windchild : I don't work properly
        boolean isAttacker = !contract.getContractType().isGarrisonType()
                || (contract.getContractType().isReliefDuty() && (Compute.d6() < 4))
                || contract.getEnemyCode().equals("REB");
        if (isAttacker) {
            contract.setSystemId(RandomFactionGenerator.getInstance().getMissionTarget(contract.getEmployerCode(), contract.getEnemyCode()));
        } else {
            contract.setSystemId(RandomFactionGenerator.getInstance().getMissionTarget(contract.getEnemyCode(), contract.getEmployerCode()));
        }
        if (contract.getSystem() == null) {
            LogManager.getLogger().warn("Could not find contract location for "
                            + contract.getEmployerCode() + " vs. " + contract.getEnemyCode());
            return generateAtBContract(campaign, employer, unitRatingMod, retries - 1);
        }
        JumpPath jp = null;
        try {
            jp = contract.getJumpPath(campaign);
        } catch (NullPointerException ex) {
            // could not calculate jump path; leave jp null
            LogManager.getLogger().warn("Could not calculate jump path to contract location: "
                            + contract.getSystem().getName(campaign.getLocalDate()), ex);
        }

        if (jp == null) {
            return generateAtBContract(campaign, employer, unitRatingMod, retries - 1);
        }

        setAllyRating(contract, isAttacker, campaign.getGameYear());
        setEnemyRating(contract, isAttacker, campaign.getGameYear());

        if (contract.getContractType().isCadreDuty()) {
            contract.setAllySkill(SkillLevel.GREEN);
            contract.setAllyQuality(IUnitRating.DRAGOON_F);
        }

        contract.calculateLength(campaign.getCampaignOptions().isVariableContractLength());
        setAtBContractClauses(contract, unitRatingMod, campaign);

        contract.calculatePaymentMultiplier(campaign);

        contract.setPartsAvailabilityLevel(contract.getContractType().calculatePartsAvailabilityLevel());

        contract.initContractDetails(campaign);
        contract.calculateContract(campaign);

        contract.setName(String.format("%s - %s - %s %s",
                contract.getStartDate().format(DateTimeFormatter.ofPattern("yyyy")
                        .withLocale(MekHQ.getMHQOptions().getDateLocale())), employer,
                        contract.getSystem().getName(contract.getStartDate()), contract.getContractType()));

        return contract;
    }

    protected AtBContract generateAtBSubcontract(Campaign campaign,
            AtBContract parent, int unitRatingMod) {
        AtBContract contract = new AtBContract("New Subcontract");
        contract.setEmployerCode(parent.getEmployerCode(), campaign.getGameYear());
        contract.setContractType(findAtBMissionType(unitRatingMod,
                Factions.getInstance().getFaction(contract.getEmployerCode()).isISMajorOrSuperPower()));

        if (contract.getContractType().isPirateHunting()) {
            contract.setEnemyCode("PIR");
        } else if (contract.getContractType().isRiotDuty()) {
            contract.setEnemyCode("REB");
        } else {
            contract.setEnemyCode(RandomFactionGenerator.getInstance().getEnemy(contract.getEmployerCode(),
                    contract.getContractType().isGarrisonType()));
        }
        if (contract.getContractType().isGarrisonDuty() && contract.getEnemyCode().equals("REB")) {
            contract.setContractType(AtBContractType.RIOT_DUTY);
        }

        contract.setParentContract(parent);
        contract.initContractDetails(campaign);
        lastId++;
        contract.setId(lastId);
        contractIds.put(lastId, contract);

        /* The AtB rules say to roll the enemy, but also that the subcontract
         * takes place in the same planet/sector. Rebels and pirates can
         * appear anywhere, but others should be limited to what's within a
         * jump. */

        // TODO : When MekHQ gets the capability of splitting the unit to different locations, this
        // TODO : restriction can be lessened or lifted.
        if (!contract.getEnemy().isRebelOrPirate()) {
            boolean factionValid = false;
            for (PlanetarySystem p : Systems.getInstance().getNearbySystems(campaign.getCurrentSystem(), 30)) {
                if (factionValid) {
                    break;
                }

                for (Faction f : p.getFactionSet(campaign.getLocalDate())) {
                    if (f.getShortName().equals(contract.getEnemyCode())) {
                        factionValid = true;
                        break;
                    }
                }
            }
            if (!factionValid) {
                contract.setEnemyCode(parent.getEnemyCode());
            }
        }

        // FIXME : Windchild : I don't work properly
        boolean isAttacker = !contract.getContractType().isGarrisonType()
                || (contract.getContractType().isReliefDuty() && (Compute.d6() < 4))
                || contract.getEnemyCode().equals("REB");
        contract.setSystemId(parent.getSystemId());
        setAllyRating(contract, isAttacker, campaign.getGameYear());
        setEnemyRating(contract, isAttacker, campaign.getGameYear());

        if (contract.getContractType().isCadreDuty()) {
            contract.setAllySkill(SkillLevel.GREEN);
            contract.setAllyQuality(IUnitRating.DRAGOON_F);
        }
        contract.calculateLength(campaign.getCampaignOptions().isVariableContractLength());

        contract.setCommandRights(ContractCommandRights.values()[Math.max(parent.getCommandRights().ordinal() - 1, 0)]);
        contract.setSalvageExchange(parent.isSalvageExchange());
        contract.setSalvagePct(Math.max(parent.getSalvagePct() - 10, 0));
        contract.setStraightSupport(Math.max(parent.getStraightSupport() - 20,
                0));
        if (parent.getBattleLossComp() <= 10) {
            contract.setBattleLossComp(0);
        } else if (parent.getBattleLossComp() <= 20) {
            contract.setBattleLossComp(10);
        } else {
            contract.setBattleLossComp(parent.getBattleLossComp() - 20);
        }
        contract.setTransportComp(100);

        contract.calculatePaymentMultiplier(campaign);
        contract.setPartsAvailabilityLevel(contract.getContractType().calculatePartsAvailabilityLevel());
        contract.calculateContract(campaign);

        contract.setName(String.format("%s - %s - %s Subcontract %s",
                contract.getStartDate().format(DateTimeFormatter.ofPattern("yyyy")
                        .withLocale(MekHQ.getMHQOptions().getDateLocale())), contract.getEmployer(),
                contract.getSystem().getName(parent.getStartDate()), contract.getContractType()));

        return contract;
    }

    public void addFollowup(Campaign campaign,
            AtBContract contract) {
        if (followupContracts.containsValue(contract.getId())) {
            return;
        }
        AtBContract followup = new AtBContract("Followup Contract");
        followup.setEmployerCode(contract.getEmployerCode(), campaign.getGameYear());
        followup.setEnemyCode(contract.getEnemyCode());
        followup.setSystemId(contract.getSystemId());
        switch (contract.getContractType()) {
            case DIVERSIONARY_RAID:
                followup.setContractType(AtBContractType.OBJECTIVE_RAID);
                break;
            case RECON_RAID:
                followup.setContractType(AtBContractType.PLANETARY_ASSAULT);
                break;
            case RIOT_DUTY:
                followup.setContractType(AtBContractType.GARRISON_DUTY);
                break;
            default:
                break;
        }
        followup.setAllySkill(contract.getAllySkill());
        followup.setAllyQuality(contract.getAllyQuality());
        followup.setEnemySkill(contract.getEnemySkill());
        followup.setEnemyQuality(contract.getEnemyQuality());
        followup.calculateLength(campaign.getCampaignOptions().isVariableContractLength());
        setAtBContractClauses(followup, campaign.getUnitRatingMod(), campaign);

        followup.calculatePaymentMultiplier(campaign);

        followup.setPartsAvailabilityLevel(followup.getContractType().calculatePartsAvailabilityLevel());

        followup.initContractDetails(campaign);
        followup.calculateContract(campaign);
        lastId++;
        followup.setId(lastId);
        contractIds.put(lastId, followup);

        contracts.add(followup);
        followupContracts.put(followup.getId(), contract.getId());
    }

    protected AtBContractType findAtBMissionType(int unitRatingMod, boolean majorPower) {
        final AtBContractType[][] table = {
            // col 0: IS Houses
            { AtBContractType.GUERRILLA_WARFARE, AtBContractType.RECON_RAID, AtBContractType.PIRATE_HUNTING,
                    AtBContractType.PLANETARY_ASSAULT, AtBContractType.OBJECTIVE_RAID, AtBContractType.OBJECTIVE_RAID,
                    AtBContractType.EXTRACTION_RAID, AtBContractType.RECON_RAID, AtBContractType.GARRISON_DUTY,
                    AtBContractType.CADRE_DUTY, AtBContractType.RELIEF_DUTY },
            // col 1: Others
                { AtBContractType.GUERRILLA_WARFARE, AtBContractType.RECON_RAID, AtBContractType.PLANETARY_ASSAULT,
                        AtBContractType.OBJECTIVE_RAID, AtBContractType.EXTRACTION_RAID, AtBContractType.PIRATE_HUNTING,
                        AtBContractType.SECURITY_DUTY, AtBContractType.OBJECTIVE_RAID, AtBContractType.GARRISON_DUTY,
                        AtBContractType.CADRE_DUTY, AtBContractType.DIVERSIONARY_RAID }
        };
        int roll = MathUtility.clamp(Compute.d6(2) + unitRatingMod - IUnitRating.DRAGOON_C, 2, 12);
        return table[majorPower ? 0 : 1][roll - 2];
    }

    public void setAllyRating(AtBContract contract, boolean isAttacker, int year) {
        int mod = 0;
        if (contract.getEnemy().isRebelOrPirate()) {
            mod -= 1;
        }

        if (contract.getContractType().isGuerrillaWarfare() || contract.getContractType().isCadreDuty()) {
            mod -= 3;
        } else if (contract.getContractType().isGarrisonDuty() || contract.getContractType().isSecurityDuty()) {
            mod -= 2;
        }

        if (AtBContract.isMinorPower(contract.getEmployerCode())) {
            mod -= 1;
        }

        if (contract.getEnemy().isIndependent()) {
            mod -= 2;
        }

        if (contract.getContractType().isPlanetaryAssault()) {
            mod += 1;
        }

        if (Factions.getInstance().getFaction(contract.getEmployerCode()).isClan() && !isAttacker) {
            //facing front-line units
            mod += 1;
        }
        contract.setAllySkill(getSkillRating(Compute.d6(2) + mod));
        if (year > 2950 && year < 3039 &&
                !Factions.getInstance().getFaction(contract.getEmployerCode()).isClan()) {
            mod -= 1;
        }
        contract.setAllyQuality(getQualityRating(Compute.d6(2) + mod));
    }

    public void setEnemyRating(AtBContract contract, boolean isAttacker, int year) {
        int mod = 0;
        if (contract.getEnemy().isRebelOrPirate()) {
            mod -= 2;
        }
        if (contract.getContractType().isGuerrillaWarfare()) {
            mod += 2;
        }
        if (contract.getContractType().isPlanetaryAssault()) {
            mod += 1;
        }
        if (AtBContract.isMinorPower(contract.getEmployerCode())) {
            mod -= 1;
        }
        if (Factions.getInstance().getFaction(contract.getEmployerCode()).isClan()) {
            mod += isAttacker?2:4;
        }
        contract.setEnemySkill(getSkillRating(Compute.d6(2) + mod));
        if (year > 2950 && year < 3039 &&
                !Factions.getInstance().getFaction(contract.getEnemyCode()).isClan()) {
            mod -= 1;
        }
        contract.setEnemyQuality(getQualityRating(Compute.d6(2) + mod));
    }

    protected SkillLevel getSkillRating(int roll) {
        if (roll <= 5) {
            return SkillLevel.GREEN;
        } else if (roll <= 9) {
            return SkillLevel.REGULAR;
        } else if (roll <= 11) {
            return SkillLevel.VETERAN;
        } else {
            return SkillLevel.ELITE;
        }
    }

    protected int getQualityRating(int roll) {
        if (roll <= 5) {
            return IUnitRating.DRAGOON_F;
        } else if (roll <= 8) {
            return IUnitRating.DRAGOON_D;
        } else if (roll <= 10) {
            return IUnitRating.DRAGOON_C;
        } else if (roll == 11) {
            return IUnitRating.DRAGOON_B;
        } else {
            return IUnitRating.DRAGOON_A;
        }
    }

    protected void setAtBContractClauses(AtBContract contract, int unitRatingMod, Campaign campaign) {
        ClauseMods mods = new ClauseMods();
        clauseMods.put(contract.getId(), mods);

        /* AtB rules seem to indicate one admin in each role (though this
         * is not explicitly stated that I have seen) but MekHQ allows
         * assignment of multiple admins to each role. Therefore we go
         * through all the admins and for each role select the one with
         * the highest admin skill, or higher negotiation if the admin
         * skills are equal.
         */
        Person adminCommand = campaign.findBestInRole(PersonnelRole.ADMINISTRATOR_COMMAND, SkillType.S_ADMIN, SkillType.S_NEG);
        Person adminTransport = campaign.findBestInRole(PersonnelRole.ADMINISTRATOR_TRANSPORT, SkillType.S_ADMIN, SkillType.S_NEG);
        Person adminLogistics = campaign.findBestInRole(PersonnelRole.ADMINISTRATOR_LOGISTICS, SkillType.S_ADMIN, SkillType.S_NEG);
        int adminCommandExp = (adminCommand == null) ? SkillType.EXP_ULTRA_GREEN : adminCommand.getSkill(SkillType.S_ADMIN).getExperienceLevel();
        int adminTransportExp = (adminTransport == null) ? SkillType.EXP_ULTRA_GREEN : adminTransport.getSkill(SkillType.S_ADMIN).getExperienceLevel();
        int adminLogisticsExp = (adminLogistics == null) ? SkillType.EXP_ULTRA_GREEN : adminLogistics.getSkill(SkillType.S_ADMIN).getExperienceLevel();

        /* Treat government units like merc units that have a retainer contract */
        if ((!campaign.getFactionCode().equals("MERC") && !campaign.getFactionCode().equals("PIR"))
                || (null != campaign.getRetainerEmployerCode())) {
            for (int i = 0; i < CLAUSE_NUM; i++) {
                mods.mods[i]++;
            }
        }

        if (campaign.getCampaignOptions().isMercSizeLimited() &&
                campaign.getFactionCode().equals("MERC")) {
            int max = (unitRatingMod + 1) * 12;
            int numMods = (AtBContract.getEffectiveNumUnits(campaign) - max) / 2;
            while (numMods > 0) {
                mods.mods[Compute.randomInt(4)]--;
                numMods--;
            }
        }

        mods.mods[CLAUSE_COMMAND] = adminCommandExp - SkillType.EXP_REGULAR;
        mods.mods[CLAUSE_SALVAGE] = 0;
        mods.mods[CLAUSE_TRANSPORT] = adminTransportExp - SkillType.EXP_REGULAR;
        mods.mods[CLAUSE_SUPPORT] = adminLogisticsExp - SkillType.EXP_REGULAR;
        if (unitRatingMod >= IUnitRating.DRAGOON_A) {
            mods.mods[Compute.randomInt(4)] += 2;
            mods.mods[Compute.randomInt(4)] += 2;
        } else if (unitRatingMod == IUnitRating.DRAGOON_B) {
            mods.mods[Compute.randomInt(4)] += 1;
            mods.mods[Compute.randomInt(4)] += 1;
        } else if (unitRatingMod == IUnitRating.DRAGOON_C) {
            mods.mods[Compute.randomInt(4)] += 1;
        } else if (unitRatingMod <= IUnitRating.DRAGOON_F) {
            mods.mods[Compute.randomInt(4)] -= 1;
        }

        if (Factions.getInstance().getFaction(contract.getEnemyCode()).isClan() &&
                !Factions.getInstance().getFaction(contract.getEmployerCode()).isClan()) {
            for (int i = 0; i < 4; i++) {
                if (i == CLAUSE_SALVAGE) {
                    mods.mods[i] -= 2;
                } else {
                    mods.mods[i] += 1;
                }
            }
        } else {
            if (contract.getEnemySkill().isVeteranOrGreater()) {
                mods.mods[Compute.randomInt(4)] += 1;
            }

            if (contract.getEnemySkill().isEliteOrGreater()) {
                mods.mods[Compute.randomInt(4)] += 1;
            }
        }

        int[][] missionMods = {
            { 1, 0, 1, 0 }, { 0, 1, -1, -3 }, { -3, 0, 2, 1 }, { -2, 1, -1, -1 },
            { -2, 0, 2, 3 }, { -1, 1, 1, 1 }, { -2, 3, -2, -1 }, { 2, 2, -1, -1 },
            { 0, 2, 2, 1 }, { -1, 0, 1, 2 }, { -1, -2, 1, -1 }, { -1, -1, 2, 1 }
        };
        for (int i = 0; i < 4; i++) {
            mods.mods[i] += missionMods[contract.getContractType().ordinal()][i];
        }

        if (Factions.getInstance().getFaction(contract.getEmployerCode()).isISMajorOrSuperPower()) {
            mods.mods[CLAUSE_SALVAGE] += -1;
            mods.mods[CLAUSE_TRANSPORT] += 1;
        }
        if (AtBContract.isMinorPower(contract.getEmployerCode())) {
            mods.mods[CLAUSE_SALVAGE] += -2;
        }
        if (contract.getEmployerCode().equals("MERC")) {
            mods.mods[CLAUSE_COMMAND] += -1;
            mods.mods[CLAUSE_SALVAGE] += 2;
            mods.mods[CLAUSE_SUPPORT] += 1;
            mods.mods[CLAUSE_TRANSPORT] += 1;
        }
        if (contract.getEmployerCode().equals("IND")) {
            mods.mods[CLAUSE_COMMAND] += 0;
            mods.mods[CLAUSE_SALVAGE] += -1;
            mods.mods[CLAUSE_SUPPORT] += -1;
            mods.mods[CLAUSE_TRANSPORT] += 0;
        }

        if (campaign.getFactionCode().equals("MERC")) {
            rollCommandClause(contract, mods.mods[CLAUSE_COMMAND]);
        } else {
            contract.setCommandRights(ContractCommandRights.INTEGRATED);
        }
        rollSalvageClause(contract, mods.mods[CLAUSE_SALVAGE]);
        rollSupportClause(contract, mods.mods[CLAUSE_SUPPORT]);
        rollTransportClause(contract, mods.mods[CLAUSE_TRANSPORT]);
    }

    private void rollCommandClause(final Contract contract, final int modifier) {
        final int roll = Compute.d6(2) + modifier;
        if (roll < 3) {
            contract.setCommandRights(ContractCommandRights.INTEGRATED);
        } else if (roll < 8) {
            contract.setCommandRights(ContractCommandRights.HOUSE);
        } else if (roll < 12) {
            contract.setCommandRights(ContractCommandRights.LIAISON);
        } else {
            contract.setCommandRights(ContractCommandRights.INDEPENDENT);
        }
    }

    private void rollSalvageClause(AtBContract contract, int mod) {
        contract.setSalvageExchange(false);
        int roll = Math.min(Compute.d6(2) + mod, 13);
        if (roll < 2) {
            contract.setSalvagePct(0);
        } else if (roll < 4) {
            contract.setSalvageExchange(true);
            int r;
            do {
                r = Compute.d6(2);
            } while (r < 4);
            contract.setSalvagePct(Math.min((r - 3) * 10, 100));
        } else {
            contract.setSalvagePct(Math.min((roll - 3) * 10, 100));
        }
    }

    private void rollSupportClause(AtBContract contract, int mod) {
        int roll = Compute.d6(2) + mod;
        contract.setStraightSupport(0);
        contract.setBattleLossComp(0);
        if (roll < 3) {
            contract.setStraightSupport(0);
        } else if (roll < 8) {
            contract.setStraightSupport((roll - 2) * 20);
        } else if (roll == 8) {
            contract.setBattleLossComp(10);
        } else {
            contract.setBattleLossComp(Math.min((roll - 8) * 20, 100));
        }
    }

    private void rollTransportClause(AtBContract contract, int mod) {
        int roll = Compute.d6(2) + mod;
        if (roll < 2) {
            contract.setTransportComp(0);
        } else if (roll < 6) {
            contract.setTransportComp((20 + (roll - 2) * 5));
        } else if (roll < 10) {
            contract.setTransportComp((45 + (roll - 6) * 5));
        } else {
            contract.setTransportComp(100);
        }
    }

    public void writeToXML(final PrintWriter pw, int indent) {
        MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "contractMarket");
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "lastId", lastId);
        for (final Contract contract : contracts) {
            contract.writeToXML(pw, indent);
        }

        for (final Integer key : clauseMods.keySet()) {
            if (!contractIds.containsKey(key)) {
                continue;
            }

            MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "clauseMods", "id", key);
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "mods", clauseMods.get(key).mods);
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "rerollsUsed", clauseMods.get(key).rerollsUsed);
            MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "clauseMods");
        }
        MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "contractMarket");
    }

    public static ContractMarket generateInstanceFromXML(Node wn, Campaign c, Version version) {
        ContractMarket retVal = null;

        try {
            // Instantiate the correct child class, and call its parsing function.
            retVal = new ContractMarket();

            // Okay, now load Part-specific fields!
            NodeList nl = wn.getChildNodes();

            // Loop through the nodes and load our contract offers
            for (int x = 0; x < nl.getLength(); x++) {
                Node wn2 = nl.item(x);

                // If it's not an element node, we ignore it.
                if (wn2.getNodeType() != Node.ELEMENT_NODE) {
                    continue;
                }

                if (wn2.getNodeName().equalsIgnoreCase("lastId")) {
                    retVal.lastId = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("mission")) {
                    Mission m = Mission.generateInstanceFromXML(wn2, c, version);

                    if (m instanceof Contract) {
                        retVal.contracts.add((Contract) m);
                        retVal.contractIds.put(m.getId(), (Contract) m);
                    }
                } else if (wn2.getNodeName().equalsIgnoreCase("clauseMods")) {
                    int key = Integer.parseInt(wn2.getAttributes().getNamedItem("id").getTextContent());
                    ClauseMods cm = new ClauseMods();
                    NodeList nl2 = wn2.getChildNodes();
                    for (int i = 0; i < nl2.getLength(); i++) {
                        Node wn3 = nl2.item(i);
                        if (wn3.getNodeName().equalsIgnoreCase("mods")) {
                            String [] s = wn3.getTextContent().split(",");
                            for (int j = 0; j < s.length; j++) {
                                cm.mods[j] = Integer.parseInt(s[j]);
                            }
                        } else if (wn3.getNodeName().equalsIgnoreCase("rerollsUsed")) {
                            String [] s = wn3.getTextContent().split(",");
                            for (int j = 0; j < s.length; j++) {
                                cm.rerollsUsed[j] = Integer.parseInt(s[j]);
                            }
                        }
                    }
                    retVal.clauseMods.put(key, cm);
                }
            }

            // Restore any parent contract references
            for (Contract contract : retVal.contracts) {
                if (contract instanceof AtBContract) {
                    final AtBContract atbContract = (AtBContract) contract;
                    atbContract.restore(c);
                }
            }
        } catch (Exception ex) {
            // Errrr, apparently either the class name was invalid...
            // Or the listed name doesn't exist.
            // Doh!
            LogManager.getLogger().error("", ex);
        }

        return retVal;
    }

    /* Keep track of how many rerolls remain for each contract clause
     * based on the admin's negotiation skill. Also track bonuses, as
     * the random clause bonuses should be persistent.
     */
    public static class ClauseMods {
        public int[] rerollsUsed = {0, 0, 0, 0};
        public int[] mods = {0, 0, 0, 0};
    }
}
