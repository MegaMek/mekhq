/*
 * Copyright (c) 2020 - The MegaMek Team. All Rights Reserved.
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
package mekhq.io;

import megamek.common.util.fileUtils.ImageFileFactory;
import megamek.common.util.fileUtils.ItemFile;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.util.Objects;

/**
 * FIXME : I am a bandaid that allows award pngs to be loaded despite having features not supported by Java.
 * FIXME : I should be removed when that is fixed.
 */
public class AwardFileFactory extends ImageFileFactory {
    //region Constructors
    public AwardFileFactory() {
        super();
    }
    //endregion Constructors

    /**
     * Get the <code>ItemFile</code> for the given <code>File</code>.
     *
     * @param file The input <code>File</code> object that will be read to produce the item.
     *             This value must not be <code>null</code>.
     * @return an <code>ItemFile</code> for the given file.
     */
    @Override
    public ItemFile getItemFile(final File file) {
        // Validate the input.
        Objects.requireNonNull(file, "A null image file was passed");

        // Construct an anonymous class that gets an Image for the file.
        return new ItemFile() {
            @Override
            public Object getItem() throws IOException {
                // Cache the image on first use.
                if (isNullOrEmpty()) {
                    item = ImageIO.read(file.getAbsoluteFile());
                }
                return item;
            }
        };
    }
}
