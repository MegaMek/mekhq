/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.autoResolve.scenarioResolver.component;

import megamek.common.GameLog;
import megamek.common.Report;

import java.util.List;

/**
 * @author Luana Coppio
 */
public class HtmlGameLogger {

    private final GameLog gameLog;
    private boolean printToConsole = false;
    /**
     * Creates GameLog named
     *
     * @param filename the name of the file
     */
    private HtmlGameLogger(String filename) {
        gameLog = new GameLog(filename);
        initializeLog();
    }

    private void initializeLog() {
        add("<html><head>\n" +
            "    <meta charset=\"UTF-8\">\n" +
            "</head><body>");
    }

    public HtmlGameLogger printToConsole() {
        this.printToConsole = true;
        return this;
    }

    public static HtmlGameLogger create(String filename) {
        return new HtmlGameLogger(filename);
    }

    public HtmlGameLogger add(List<Report> reports) {
        for (var report : reports) {
            add(report.text());
        }
        return this;
    }

    public HtmlGameLogger add(Report report) {
        add(report.text());
        return this;
    }

    public HtmlGameLogger add(String message) {
        gameLog.append(message);
        if (printToConsole) {
            System.out.println(message);
        }
        return this;
    }

    public void close() {
        add("</body></html>");
    }

}
