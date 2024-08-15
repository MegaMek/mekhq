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

import java.io.PrintWriter;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import megamek.Version;
import megamek.codeUtilities.MathUtility;
import megamek.common.Compute;
import megamek.common.annotations.Nullable;
import megamek.common.enums.SkillLevel;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.utilities.MHQXMLUtility;
import mekhq.campaign.Campaign;
import mekhq.campaign.JumpPath;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.Contract;
import mekhq.campaign.mission.Mission;
import mekhq.campaign.mission.enums.AtBContractType;
import mekhq.campaign.mission.enums.ContractCommandRights;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.rating.IUnitRating;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Factions;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.campaign.universe.RandomFactionGenerator;
import mekhq.campaign.universe.Systems;

/**
 * Contract offers that are generated monthly under AtB rules.
 *
 * Based on PersonnelMarket
 *
 * @author Neoancient
 */
public class AtbMonthlyContractMarket extends AbstractContractMarket {
    public AtbMonthlyContractMarket() {
        contracts = new ArrayList<>();
        contractIds = new HashMap<>();
        clauseMods = new HashMap<>();
        followupContracts = new HashMap<>();
    }

    @Override
    public AtBContract addAtBContract(Campaign campaign) {
        AtBContract c = generateAtBContract(campaign, campaign.getAtBUnitRatingMod());
        if (c != null) {
            contracts.add(c);
        }
        return c;
    }

    @Override
    public void rerollClause(AtBContract c, int clause, Campaign campaign) {
        if (null != clauseMods.get(c.getId())) {
            switch (clause) {
                case CLAUSE_COMMAND:
                    rollCommandClause(c, clauseMods.get(c.getId()).mods[clause]);
                    break;
                case CLAUSE_SALVAGE:
                    rollSalvageClause(c, clauseMods.get(c.getId()).mods[clause],
                            campaign.getCampaignOptions().getContractMaxSalvagePercentage());
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

    @Override
    public void generateContractOffers(Campaign campaign, boolean newCampaign) {
        if (((campaign.getLocalDate().getDayOfMonth() == 1)) || newCampaign) {
            // need to copy to prevent concurrent modification errors
            new ArrayList<>(contracts).forEach(this::removeContract);

            int unitRatingMod = campaign.getAtBUnitRatingMod();

            for (AtBContract contract : campaign.getActiveAtBContracts()) {
                checkForSubcontracts(campaign, contract, unitRatingMod);
            }

            int numContracts = Compute.d6() - 4 + unitRatingMod;

            Set<Faction> currentFactions = campaign.getCurrentSystem().getFactionSet(campaign.getLocalDate());
            final boolean inMinorFaction = currentFactions.stream()
                    .noneMatch(faction -> faction.isISMajorOrSuperPower() || faction.isClan());
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
                logger.warn(
                        "Unable to find any factions around "
                                + campaign.getCurrentSystem().getName(campaign.getLocalDate())
                                + " on "
                                + campaign.getLocalDate());
            }

            if (inBackwater) {
                numContracts--;
            }

            if (campaign.getFaction().isMercenary() || campaign.getFaction().isPirate()) {
                if (campaign.getAtBConfig().isHiringHall(campaign.getCurrentSystem().getId(),
                        campaign.getLocalDate())) {
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

            /*
             * If located on a faction's capital (interpreted as the starting planet for that faction),
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
                    // no starting planet in current era; continue to next faction
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

    /*
     * If no suitable planet can be found or no jump path to the planet can be
     * calculated after the indicated number of retries, this will return null.
     */
    private @Nullable AtBContract generateAtBContract(Campaign campaign, int unitRatingMod) {
        if (campaign.getFaction().isMercenary()) {
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
            return generateAtBContract(campaign, campaign.getFaction().getShortName(), unitRatingMod);
        }
    }

    private @Nullable AtBContract generateAtBContract(Campaign campaign, @Nullable String employer, int unitRatingMod) {
        return generateAtBContract(campaign, employer, unitRatingMod, MAXIMUM_GENERATION_RETRIES);
    }

    private @Nullable AtBContract generateAtBContract(Campaign campaign, @Nullable String employer, int unitRatingMod, int retries) {
        if (employer == null) {
            logger.warn("Could not generate an AtB Contract because there was no employer!");
            return null;
        } else if (retries <= 0) {
            logger.warn("Could not generate an AtB Contract because we ran out of retries!");
            return null;
        }

        AtBContract contract = new AtBContract("UnnamedContract");
        lastId++;
        contract.setId(lastId);
        contractIds.put(lastId, contract);

        if (Factions.getInstance().getFaction(employer).isMercenary()) {
            contract.setMercSubcontract(true);
            for (int attempts = 0; attempts < MAXIMUM_ATTEMPTS_TO_FIND_NON_MERC_EMPLOYER; ++attempts) {
                employer = RandomFactionGenerator.getInstance().getEmployer();
                if ((employer != null) && !Factions.getInstance().getFaction(employer).isMercenary()) {
                    break;
                }
            }

            if ((employer == null) || Factions.getInstance().getFaction(employer).isMercenary()) {
                logger.warn("Could not generate an AtB Contract because we could not find a non-MERC employer!");
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

        if (contract.getContractType().isGarrisonDuty() && contract.getEnemy().isRebel()) {
            contract.setContractType(AtBContractType.RIOT_DUTY);
        }

        /*
         * Addition to AtB rules: factions which are generally neutral
         * (ComStar, Mercs not under contract) are more likely to have garrison-type
         * contracts and less likely to have battle-type contracts unless at war.
         */
        if (RandomFactionGenerator.getInstance().getFactionHints()
                .isNeutral(Factions.getInstance().getFaction(employer)) &&
                !RandomFactionGenerator.getInstance().getFactionHints().isAtWarWith(
                        Factions.getInstance().getFaction(employer),
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
                || contract.getEnemy().isRebel();
        if (isAttacker) {
            contract.setSystemId(RandomFactionGenerator.getInstance().getMissionTarget(contract.getEmployerCode(),
                    contract.getEnemyCode()));
        } else {
            contract.setSystemId(RandomFactionGenerator.getInstance().getMissionTarget(contract.getEnemyCode(),
                    contract.getEmployerCode()));
        }
        if (contract.getSystem() == null) {
            logger.warn("Could not find contract location for "
                    + contract.getEmployerCode() + " vs. " + contract.getEnemyCode());
            return generateAtBContract(campaign, employer, unitRatingMod, retries - 1);
        }
        JumpPath jp = null;
        try {
            jp = contract.getJumpPath(campaign);
        } catch (NullPointerException ex) {
            // could not calculate jump path; leave jp null
            logger.warn("Could not calculate jump path to contract location: "
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
        if (contract.getContractType().isGarrisonDuty() && contract.getEnemy().isRebel()) {
            contract.setContractType(AtBContractType.RIOT_DUTY);
        }

        contract.setParentContract(parent);
        contract.initContractDetails(campaign);
        lastId++;
        contract.setId(lastId);
        contractIds.put(lastId, contract);

        /*
         * The AtB rules say to roll the enemy, but also that the subcontract
         * takes place in the same planet/sector. Rebels and pirates can
         * appear anywhere, but others should be limited to what's within a
         * jump.
         */

        // TODO : When MekHQ gets the capability of splitting the unit to different
        // locations, this restriction can be lessened or lifted.
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
                || contract.getEnemy().isRebel();
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

    @Override
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
        setAtBContractClauses(followup, campaign.getAtBUnitRatingMod(), campaign);

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
                        AtBContractType.PLANETARY_ASSAULT, AtBContractType.OBJECTIVE_RAID,
                        AtBContractType.OBJECTIVE_RAID,
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
            // facing front-line units
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
            mod += isAttacker ? 2 : 4;
        }
        contract.setEnemySkill(getSkillRating(Compute.d6(2) + mod));
        if (year > 2950 && year < 3039 &&
                !Factions.getInstance().getFaction(contract.getEnemyCode()).isClan()) {
            mod -= 1;
        }
        contract.setEnemyQuality(getQualityRating(Compute.d6(2) + mod));
    }

    @Override
    protected void setAtBContractClauses(AtBContract contract, int unitRatingMod, Campaign campaign) {
        ClauseMods mods = new ClauseMods();
        clauseMods.put(contract.getId(), mods);

        /*
         * AtB rules seem to indicate one admin in each role (though this
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
        if ((!campaign.getFaction().isMercenary() && !campaign.getFaction().isPirate())
                || (null != campaign.getRetainerEmployerCode())) {
            for (int i = 0; i < CLAUSE_NUM; i++) {
                mods.mods[i]++;
            }
        }

        if (campaign.getCampaignOptions().isMercSizeLimited() &&
                campaign.getFaction().isMercenary()) {
            int max = (unitRatingMod + 1) * 12;
            int numMods = (AtBContract.getEffectiveNumUnits(campaign) - max) / 2;
            while (numMods > 0) {
                mods.mods[Compute.randomInt(4)]--;
                numMods--;
            }
        }

        mods.mods[CLAUSE_COMMAND] = adminCommandExp - SkillType.EXP_REGULAR;
        mods.mods[CLAUSE_SALVAGE] = adminLogisticsExp - SkillType.EXP_REGULAR;
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
        if (contract.getEmployerFaction().isMercenary()) {
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

        if (campaign.getFaction().isMercenary()) {
            rollCommandClause(contract, mods.mods[CLAUSE_COMMAND]);
        } else {
            contract.setCommandRights(ContractCommandRights.INTEGRATED);
        }
        rollSalvageClause(contract, mods.mods[CLAUSE_SALVAGE],
                campaign.getCampaignOptions().getContractMaxSalvagePercentage());
        rollSupportClause(contract, mods.mods[CLAUSE_SUPPORT]);
        rollTransportClause(contract, mods.mods[CLAUSE_TRANSPORT]);
    }
}
