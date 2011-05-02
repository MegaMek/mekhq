package mekhq;

import java.awt.Component;
import java.awt.Image;
import java.awt.image.ImageObserver;
import java.awt.image.MemoryImageSource;
import java.awt.image.PixelGrabber;

	/**
     * A class to handle the image permutations for an entity (borrowed from MegaMek#TileSetManager
     */
    public class EntityImage {
        private Image base;
        private Image wreck;
        private Image icon;
        int tint;
        private Image camo;
        private Component parent;

        private static final int IMG_WIDTH = 84;
        private static final int IMG_HEIGHT = 72;
        private static final int IMG_SIZE = IMG_WIDTH * IMG_HEIGHT;

        public EntityImage(Image base, int tint, Image camo, Component comp) {
            this(base, null, tint, camo, comp);
        }

        public EntityImage(Image base, Image wreck, int tint, Image camo, Component comp) {
            this.base = base;
            this.tint = tint;
            this.camo = camo;
            this.parent = comp;
            this.wreck = wreck;
        }

        public Image loadPreviewImage() {
            base = applyColor(base);
            return base;
        }

        public Image getBase() {
            return base;
        }

        public Image getIcon() {
            return icon;
        }

        private Image applyColor(Image image) {
            Image iMech;
            boolean useCamo = (camo != null);

            iMech = image;

            int[] pMech = new int[IMG_SIZE];
            int[] pCamo = new int[IMG_SIZE];
            PixelGrabber pgMech = new PixelGrabber(iMech, 0, 0, IMG_WIDTH, IMG_HEIGHT, pMech, 0, IMG_WIDTH);

            try {
                pgMech.grabPixels();
            } catch (InterruptedException e) {
                System.err
                        .println("EntityImage.applyColor(): Failed to grab pixels for mech image." + e.getMessage()); //$NON-NLS-1$
                return image;
            }
            if ((pgMech.getStatus() & ImageObserver.ABORT) != 0) {
                System.err
                        .println("EntityImage.applyColor(): Failed to grab pixels for mech image. ImageObserver aborted."); //$NON-NLS-1$
                return image;
            }

            if (useCamo) {
                PixelGrabber pgCamo = new PixelGrabber(camo, 0, 0, IMG_WIDTH,
                        IMG_HEIGHT, pCamo, 0, IMG_WIDTH);
                try {
                    pgCamo.grabPixels();
                } catch (InterruptedException e) {
                    System.err
                            .println("EntityImage.applyColor(): Failed to grab pixels for camo image." + e.getMessage()); //$NON-NLS-1$
                    return image;
                }
                if ((pgCamo.getStatus() & ImageObserver.ABORT) != 0) {
                    System.err
                            .println("EntityImage.applyColor(): Failed to grab pixels for mech image. ImageObserver aborted."); //$NON-NLS-1$
                    return image;
                }
            }

            for (int i = 0; i < IMG_SIZE; i++) {
                int pixel = pMech[i];
                int alpha = (pixel >> 24) & 0xff;

                if (alpha != 0) {
                    int pixel1 = useCamo ? pCamo[i] : tint;
                    float red1 = ((float) ((pixel1 >> 16) & 0xff)) / 255;
                    float green1 = ((float) ((pixel1 >> 8) & 0xff)) / 255;
                    float blue1 = ((float) ((pixel1) & 0xff)) / 255;

                    float black = ((pMech[i]) & 0xff);

                    int red2 = Math.round(red1 * black);
                    int green2 = Math.round(green1 * black);
                    int blue2 = Math.round(blue1 * black);

                    pMech[i] = (alpha << 24) | (red2 << 16) | (green2 << 8)
                            | blue2;
                }
            }

            image = parent.createImage(new MemoryImageSource(IMG_WIDTH,
                    IMG_HEIGHT, pMech, 0, IMG_WIDTH));
            return image;
        }
    }	