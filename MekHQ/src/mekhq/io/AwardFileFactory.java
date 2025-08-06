/*
 * Copyright (C) 2020-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.io;

import java.io.File;
import java.io.IOException;
import java.util.Objects;
import javax.imageio.ImageIO;

import megamek.common.util.fileUtils.ImageFileFactory;
import megamek.common.util.fileUtils.ItemFile;

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
     * @param file The input <code>File</code> object that will be read to produce the item. This value must not be
     *             <code>null</code>.
     *
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
