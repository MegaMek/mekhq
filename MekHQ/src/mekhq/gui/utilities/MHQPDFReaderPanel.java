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
package mekhq.gui.utilities;

import java.awt.Window;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import javax.swing.border.Border;

import megamek.common.ui.PDFReaderPanel;
import mekhq.gui.baseComponents.roundedComponents.RoundedJButton;
import mekhq.gui.baseComponents.roundedComponents.RoundedLineBorder;

/**
 * {@code MHQPDFReaderPanel} is a reusable Swing {@link JPanel} component for viewing and navigating PDF documents.
 *
 * <p>This utility leverages the Apache PDFBox library to render each page of a PDF document as an image. Users can
 * scroll through pages and use Zoom controls (zoom in, zoom out, and reset zoom) to adjust the viewing scale. All
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
