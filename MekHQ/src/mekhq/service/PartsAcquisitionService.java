/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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
 */
package mekhq.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import megamek.common.TargetRoll;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Money;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.PartInventory;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.work.IAcquisitionWork;

public class PartsAcquisitionService {
    private static Map<String, List<IAcquisitionWork>> acquisitionMap = null;
    private static Map<String, PartCountInfo> partCountInfoMap = new HashMap<>();

    private static int inTransitCount = 0;
    private static int onOrderCount = 0;
    private static int omniPodCount = 0;
    private static int missingCount = 0;
    private static int requiredCount = 0;
    private static int unavailableCount = 0;
    private static Money missingTotalPrice = Money.zero();

    private PartsAcquisitionService() {
    }

    public static Map<String, List<IAcquisitionWork>> getAcquisitionMap() {
        return acquisitionMap;
    }

    public static void setAcquisitionMap(Map<String, List<IAcquisitionWork>> acquisitionMap) {
        PartsAcquisitionService.acquisitionMap = acquisitionMap;
    }

    public static Map<String, PartCountInfo> getPartCountInfoMap() {
        return partCountInfoMap;
    }

    public static void setPartCountInfoMap(Map<String, PartCountInfo> partCountInfoMap) {
        PartsAcquisitionService.partCountInfoMap = partCountInfoMap;
    }

    public static int getInTransitCount() {
        return inTransitCount;
    }

    public static void setInTransitCount(int inTransitCount) {
        PartsAcquisitionService.inTransitCount = inTransitCount;
    }

    public static int getOnOrderCount() {
        return onOrderCount;
    }

    public static void setOnOrderCount(int onOrderCount) {
        PartsAcquisitionService.onOrderCount = onOrderCount;
    }

    public static int getOmniPodCount() {
        return omniPodCount;
    }

    public static void setOmniPodCount(int omniPodCount) {
        PartsAcquisitionService.omniPodCount = omniPodCount;
    }

    public static int getMissingCount() {
        return missingCount;
    }

    public static void setMissingCount(int missingCount) {
        PartsAcquisitionService.missingCount = missingCount;
    }

    public static int getRequiredCount() {
        return requiredCount;
    }

    public static void setRequiredCount(int requiredCount) {
        PartsAcquisitionService.requiredCount = requiredCount;
    }

    public static int getUnavailableCount() {
        return unavailableCount;
    }

    public static void setUnavailableCount(int unavailableCount) {
        PartsAcquisitionService.unavailableCount = unavailableCount;
    }

    public static Money getMissingTotalPrice() {
        return missingTotalPrice;
    }

    public static void setMissingTotalPrice(Money missingTotalPrice) {
        PartsAcquisitionService.missingTotalPrice = missingTotalPrice;
    }

    public static void buildPartsList(Campaign campaign) {
        acquisitionMap = new HashMap<>();

        for (Unit unit : campaign.getServiceableUnits()) {
            for (IAcquisitionWork aw : unit.getPartsNeeded()) {
                if (null == aw.getAcquisitionPart()) {
                    continue;
                }

                List<IAcquisitionWork> awList = acquisitionMap.computeIfAbsent(aw.getAcquisitionDisplayName(),
                        k -> new ArrayList<>());

                awList.add(aw);
            }
        }

        generateSummaryCounts(campaign);
    }

    public static void generateSummaryCounts(Campaign campaign) {
        partCountInfoMap = new HashMap<>();

        Person admin = campaign.getLogisticsPerson();

        for (List<IAcquisitionWork> awList : acquisitionMap.values()) {
            IAcquisitionWork awFirst = awList.get(0);
            Part part = awFirst.getAcquisitionPart();
            TargetRoll target = campaign.getTargetForAcquisition(awFirst, admin, true);
            PartCountInfo pci = new PartCountInfo();

            PartInventory inventories = campaign.getPartInventory(part);
            pci.setCountModifier(inventories.getCountModifier());

            int inTransit = inventories.getTransit();
            int onOrder = inventories.getOrdered();
            int omniPod = 0;

            if (!part.isOmniPodded()) {
                part.setOmniPodded(true);
                PartInventory omniPodInventory = campaign.getPartInventory(part);

                if (omniPodInventory.getSupply() > 0) {
                    omniPod = omniPodInventory.getSupply();
                }

                part.setOmniPodded(false);
            }

            int missing = Math.max(0, awList.size() - inTransit - onOrder);

            pci.setKey(awFirst.getAcquisitionDisplayName());
            pci.setRequiredCount(awList.size());
            pci.setStickerPrice(part.getStickerPrice());
            pci.setMissingCount(missing);

            if (target.getValue() == TargetRoll.IMPOSSIBLE) {
                pci.setCanBeAcquired(false);
                pci.setFailedMessage(target.getPlainDesc());
            } else {
                pci.setInTransitCount(inTransit);
                pci.setOnOrderCount(onOrder);
                pci.setOmniPodCount(omniPod);
            }

            partCountInfoMap.put(awList.get(0).getAcquisitionDisplayName(), pci);
        }

        inTransitCount = 0;
        onOrderCount = 0;
        omniPodCount = 0;
        missingCount = 0;
        requiredCount = 0;
        unavailableCount = 0;
        missingTotalPrice = Money.zero();

        // campaign.addReport("***START: generateSummaryCounts");

        for (PartCountInfo pci : partCountInfoMap.values()) {
            inTransitCount += pci.getInTransitCount();
            onOrderCount += pci.getOnOrderCount();
            missingCount += pci.getMissingCount();
            requiredCount += pci.getRequiredCount();
            omniPodCount += pci.getOmniPodCount();

            if (pci.getMissingCount() > 0) {
                if (!pci.isCanBeAcquired()) {
                    unavailableCount += pci.getMissingCount();
                } else {
                    missingTotalPrice = missingTotalPrice.plus(
                            pci.getStickerPrice().multipliedBy(pci.getMissingCount()));
                }
            }

            // campaign.addReport(pci.toString());
        }

        // campaign.addReport("***END: generateSummaryCounts");
    }

    public static class PartCountInfo {
        private String key;
        private int requiredCount;
        private int missingCount;
        private int inTransitCount;
        private int onOrderCount;
        private String countModifier = "";
        private int omniPodCount;
        private Money stickerPrice;
        private String failedMessage;
        private boolean canBeAcquired = true;

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public int getRequiredCount() {
            return requiredCount;
        }

        public void setRequiredCount(int requiredCount) {
            this.requiredCount = requiredCount;
        }

        public int getMissingCount() {
            return missingCount;
        }

        public void setMissingCount(int missingCount) {
            this.missingCount = missingCount;
        }

        public int getInTransitCount() {
            return inTransitCount;
        }

        public void setInTransitCount(int inTransitCount) {
            this.inTransitCount = inTransitCount;
        }

        public int getOnOrderCount() {
            return onOrderCount;
        }

        public void setOnOrderCount(int onOrderCount) {
            this.onOrderCount = onOrderCount;
        }

        public int getOmniPodCount() {
            return omniPodCount;
        }

        public void setOmniPodCount(int omniPodCount) {
            this.omniPodCount = omniPodCount;
        }

        public String getCountModifier() {
            return countModifier;
        }

        public void setCountModifier(String countModifier) {
            this.countModifier = countModifier;
        }

        public Money getStickerPrice() {
            return stickerPrice;
        }

        public void setStickerPrice(Money stickerPrice) {
            this.stickerPrice = stickerPrice;
        }

        public String getFailedMessage() {
            return failedMessage;
        }

        public void setFailedMessage(String failedMessage) {
            this.failedMessage = failedMessage;
        }

        public boolean isCanBeAcquired() {
            return canBeAcquired;
        }

        public void setCanBeAcquired(boolean canBeAcquired) {
            this.canBeAcquired = canBeAcquired;
        }

        @Override
        public String toString() {
            return key + "{"
                    + "requiredCount=" + requiredCount
                    + ",missingCount=" + missingCount
                    + ",inTransitCount=" + inTransitCount
                    + ",onOrderCount=" + onOrderCount
                    + ",omniPodCount=" + omniPodCount
                    + ",countModifier='" + countModifier + "'"
                    + ",stickerPrice=" + stickerPrice
                    + ",failedMessage='" + failedMessage + "'"
                    + ",canBeAcquired=" + canBeAcquired
                    + "}";
        }
    }
}
