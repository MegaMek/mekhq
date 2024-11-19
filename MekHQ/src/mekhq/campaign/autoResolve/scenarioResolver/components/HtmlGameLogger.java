package mekhq.campaign.autoResolve.scenarioResolver.components;

import megamek.common.GameLog;
import megamek.common.Report;
import megamek.common.strategicBattleSystems.SBFReportEntry;

import java.util.List;

public class HtmlGameLogger {

    private GameLog gameLog;
    private boolean printToConsole = false;
    /**
     * Creates GameLog named
     *
     * @param filename
     */
    private HtmlGameLogger(String filename) {
        gameLog = new GameLog(filename);
        initializeLog();
    }

    private void initializeLog() {
        add("<HTML><BODY>");
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
        add("</BODY></HTML>");
    }

}
