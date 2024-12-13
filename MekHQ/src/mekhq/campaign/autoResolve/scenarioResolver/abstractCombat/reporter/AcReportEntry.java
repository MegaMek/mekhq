/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.autoResolve.scenarioResolver.abstractCombat.reporter;

import megamek.common.Report;
import megamek.common.ReportEntry;
import megamek.common.Roll;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class AcReportEntry implements ReportEntry {

    record DataEntry(String data, boolean isObscured) implements Serializable {
    }

    private final int messageId;
    private final List<DataEntry> data = new ArrayList<>();
    private boolean endLine = true;
    private boolean endSpace = false;
    private int indentation = 0;

    public AcReportEntry(int messageId) {
        this.messageId = messageId;
    }

    /**
     * Add the given int to the list of data that will be substituted for the
     * &lt;data&gt; tags in the report. The order in which items are added must
     * match the order of the tags in the report text.
     *
     * @param data the int to be substituted
     * @return This Report to allow chaining
     */
    public AcReportEntry add(int data) {
        return add(String.valueOf(data), true);
    }

    /**
     * Add the given int to the list of data that will be substituted for the
     * &lt;data&gt; tags in the report, and mark it as double-blind sensitive
     * information if <code>obscure</code> is true. The order in which items
     * are added must match the order of the tags in the report text.
     *
     * @param data    the int to be substituted
     * @param obscure boolean indicating whether the data is double-blind
     *                sensitive
     * @return This Report to allow chaining
     */
    public AcReportEntry add(int data, boolean obscure) {
        return add(String.valueOf(data), obscure);
    }

    /**
     * Add the given String to the list of data that will be substituted for the
     * &lt;data&gt; tags in the report. The order in which items are added must
     * match the order of the tags in the report text.
     *
     * @param data the String to be substituted
     * @return This Report to allow chaining
     */
    public AcReportEntry add(String data) {
        return add(data, true);
    }

    /**
     * Add the given String to the list of data that will be substituted for the
     * &lt;data&gt; tags in the report, and mark it as double-blind sensitive
     * information if <code>obscure</code> is true. The order in which items
     * are added must match the order of the tags in the report text.
     *
     * @param data    the String to be substituted
     * @param obscure boolean indicating whether the data is double-blind
     *                sensitive
     * @return This Report to allow chaining
     */
    public AcReportEntry add(String data, boolean obscure) {
        this.data.add(new DataEntry(data, obscure));
        return this;
    }

    @Override
    public final String text() {
        return " ".repeat(indentation) + reportText() + lineEnd();
    }

    @Override
    public ReportEntry addRoll(Roll roll) {
        return this;
    }

    /**
     * Indent the report. Equivalent to calling {@link #indent(int)} with a
     * parameter of 1.
     *
     * @return This Report to allow chaining
     */
    public AcReportEntry indent() {
        return indent(1);
    }

    /**
     * Indent the report n times.
     *
     * @param n the number of times to indent the report
     * @return This Report to allow chaining
     */
    public AcReportEntry indent(int n) {
        indentation += (n * Report.DEFAULT_INDENTATION);
        return this;
    }

    public AcReportEntry noNL() {
        endLine = false;
        return this;
    }

    public AcReportEntry endSpace() {
        endSpace = true;
        return this;
    }

    public AcReportEntry addNL() {
        endLine = true;
        return this;
    }

    private String lineEnd() {
        return (endSpace ? " " : "") + (endLine ? "<BR>" : "");
    }

    protected String reportText() {
        return AcReportMessages.getString(String.valueOf(messageId), data.stream().map(d -> (Object) d.data).toList());
    }
}
