/*
 * MekHQ - Copyright (C) 2019 - The MegaMek Team
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 */
package mekhq.gui.utilities;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;

import javax.swing.JLabel;

/**
 * A custom label that paints an image to the label that resizes based on the size of the label while
 * maintaining the aspect ratio of the original image.
 * 
 * Code borrowed from:
 * https://stackoverflow.com/questions/10245220/java-image-resize-maintain-aspect-ratio
 * 
 * @author Taharqa
 *
 */
public class ImgLabel extends JLabel {
     /**
     * 
     */
    private static final long serialVersionUID = 2805687715274055318L;
    Image image;
    
    public ImgLabel(Image i) {
        super();
        this.image = i;
    }
    
    /**
     * Get the scaled dimensions for the image that allow it to fit into the label's current size
     * @return <code>Dimension</code> giving the new scaled dimensions 
     */
    private Dimension getScaledDimension() {
        int original_width = image.getWidth(this);
        int original_height = image.getHeight(this);
        int bound_width = getWidth();
        int bound_height = getHeight();
        int new_width = original_width;
        int new_height = original_height;

        // first check if we need to scale width
        if (original_width > bound_width) {
            //scale width to fit
            new_width = bound_width;
            //scale height to maintain aspect ratio
            new_height = (new_width * original_height) / original_width;
        }

        // then check if we need to scale even with the new height
        if (new_height > bound_height) {
            //scale height to fit instead
            new_height = bound_height;
            //scale width to maintain aspect ratio
            new_width = (new_height * original_width) / original_height;
        }
        return new Dimension(new_width, new_height);
    }
        
    @Override
    public void paintComponent(Graphics g)
    {
        super.paintComponent(g);
        Dimension dims = getScaledDimension();
        g.drawImage(image, 0, 0, (int) dims.getWidth(), (int) dims.getHeight(), this);
    }
}