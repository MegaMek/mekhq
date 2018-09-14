/*
 * MekHQ - Copyright (C) 2018 - The MekHQ Team
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
package mekhq.util;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Optional;
import java.util.function.IntBinaryOperator;
import java.util.stream.Stream;

import javax.imageio.ImageIO;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import org.junit.Test;

import mekhq.IconPackage;

@SuppressWarnings({"nls","javadoc"})
public class ImagesTest {

    public static void main(String[] args) throws Exception {
 
        BufferedImage unit  = load("data/images/units/mechs/BattleMaster.png");
        BufferedImage camo1 = load("data/images/camo/Davion/Periphery Guard/1st Periphery Guard.jpg");
        BufferedImage camo2 = load("data/images/camo/Mercs/Camachos Caballeros.jpg");
        BufferedImage camo3 = load("data/images/camo/Standard Camouflage/Hex/Hex green.jpg");

        JFrame frame = new JFrame("");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().setLayout(new BoxLayout(frame.getContentPane(), SwingConstants.VERTICAL));

        JPanel rootPanel = new JPanel();
        rootPanel.setLayout(new BoxLayout(rootPanel, SwingConstants.VERTICAL));
        frame.getContentPane().add(rootPanel);

        Stream.<IntBinaryOperator>of(
            Images.PixelStrategy.BLUE,
            Images.PixelStrategy.LUMA,
            Images.PixelStrategy.PRESERVE_NON_GRAY,
            Images.PixelStrategy.PRESERVE_SATURATED
        ).forEach(op -> {
            JPanel panel = new JPanel();
            panel.setLayout(new BoxLayout(panel, SwingConstants.HORIZONTAL));
            panel.add(new JLabel(new ImageIcon(Images.applyCamo(unit, Optional.empty(),   Color.WHITE,  150, 150, op))));
            panel.add(new JLabel(new ImageIcon(Images.applyCamo(unit, Optional.empty(),   Color.ORANGE, 150, 150, op))));
            panel.add(new JLabel(new ImageIcon(Images.applyCamo(unit, Optional.of(camo1), Color.ORANGE, 150, 150, op))));
            panel.add(new JLabel(new ImageIcon(Images.applyCamo(unit, Optional.of(camo2), Color.ORANGE, 150, 150, op))));
            panel.add(new JLabel(new ImageIcon(Images.applyCamo(unit, Optional.of(camo3), Color.ORANGE, 150, 150, op))));
            rootPanel.add(panel);
        });

        {
            ForceIconId fii1 = ForceIconId.of(ImageId.of("Pieces/Frames/","Frame.png")).withAddedLayers(
                ImageId.of("Pieces/Type/","BattleMech Medium.png"),
                ImageId.of("Pieces/Formations/","04 Lance.png"),
                ImageId.of("Pieces/Alphanumerics/","A.png")
            );
            ForceIconId fii2 = ForceIconId.of(ImageId.of("Pieces/Frames/","Frame.png")).withAddedLayers(
                ImageId.of("Pieces/Type/","BattleMech Light.png"),
                ImageId.of("Pieces/Formations/","05 Lance Augmented.png"),
                ImageId.of("Pieces/Alphanumerics/","B.png"),
                ImageId.of("Pieces/Adjustments/","Recon.png")
            );
            IconPackage icons = new IconPackage();
            icons.loadDirectories();
            JPanel panel = new JPanel();
            panel.setLayout(new BoxLayout(panel, SwingConstants.HORIZONTAL));
            panel.add(new JLabel(new ImageIcon(Images.portrait(icons, ImageId.of("Pursing Peregrines/", "Hayes.jpg"),  50, 250))));
            panel.add(new JLabel(new ImageIcon(Images.force(icons, Optional.of(fii1), 200, 250))));
            panel.add(new JLabel(new ImageIcon(Images.portrait(icons, ImageId.of("Pursing Peregrines/", "Hayes.jpg"), 150, 250))));
            panel.add(new JLabel(new ImageIcon(Images.force(icons, Optional.of(fii2), 50, 250))));
            panel.add(new JLabel(new ImageIcon(Images.applyCamo(unit, Optional.empty(), new Color(0x0080c0), 250, 250, Images.PixelStrategy.PRESERVE_NON_GRAY))));
            rootPanel.add(panel);
        }

        frame.pack();
        frame.setVisible(true);
    }

    @Test
    public void testPortrait() {
        IconPackage icons = new IconPackage();
        icons.loadDirectories();
        Images.portrait(icons, ImageId.of("Pursing Peregrines/", "Hayes.jpg"), 10, 10);
    }
    
    @Test
    public void testForceIcon() {
        // just a smoke test
        ForceIconId fii = ForceIconId.of(ImageId.of("Pieces/Frames/","Frame.png")).withAddedLayers(
            ImageId.of("Pieces/Frames/","Frame.png"),
            ImageId.of("Pieces/Type/","BattleMech.png"),
            ImageId.of("Pieces/Formations/","04 Lance.png"),
            ImageId.of("Pieces/Alphanumerics/","A.png")
        );
        IconPackage icons = new IconPackage();
        icons.loadDirectories();
        Images.force(icons, Optional.of(fii), 100, 100);
    }

    @Test
    public void testApplyCamo() throws IOException {
        // just a smoke test
        BufferedImage unit = load("data/images/units/mechs/BattleMaster.png");
        BufferedImage camo = load("data/images/camo/Davion/Periphery Guard/1st Periphery Guard.jpg");
        Stream.<IntBinaryOperator>of(
                Images.PixelStrategy.BLUE,
                Images.PixelStrategy.LUMA,
                Images.PixelStrategy.PRESERVE_NON_GRAY,
                Images.PixelStrategy.PRESERVE_SATURATED
        ).forEach(op -> Images.applyCamo(unit, Optional.of(camo), Color.WHITE,  150, 150, op) );
    }

    private static BufferedImage load(String file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            return ImageIO.read(fis);
        }
    }

}
