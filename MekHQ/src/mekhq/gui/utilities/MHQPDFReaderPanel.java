package mekhq.gui.utilities;

import java.awt.Window;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import javax.swing.border.Border;

import megamek.utilities.PDFReaderPanel;
import mekhq.gui.baseComponents.roundedComponents.RoundedJButton;
import mekhq.gui.baseComponents.roundedComponents.RoundedLineBorder;

/**
 * {@code MHQPDFReaderPanel} is a reusable Swing {@link JPanel} component for viewing and navigating PDF documents.
 *
 * <p>This utility leverages the Apache PDFBox library to render each page of a PDF document as an image. Users can
 * scroll through pages and use zoom controls (zoom in, zoom out, and reset zoom) to adjust the viewing scale. All
 * rendering and file I/O operations that might block the Event Dispatch Thread are performed asynchronously using
 * {@link SwingWorker}, ensuring UI responsiveness even for large documents.</p>
 *
 * <p><b>Features</b></p>
 * <ul>
 *     <li>Open and display multipage PDF files in a dedicated panel</li>
 *     <li>Render each page as a Swing image component for fast display and scrolling</li>
 *     <li>Zoom in and out with predefined DPI steps and boundaries</li>
 *     <li>Reset zoom to the default DPI value</li>
 *     <li>Progress dialog for time-consuming operations</li>
 *     <li>Thread-safe UI updates using SwingWorker</li>
 *     <li>Automatic resource cleanup</li>
 * </ul>
 *
 * <p><b>Typical usage:</b></p>
 * {@code MHQPDFReaderPanel pdfViewer = new MHQPDFReaderPanel(ownerWindow, "/path/to/File.pdf");}
 *
 * @author Illiani
 * @since 0.50.07
 */
public class MHQPDFReaderPanel extends PDFReaderPanel {
    public MHQPDFReaderPanel(Window ownerWindow, String pdfPath) {
        super(ownerWindow, pdfPath);
    }

    @Override
    protected Border getCustomBorder() {
        return RoundedLineBorder.createRoundedLineBorder();
    }

    @Override
    protected JButton getCustomButton(String buttonLabel) {
        return new RoundedJButton(buttonLabel);
    }
}
