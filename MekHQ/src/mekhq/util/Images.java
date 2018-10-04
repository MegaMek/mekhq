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
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.IntBinaryOperator;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.log4j.Logger;

import megamek.client.ui.swing.MechTileset.MechEntry;
import megamek.client.ui.swing.util.PlayerColors;
import megamek.common.util.DirectoryItems;
import mekhq.IconPackage;
import mekhq.campaign.force.Force;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;

/**
 * Utility class dealing with images
 */
public class Images {

    private static final Logger log = Logger.getLogger(Images.class);

    private static final Color TRANSPARENT = new Color(0, true);
    private static final ImageId DEFAULT_PERSON_PORTRAIT = ImageId.of("", "default.gif"); //$NON-NLS-1$ //$NON-NLS-2$

    private Images() {
        // no instances
    }

    /**
     * Returns the portrait for the given person.
     */
    public static BufferedImage portrait(IconPackage icons, Person p, int w, int h) {
        return portrait(icons, p.getPortraitId().orElse(DEFAULT_PERSON_PORTRAIT), w, h);
    }

    /**
     * Returns the portrait for the given image id, or a default one if no id is present
     */
    public static BufferedImage portrait(IconPackage icons, Optional<ImageId> iid, int w, int h) {
        return portrait(icons, iid.orElse(DEFAULT_PERSON_PORTRAIT), w, h);
    }

    /**
     * Returns the portrait for the given image id.
     */
    @SuppressWarnings("nls")
    public static BufferedImage portrait(IconPackage icons, ImageId iid, int w, int h) {
        try {
            return scaleAndCenter(bufferedImage((Image) icons.getPortraits().getItem(iid.getCategory(), iid.getFileName())), w, h);
        } catch (Exception e) {
            String msg = String.format("Could not load portrait %s/%s - returning red image", iid.getCategory(), iid.getFileName());
            log.error(msg, e);
            return redImage(w, h);
        }
    }

    /**
     * Returns the icon for the given force.
     */
    public static Image force(IconPackage icons, Force f, int w, int h) {
        return force(icons, f.getForceIconId(), w, h);
    }

    /**
     * Renders the given {@code forceIconId} as an image.
     */
    @SuppressWarnings("nls")
    public static Image force(IconPackage icons, Optional<ForceIconId> forceIconId, int w, int h) {

        ForceIconId fid = forceIconId.orElse(ForceIconId.defatultFrame());

        Comparator<ImageId> drawOrder = Comparator.comparing( (ImageId id) -> {
            // Categories not in FORCE_DRAW_ORDER will be rendered first
            // (negative index), then FORCE_DRAW_ORDER will take place
            return ArrayUtils.indexOf(IconPackage.FORCE_DRAW_ORDER, id.getCategory());
        });

        DirectoryItems forceIcons = icons.getForceIcons();

        List<BufferedImage> layers = fid.layers().sorted(drawOrder).map(iid -> {
            try {
                return (BufferedImage) forceIcons.getItem(iid.getCategory(), iid.getFileName());
            } catch (Exception e) {
                String msg = String.format("Could not load force layer %s/%s - ignoring", iid.getCategory(), iid.getFileName());
                log.error(msg, e);
                return null;
            }
        }).filter(Objects::nonNull).collect(Collectors.toList());

        if (layers.size() == 1) {
            // The process used for layered icons, below, must assume images
            // have all the same "resolution" and work at fixed size because
            // images all have different sizes.
            // This doesn't quite work for "old", single-layer force icons,
            // which are often quite "low-res" (ie. small and need upscaling).
            return scaleAndCenter(layers.iterator().next(), w, h);
        } else {
            // Force layers come in all different sizes, so we must work at
            // their max size and scale later.

            int maxw = 289; // last checked in Sep 2018
            int maxh = 220; // last checked in Sep 2018

            BufferedImage result = new BufferedImage(maxw, maxh, BufferedImage.TYPE_INT_ARGB);
            Graphics2D canvas = result.createGraphics();
            try {
                canvas.addRenderingHints(Collections.singletonMap(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY));
                canvas.setBackground(new Color(0x00_00_00_00, true)); // ie: transparent
                canvas.clearRect(0, 0, maxw, maxh); // for good measure
                layers.forEach(layer -> {
                    int lw = layer.getWidth();
                    int lh = layer.getHeight();
                    // canvas.drawImage(layer, (maxw - lw) / 2, (maxh - lh) / 2, lw, lh, null);
                    canvas.drawImage(layer, (maxw - lw) +1, (maxh - lh) +1, lw, lh, null); // this aligns... the images' bottom and right sides?
                                                                                       // it doesn't seem right, but produced images look good
                });
            } finally {
                canvas.dispose();
            }
            return scaleAndCenter(result, w, h);
        }
    }

    /**
     * @return the icon for the given unit
     */
    @SuppressWarnings("nls")
    public static BufferedImage unit(IconPackage icons, Unit u, int w, int h) {

        BufferedImage unit; {
            MechEntry entryFor = icons.getMechTiles().entryFor(u.getEntity(), -1); // no idea what -1 is for
            entryFor.loadImage(null); // requires a java.awt.Component, but then ignores it
            unit = bufferedImage(entryFor.getImage());
        }

        Optional<BufferedImage> camo;
        try {
            camo = Optional.ofNullable(bufferedImage((Image) icons.getCamos().getItem(u.getCamoCategory(), u.getCamoFileName())));
        } catch (Exception e) {
            String msg = String.format("Could not load camo %s/%s - ignoring", u.getCamoCategory(), u.getCamoFileName());
            log.error(msg, e);
            camo = Optional.empty();
        }

        Color tint = new Color(PlayerColors.getColorRGB(u.campaign.getColorIndex()), false);

        return applyCamo(unit, camo, tint, w, h, Images.PixelStrategy.PRESERVE_NON_GRAY);
    }

    static BufferedImage applyCamo(BufferedImage unitImage, Optional<BufferedImage> camoImage, Color tint, int w, int h, IntBinaryOperator pixelMergingStrategy) {

        BufferedImage mech = scaleAndCenter(unitImage, w, h);

        IntBinaryOperator getCamoRGB = camoImage.isPresent()
                                    ? scaleAndCenter(camoImage.get(), w, h)::getRGB
                                    : (x,y) -> tint.getRGB();

        BufferedImage cnv = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                int mechARGB = mech.getRGB(x, y);
                int camoARGB = getCamoRGB.applyAsInt(x,y);
                cnv.setRGB(x, y, pixelMergingStrategy.applyAsInt(mechARGB, camoARGB));
            }
        }
        return cnv;
    }

    private static BufferedImage scaleAndCenter(BufferedImage img, int w, int h) {
        if ((img.getWidth() == w) && (img.getHeight() == h)) {
            return img;
        } else {
            float srcRatio = img.getWidth() / (float) img.getHeight();
            float trgRatio = w / (float) h;

            float f = srcRatio > trgRatio // too wide?
                    ? w / (float) img.getWidth()
                    : h / (float) img.getHeight();

            int imgw = Math.round(img.getWidth()  * f);
            int imgh = Math.round(img.getHeight() * f);

            BufferedImage cnv = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = cnv.createGraphics();
            try {
                g2d.addRenderingHints(Collections.singletonMap(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY));
                g2d.setBackground(TRANSPARENT);
                g2d.clearRect(0, 0, w, h);
                g2d.drawImage(img, (w - imgw) / 2, (h - imgh) / 2, imgw, imgh, null);
            } finally {
                g2d.dispose();
            }
            return cnv;
        }
    }

    static enum PixelStrategy implements IntBinaryOperator {
        /**
         * The traditional MM approach: darken the camo where the unit image is dark.
         * Use the blue channel as a measure of brightness.
         */
        BLUE {
            @Override public int applyAsInt(int mechARGB, int camoARGB) {
                int mechA  = mechARGB >> 24;
                if (mechA == 0) {
                    return 0x00000000;
                } else {
                    int camoR = (camoARGB >> 16) & 0xFF;
                    int camoG = (camoARGB >>  8) & 0xFF;
                    int camoB = (camoARGB      ) & 0xFF;

                    float lum = (mechARGB & 0xff) / (float) 255; // blue channel

                    int r = Math.round(camoR * lum);
                    int g = Math.round(camoG * lum);
                    int b = Math.round(camoB * lum);

                    return (mechA << 24) | (r << 16) | (g << 8) | b;
                }
            }
        },
        /**
         * Similar to the traditional approach, but uses an approximation of
         * each pixel's luma instead of the blue channel to measure brightness.
         *
         * @see "https://stackoverflow.com/questions/596216/formula-to-determine-brightness-of-rgb-color/596241#596241"
         */
        LUMA {
            @Override public int applyAsInt(int mechARGB, int camoARGB) {
                int mechA  = mechARGB >> 24;
                if (mechA == 0) {
                    return 0x00000000;
                } else {
                    int camoR = (camoARGB >> 16) & 0xFF;
                    int camoG = (camoARGB >>  8) & 0xFF;
                    int camoB = (camoARGB      ) & 0xFF;

                    int mechR = (mechARGB >> 16) & 0xFF;
                    int mechG = (mechARGB >>  8) & 0xFF;
                    int mechB = (mechARGB      ) & 0xFF;

                    float lum = (((3 * mechR) + (4 * mechG) + mechB) >> 3) / (float) 255;

                    int r = Math.round(camoR * lum);
                    int g = Math.round(camoG * lum);
                    int b = Math.round(camoB * lum);

                    return (mechA << 24) | (r << 16) | (g << 8) | b;
                }
            }
        },
        /**
         * Same as {@linkplain #LUMA}, but leaves non-gray pixels
         * (ie. those whose saturation != 0) alone.
         */
        PRESERVE_NON_GRAY {
            @Override public int applyAsInt(int mechARGB, int camoARGB) {
                int mechA  = mechARGB >> 24;
                if (mechA == 0) {
                    return 0x00000000;
                } else {
                    int mechR = (mechARGB >> 16) & 0xFF;
                    int mechG = (mechARGB >>  8) & 0xFF;
                    int mechB = (mechARGB      ) & 0xFF;

                    if ((mechR != mechG) || (mechG != mechB)) {
                        return mechARGB;
                    }

                    int camoR = (camoARGB >> 16) & 0xFF;
                    int camoG = (camoARGB >>  8) & 0xFF;
                    int camoB = (camoARGB      ) & 0xFF;

                    float lum = (((3 * mechR) + (4 * mechG) + mechB) >> 3) / (float) 255;

                    int r = Math.round(camoR * lum);
                    int g = Math.round(camoG * lum);
                    int b = Math.round(camoB * lum);

                    return (mechA << 24) | (r << 16) | (g << 8) | b;
                }
            }
        },
        /**
         * Similar to {@linkplain #PRESERVE_NON_GRAY}, but computes
         * the actual HSL saturation for each pixel and uses that to decide
         * which pixels are to be left alone and also uses the HSL lightness as
         * a measure of brightness.
         */
        PRESERVE_SATURATED {
            static final float THRESHOLD = 3 / (float) 255;

            @Override public int applyAsInt(int mechARGB, int camoARGB) {
                int mechA  = mechARGB >> 24;
                if (mechA == 0) {
                    return 0x00000000;
                } else {
                    int mechR = (mechARGB >> 16) & 0xFF;
                    int mechG = (mechARGB >>  8) & 0xFF;
                    int mechB = (mechARGB      ) & 0xFF;

                    float max = Math.max(Math.max(mechR, mechG), mechB) / (float) 255;
                    float min = Math.min(Math.min(mechR, mechG), mechB) / (float) 255;

                    float sat;
                    float lig = (min + max) / 2;
                    {
                        float diff = max - min;
                        if (diff < THRESHOLD ) {
                            sat = 0;
                        } else {
                            sat = diff;
                            if (lig > .5) {
                                sat /= 2 - (2 * lig);
                            } else {
                                sat /= 2 * lig;
                            }
                        }
                    }

                    if (sat > .1) {
                        return mechARGB;
                    }

                    int camoR = (camoARGB >> 16) & 0xFF;
                    int camoG = (camoARGB >>  8) & 0xFF;
                    int camoB = (camoARGB      ) & 0xFF;

                    int r = Math.round(camoR * lig);
                    int g = Math.round(camoG * lig);
                    int b = Math.round(camoB * lig);

                    return (mechA << 24) | (r << 16) | (g << 8) | b;
                }
            }
        },
    }

    /**
     * Converts the given {@linkplain Image} to a {@linkplain BufferedImage}
     *
     * If called with a {@code null} argument, this method returns {@code null}.
     */
    public static BufferedImage bufferedImage(Image img) {
        if ((img instanceof BufferedImage) || (img == null)) {
            return (BufferedImage) img;
        } else {
            while (img.getHeight(null) == -1) {
                Thread.yield();
            }
            while (img.getWidth(null)  == -1) {
                Thread.yield();
            }
            BufferedImage buf = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = buf.createGraphics();
            g2d.drawImage(img, 0, 0, null);
            g2d.dispose();
            return buf;
        }
    }

    private static BufferedImage redImage(int w, int h) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = img.createGraphics();
        try {
            g2d.setBackground(Color.RED);
            g2d.clearRect(0, 0, w, h);
        } finally {
            g2d.dispose();
        }
        return img;
    }

}
