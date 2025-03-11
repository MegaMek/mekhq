/*
 * Copyright (c) 2009 - Jay Lawson (jaylawson39 at yahoo.com). All rights reserved.
 * Copyright (C) 2020-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.personnel.ranks;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import megamek.Version;
import megamek.common.annotations.Nullable;
import megamek.common.preference.PreferenceManager;
import megamek.logging.MMLogger;
import mekhq.MHQConstants;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.enums.RankSystemType;
import mekhq.utilities.MHQXMLUtility;

/**
 * Ranks keeps track of all data-file loaded rank systems. It does not include
 * the campaign rank
 * system, if there is a custom one there.
 */
public class Ranks {
    private static final MMLogger logger = MMLogger.create(Ranks.class);

    // region Variable Declarations
    public static final String DEFAULT_SYSTEM_CODE = "SSLDF";

    private static Map<String, RankSystem> rankSystems;
    // endregion Variable Declarations

    // region Constructors
    private Ranks() {
        // This Class should never be constructed
    }
    // endregion Constructors

    // region Getters/Setters
    public static Map<String, RankSystem> getRankSystems() {
        return rankSystems;
    }

    protected static void setRankSystems(final Map<String, RankSystem> rankSystems) {
        Ranks.rankSystems = rankSystems;
    }

    public static @Nullable RankSystem getRankSystemFromCode(final String code) {
        final RankSystem ranks = getRankSystems().get(code);
        return (ranks == null) ? getRankSystems().get(DEFAULT_SYSTEM_CODE) : ranks;
    }
    // endregion Getters/Setters

    // region File I/O
    public static void exportRankSystemsToFile(final @Nullable File file, final RankSystem rankSystem) {
        if (file == null) {
            return;
        }

        final List<RankSystem> rankSystems = new ArrayList<>(getRankSystems().values());
        if (!getRankSystems().containsKey(rankSystem.getCode())) {
            rankSystems.add(rankSystem);
        }
        exportRankSystemsToFile(file, rankSystems);
    }

    public static void exportRankSystemsToFile(@Nullable File file,
            final Collection<RankSystem> rankSystems) {
        if (file == null) {
            return;
        }
        String path = file.getPath();
        if (!path.endsWith(".xml")) {
            path += ".xml";
            file = new File(path);
        }
        int indent = 0;
        try (OutputStream fos = new FileOutputStream(file);
                OutputStream bos = new BufferedOutputStream(fos);
                OutputStreamWriter osw = new OutputStreamWriter(bos, StandardCharsets.UTF_8);
                PrintWriter pw = new PrintWriter(osw)) {
            // Then save it out to that file.
            pw.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "rankSystems", "version", MHQConstants.VERSION);
            for (final RankSystem rankSystem : rankSystems) {
                rankSystem.writeToXML(pw, indent, true);
            }
            MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "rankSystems");
        } catch (Exception ex) {
            logger.error("", ex);
        }
    }

    public static void initializeRankSystems() {
        logger.info("Starting Rank Systems XML load...");
        setRankSystems(new HashMap<>());
        final RankValidator rankValidator = new RankValidator();
        for (final RankSystemType type : RankSystemType.values()) {
            if (type.isCampaign()) {
                continue;
            }
            final List<RankSystem> rankSystems = loadRankSystemsFromFile(new File(type.getFilePath()), type);
            if (type.isUserData()) {
                String userDir = PreferenceManager.getClientPreferences().getUserDir();
                if (!userDir.isBlank() && new File(userDir).isDirectory()) {
                    File userDirRanks = new File(userDir + "/" + MHQConstants.RANKS_FILE_PATH);
                    if (userDirRanks.exists()) {
                        rankSystems.addAll(
                                loadRankSystemsFromFile(new File(userDir + "/" + MHQConstants.RANKS_FILE_PATH), type));
                    }
                }
            }
            for (final RankSystem rankSystem : rankSystems) {
                if (rankValidator.validate(rankSystem, true)) {
                    getRankSystems().put(rankSystem.getCode(), rankSystem);
                }
            }
        }

        if (!getRankSystems().containsKey(DEFAULT_SYSTEM_CODE)) {
            logger.fatal("Ranks MUST load the " + DEFAULT_SYSTEM_CODE
                    + " system. Initialization failure, shutting MekHQ down.");
            System.exit(-1);
        }

        logger.info("Completed Rank System XML Load");
    }

    public static void reinitializeRankSystems(final Campaign campaign) {
        // Initialization is set up so that it will clear what exists
        initializeRankSystems();
        // Then, we need to check and fix any issues that may arise from the file load
        final RankValidator rankValidator = new RankValidator();
        rankValidator.checkAssignedRankSystems(campaign);
        campaign.getPersonnel().forEach(rankValidator::checkPersonRank);
    }

    public static List<RankSystem> loadRankSystemsFromFile(final @Nullable File file,
            final RankSystemType type) {
        if (file == null) {
            return new ArrayList<>();
        }

        final Document xmlDoc;

        try (InputStream is = new FileInputStream(file)) {
            xmlDoc = MHQXMLUtility.newSafeDocumentBuilder().parse(is);
        } catch (Exception e) {
            logger.error("", e);
            return new ArrayList<>();
        }

        final Element element = xmlDoc.getDocumentElement();
        element.normalize();
        final Version version = new Version(element.getAttribute("version"));
        final NodeList nl = element.getChildNodes();
        final List<RankSystem> rankSystems = new ArrayList<>();
        for (int x = 0; x < nl.getLength(); x++) {
            final Node wn = nl.item(x);

            if (!wn.getParentNode().equals(element) || (wn.getNodeType() != Node.ELEMENT_NODE)) {
                continue;
            }

            if (wn.getNodeName().equalsIgnoreCase("rankSystem") && wn.hasChildNodes()) {
                final RankSystem rankSystem = RankSystem.generateInstanceFromXML(wn.getChildNodes(), version, true,
                        type);
                if (rankSystem != null) {
                    rankSystems.add(rankSystem);
                }
            }
        }
        return rankSystems;
    }
    // endregion File I/O
}
