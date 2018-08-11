package mekhq.gui.utilities;

import java.awt.*;
/**
 * This class will contain helper methods to deal with images
 *
 * @author Miguel Azevedo aka Bandildo
 * @version 1
 */
public class ImageHelpers {

    /**
     * Resizes an image, keeping its aspect ratio, in order to fit in the given boundaries. It will not resize in case
     * the image already fits the boundaries.
     * @param image Image to resize
     * @param boundary Maximum size the image can have
     * @param resamplingAlgorithm flags to indicate the type of algorithm to use for image resampling.
     * @return Inputed image, resized if necessary.
     */
    public static Image getScaledForBoundaries(Image image, Dimension boundary, int resamplingAlgorithm) {

        double original_width = (double) image.getWidth(null);
        double original_height = (double) image.getHeight(null);
        int bound_width = boundary.width;
        int bound_height = boundary.height;
        double new_width = original_width;
        double new_height = original_height;

        double ratio = original_width/original_height;

        if(original_width > bound_width || original_height > bound_height){
            if(original_width > original_height){
                new_width = bound_width;
                new_height = (int) (new_width / ratio);
            }

            if(original_height > original_width){
                new_height = bound_height;
                new_width = (int) (new_height * ratio);
            }
        }

        return image.getScaledInstance((int)Math.round(new_width), (int)Math.round(new_height), resamplingAlgorithm);
    }
}
