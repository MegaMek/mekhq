# MekHQ

## Table of Contents

1. [About](#about)
2. [Status](#status)
3. [Compiling](#compiling)
4. [Support](#support)
5. [License](#licensing)

## About

MekHQ is a Java helper program for the [MegaMek](http://megamek.org) game that allows users to run a campaign. For more
details, see our [website](http://megamek.org/) and join our [Discord](https://discord.gg/XM54YH9396).

## Status

| Type           | MM Status                                                                                                                                                              | MML Status                                                                                                                                                                       | MHQ Status                                                                                                                                                        |
|----------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Latest Release | [![Release](https://img.shields.io/github/release/MegaMek/megamek.svg)](https://gitHub.com/MegaMek/megamek/releases/)                                                  | [![Release](https://img.shields.io/github/release/MegaMek/megameklab.svg)](https://gitHub.com/MegaMek/megameklab/releases/)                                                      | [![Release](https://img.shields.io/github/release/MegaMek/mekhq.svg)](https://gitHub.com/MegaMek/mekhq/releases/)                                                 |
| Javadocs | [![javadoc](https://badgen.net/badge/javadoc/master/red?icon=github)](https://megamek.org/megamek) | [![javadoc](https://badgen.net/badge/javadoc/master/red?icon=github)](https://megamek.org/megameklab) | [![javadoc](https://badgen.net/badge/javadoc/master/red?icon=github)](https://megamek.org/mekhq) |
| License        | [![GPLv3 license](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0.html)                                                     | [![GPLv3 license](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0.html)                                                               | [![GPLv3 license](https://img.shields.io/badge/License-GPLv3-blue.svg)](http://www.gnu.org/licenses/gpl-3.0.html)                                                 |
| Build (CI)     | [![MM Nightly CI](https://github.com/MegaMek/megamek/workflows/MegaMek%20Nightly%20CI/badge.svg)](https://github.com/MegaMek/megamek/actions/workflows/nightly-ci.yml) | [![MML Nightly CI](https://github.com/MegaMek/megameklab/workflows/MegaMekLab%20Nightly%20CI/badge.svg)](https://github.com/MegaMek/megameklab/actions/workflows/nightly-ci.yml) | [![MHQ Nightly CI](https://github.com/MegaMek/mekhq/workflows/MekHQ%20Nightly%20CI/badge.svg)](https://github.com/MegaMek/mekhq/actions/workflows/nightly-ci.yml) |
| Issues         | [![GitHub Issues](https://badgen.net/github/open-issues/MegaMek/megamek)](https://gitHub.com/MegaMek/megamek/issues/)                                                  | [![GitHub Issues](https://badgen.net/github/open-issues/MegaMek/megameklab)](https://gitHub.com/MegaMek/megameklab/issues/)                                                      | [![GitHub Issues](https://badgen.net/github/open-issues/MegaMek/mekhq)](https://gitHub.com/MegaMek/mekhq/issues/)                                                 |
| PRs            | [![GitHub Open Pull Requests](https://badgen.net/github/open-prs/MegaMek/megamek)](https://gitHub.com/MegaMek/megamek/pull/)                                           | [![GitHub Open Pull Requests](https://badgen.net/github/open-prs/MegaMek/megameklab)](https://gitHub.com/MegaMek/megameklab/pull/)                                               | [![GitHub Open Pull Requests](https://badgen.net/github/open-prs/MegaMek/mekhq)](https://gitHub.com/MegaMek/mekhq/pull/)                                          |
| Code Coverage  | [![MegaMek codecov.io](https://codecov.io/github/MegaMek/megamek/coverage.svg)](https://codecov.io/github/MegaMek/megamek)                                             | [![MegaMekLab codecov.io](https://codecov.io/github/MegaMek/megameklab/coverage.svg)](https://codecov.io/github/MegaMek/megameklab)                                              | [![MekHQ codecov.io](https://codecov.io/github/MegaMek/mekhq/coverage.svg)](https://codecov.io/github/MegaMek/mekhq)                                              |

Note that not everything has been implemented across the suite at this time, which will lead to gaps.

## Compiling

1) Install [Gradle](https://gradle.org/).

2) Follow the [instructions on the wiki](https://github.com/MegaMek/megamek/wiki/Working-With-Gradle) for using Gradle.

### 3.1 Style Guide

When contributing to this project, please enable the EditorConfig option within your IDE to ensure some basic compliance
with our [style guide](https://github.com/MegaMek/megamek/wiki/MegaMek-Coding-Style-Guide) which includes some defaults
for line length, tabs vs spaces, etc. When all else fails, we follow
the [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html).

The first ensures compliance with with the EditorConfig file, the other works with the Google Style Guide for most of
the rest.

## Support

For bugs, crashes, or other issues you can fill out a [GitHub issue request](https://github.com/MegaMek/mekhq/issues).

## Licensing

MekHQ is licensed under a dual-licensing approach:

### Code License

All source code is licensed under the GNU General Public License v3.0 (GPLv3). See the [LICENSE.code](LICENSE.code) file
for details.

### Data/Assets License

Game data, artwork, and other non-code assets are licensed under the Creative Commons Attribution-NonCommercial 4.0
International License (CC-BY-NC-4.0). See the [LICENSE.assets](LICENSE.assets) file for details.

### BattleTech IP Notice

MechWarrior, BattleMech, `Mech, and AeroTech are registered trademarks of The Topps Company, Inc. All Rights Reserved.
Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of InMediaRes Productions, LLC.

The BattleTech name for electronic games is a trademark of Microsoft Corporation.

MegaMek is an unofficial, fan-created digital adaptation and is not affiliated with, endorsed by, or licensed by
Microsoft Corporation, The Topps Company, Inc., or Catalyst Game Labs.

### Full Licensing Details

For complete information about licensing, including specific directories and files, please see the [LICENSE](LICENSE)
document.
