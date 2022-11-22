# MekHQ

## Table of Contents
1. About
2. Status
3. Compiling
4. Support
5. License
6. Dependencies

## 1. About

MekHQ is a Java helper program for the [MegaMek](http://megamek.org)
game that allows users to load a list of entities from an XML file, perform repairs
and customizations, and then save the new entities to another XML file that
can be loaded into MegaMek. For more details, see
our [website](http://megamek.org/) and join our [Discord](https://discord.gg/XM54YH9396).

## 2. Status
| Type | MM Status | MML Status | MHQ Status |
| ---- | --------- | ---------- | ---------- |
| Latest Release | [![Release](https://img.shields.io/github/release/MegaMek/megamek.svg)](https://gitHub.com/MegaMek/megamek/releases/) | [![Release](https://img.shields.io/github/release/MegaMek/megameklab.svg)](https://gitHub.com/MegaMek/megameklab/releases/) | [![Release](https://img.shields.io/github/release/MegaMek/mekhq.svg)](https://gitHub.com/MegaMek/mekhq/releases/) |
| Javadocs | [![javadoc](https://javadoc.io/badge2/org.megamek/megamek/javadoc.svg?color=red)](https://javadoc.io/doc/org.megamek/megamek) | [![javadoc](https://javadoc.io/badge2/org.megamek/megameklab/javadoc.svg?color=red)](https://javadoc.io/doc/org.megamek/megameklab) | [![javadoc](https://javadoc.io/badge2/org.megamek/mekhq/javadoc.svg?color=red)](https://javadoc.io/doc/org.megamek/mekhq) |
| License | [![GPLv3 license](https://img.shields.io/badge/License-GPLv2-blue.svg)](http://www.gnu.org/licenses/old-licenses/gpl-2.0.html) | [![GPLv3 license](https://img.shields.io/badge/License-GPLv2-blue.svg)](http://www.gnu.org/licenses/old-licenses/gpl-2.0.html) | [![GPLv3 license](https://img.shields.io/badge/License-GPLv3-blue.svg)](http://www.gnu.org/licenses/gpl-3.0.html) |
| Build (CI) | [![MM Nightly CI](https://github.com/MegaMek/megamek/workflows/MegaMek%20Nightly%20CI/badge.svg)](https://github.com/MegaMek/megamek/actions/workflows/nightly-ci.yml) | [![MML Nightly CI](https://github.com/MegaMek/megameklab/workflows/MegaMekLab%20Nightly%20CI/badge.svg)](https://github.com/MegaMek/megameklab/actions/workflows/nightly-ci.yml) | [![MHQ Nightly CI](https://github.com/MegaMek/mekhq/workflows/MekHQ%20Nightly%20CI/badge.svg)](https://github.com/MegaMek/mekhq/actions/workflows/nightly-ci.yml) |
| Issues | [![GitHub Issues](https://badgen.net/github/open-issues/MegaMek/megamek)](https://gitHub.com/MegaMek/megamek/issues/) | [![GitHub Issues](https://badgen.net/github/open-issues/MegaMek/megameklab)](https://gitHub.com/MegaMek/megameklab/issues/) | [![GitHub Issues](https://badgen.net/github/open-issues/MegaMek/mekhq)](https://gitHub.com/MegaMek/mekhq/issues/) |
| PRs | [![GitHub Open Pull Requests](https://badgen.net/github/open-prs/MegaMek/megamek)](https://gitHub.com/MegaMek/megamek/pull/) | [![GitHub Open Pull Requests](https://badgen.net/github/open-prs/MegaMek/megameklab)](https://gitHub.com/MegaMek/megameklab/pull/) | [![GitHub Open Pull Requests](https://badgen.net/github/open-prs/MegaMek/mekhq)](https://gitHub.com/MegaMek/mekhq/pull/) |
| Lines | [![MM Lines](https://badgen.net/lgtm/lines/g/MegaMek/megamek/java)](https://gitHub.com/MegaMek/megamek/) | [![MML Lines](https://badgen.net/lgtm/lines/g/MegaMek/megameklab/java)](https://gitHub.com/MegaMek/megameklab/) | [![MHQ Lines](https://badgen.net/lgtm/lines/g/MegaMek/mekhq/java)](https://gitHub.com/MegaMek/mekhq/) |
| LGTM Code Quality | [![LGTM Code Quality](https://img.shields.io/lgtm/grade/java/g/MegaMek/megamek.svg?logo=lgtm&logoWidth=18)](https://lgtm.com/projects/g/MegaMek/megamek/context:java) | [![LGTM Code Quality](https://img.shields.io/lgtm/grade/java/g/MegaMek/megameklab.svg?logo=lgtm&logoWidth=18)](https://lgtm.com/projects/g/MegaMek/megameklab/context:java) | [![LGTM Code Quality](https://img.shields.io/lgtm/grade/java/g/MegaMek/mekhq.svg?logo=lgtm&logoWidth=18)](https://lgtm.com/projects/g/MegaMek/mekhq/context:java) |
| LGTM Alerts | [![LGTM Code Alerts](https://img.shields.io/lgtm/alerts/g/MegaMek/megamek.svg?logo=lgtm&logoWidth=18)](https://lgtm.com/projects/g/MegaMek/megamek/alerts/) | [![LGTM Code Alerts](https://img.shields.io/lgtm/alerts/g/MegaMek/megameklab.svg?logo=lgtm&logoWidth=18)](https://lgtm.com/projects/g/MegaMek/megameklab/alerts/) | [![LGTM Code Alerts](https://img.shields.io/lgtm/alerts/g/MegaMek/mekhq.svg?logo=lgtm&logoWidth=18)](https://lgtm.com/projects/g/MegaMek/mekhq/alerts/) |
| Code Coverage | [![MegaMek codecov.io](https://codecov.io/github/MegaMek/megamek/coverage.svg)](https://codecov.io/github/MegaMek/megamek) | [![MegaMekLab codecov.io](https://codecov.io/github/MegaMek/megameklab/coverage.svg)](https://codecov.io/github/MegaMek/megameklab) | [![MekHQ codecov.io](https://codecov.io/github/MegaMek/mekhq/coverage.svg)](https://codecov.io/github/MegaMek/mekhq) |

Note that not everything has been implemented across the suite at this time, which will lead to gaps.

## 3. Compiling
1) Install [Gradle](https://gradle.org/).

2) Follow the [instructions on the wiki](https://github.com/MegaMek/megamek/wiki/Working-With-Gradle) for using Gradle.


## 4. Support
For bugs, crashes, or other issues you can fill out a [GitHub issue request](https://github.com/MegaMek/mekhq/issues).


## 5. License
```
MekHQ is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
MekHQ is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
You should have received a copy of the GNU General Public License along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
```
Please see `MekHQ/license.txt` for more information.


## 6. Dependencies
These are all listed in the format "{ Project Name } ({ Optional - Alternate Project Name }) ({ Optional - Description }) : { Implemented Version } { Current Version Badge } : { Project Link }"

### 6.1. Internal Dependencies
MegaMek (MM) : N/A : https://github.com/MegaMek/megamek

MegaMekLab (MML) : N/A : https://github.com/MegaMek/megameklab

### 6.2. Build Script Dependencies
launch4j : 2.5.4 [ ![launch4j](https://img.shields.io/maven-metadata/v.svg?colorB=007ec6&label=Gradle&metadataUrl=https%3A%2F%2Fplugins.gradle.org%2Fm2%2Fedu%2Fsc%2Fseis%2Flaunch4j%2Fedu.sc.seis.launch4j.gradle.plugin%2Fmaven-metadata.xml)](https://plugins.gradle.org/plugin/edu.sc.seis.launch4j) : https://github.com/TheBoegl/gradle-launch4j

grgit : 5.0.0 [![Maven Central](https://img.shields.io/maven-central/v/org.ajoberstar.grgit/grgit-gradle.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22org.ajoberstar.grgit%22%20AND%20a:%22grgit-gradle%22) : https://github.com/ajoberstar/grgit

### 6.3. Suitewide Dependencies
Flat Look and Feel (FlatLaf) (Expanded Themes, including Dark Mode) : 2.6 [![Maven Central](https://img.shields.io/maven-central/v/com.formdev/flatlaf.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22com.formdev%22%20AND%20a:%22flatlaf%22) : https://github.com/JFormDesigner/FlatLaf

Jakarta XML Bind API (JAXB API) (XML Setup) : 4.0.0 [![Maven Central](https://img.shields.io/maven-central/v/jakarta.xml.bind/jakarta.xml.bind-api.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22jakarta.xml.bind%22%20AND%20a:%22jakarta.xml.bind-api%22) : https://eclipse-ee4j.github.io/jaxb-ri/

Jakarta XML Bind Runtime (JAXB Runtime) (XML Setup) : 4.0.1 [![Maven Central](https://img.shields.io/maven-central/v/org.glassfish.jaxb/jaxb-runtime.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22org.glassfish.jaxb%22%20AND%20a:%22jaxb-runtime%22) : https://eclipse-ee4j.github.io/jaxb-ri/

Log4j2 (Logging) : 2.19.0 [![Maven Central](https://img.shields.io/maven-central/v/org.apache.logging.log4j/log4j.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22org.apache.logging.log4j%22%20AND%20a:%22log4j%22) :  https://logging.apache.org/log4j/2.x/

JUnit Jupiter (Unit Testing) : 5.9.1 [![Maven Central](https://img.shields.io/maven-central/v/org.junit.jupiter/junit-jupiter.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22org.junit.jupiter%22%20AND%20a:%22junit-jupiter%22) : https://junit.org/junit5/

Mockito (Unit Testing) : 4.9.0 [![Maven Central](https://img.shields.io/maven-central/v/org.mockito/mockito-core.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22org.mockito%22%20AND%20a:%22mockito-core%22) : https://site.mockito.org/

### 6.4. MegaMek Dependencies
Jackson (Jackson JSON) (JSON setup used for the internal graphical preference setup) : 2.14.1 [![Jackson](https://img.shields.io/maven-central/v/com.fasterxml.jackson.core/jackson-core.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22com.fasterxml.jackson.core%22%20AND%20a:%22jackson-core%22) : https://github.com/FasterXML/jackson-core

SerialKiller (Java Deserialization Security) : Latest Develop : https://github.com/ikkisoft/SerialKiller

Jakarta Mail : 2.0.1 [![Maven Central](https://img.shields.io/maven-central/v/com.sun.mail/jakarta.mail.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22com.sun.mail%22%20AND%20a:%22jakarta.mail%22) : https://eclipse-ee4j.github.io/mail/

XStream (Legacy XML Setup) : 1.4.14 [![Maven Central](https://img.shields.io/maven-central/v/com.thoughtworks.xstream/xstream.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22com.thoughtworks.xstream%22%20AND%20a:%22xstream%22) : http://x-stream.github.io/

Apache Commons Text : 1.10.0 [![Maven Central](https://img.shields.io/maven-central/v/org.apache.commons/commons-text.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22org.apache.commons%22%20AND%20a:%22commons-text%22) : https://commons.apache.org/proper/commons-text/

Apache Freemarker : 2.3.31 [![Maven Central](https://img.shields.io/maven-central/v/org.freemarker/freemarker.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22org.freemarker%22%20AND%20a:%22freemarker%22) : https://freemarker.apache.org/
Update Note : You'll need to also increase the version of Freemarker being actively used in MegaMek's TemplateConfiguration.java file.

### 6.5. MegaMekLab Dependencies
Apache PDFBox : 2.0.27 [![Maven Central](https://img.shields.io/maven-central/v/org.apache.pdfbox/pdfbox.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22org.apache.pdfbox%22%20AND%20a:%22pdfbox%22) : https://pdfbox.apache.org/

Apache XMLGraphics Batik Bridge : 1.14 [![Maven Central](https://img.shields.io/maven-central/v/org.apache.xmlgraphics/batik-bridge.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22org.apache.xmlgraphics%22%20AND%20a:%22batik-bridge%22) : https://xmlgraphics.apache.org/batik/

Apache XMLGraphics Batik Codec : 1.14 [![Maven Central](https://img.shields.io/maven-central/v/org.apache.xmlgraphics/batik-codec.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22org.apache.xmlgraphics%22%20AND%20a:%22batik-codec%22) : https://xmlgraphics.apache.org/batik/

Apache XMLGraphics Batik Dom : 1.14 [![Maven Central](https://img.shields.io/maven-central/v/org.apache.xmlgraphics/batik-dom.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22org.apache.xmlgraphics%22%20AND%20a:%22batik-dom%22) : https://xmlgraphics.apache.org/batik/

Apache XMLGraphics Batik Rasterizer : 1.14 [![Maven Central](https://img.shields.io/maven-central/v/org.apache.xmlgraphics/batik-rasterizer.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22org.apache.xmlgraphics%22%20AND%20a:%22batik-rasterizer%22) : https://xmlgraphics.apache.org/batik/

Apache XMLGraphics Batik SVGGen : 1.14 [![Maven Central](https://img.shields.io/maven-central/v/org.apache.xmlgraphics/batik-svggen.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22org.apache.xmlgraphics%22%20AND%20a:%22batik-svggen%22) : https://xmlgraphics.apache.org/batik/

Apache XMLGraphics FOP : 2.7 [![Maven Central](https://img.shields.io/maven-central/v/org.apache.xmlgraphics/fop.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22org.apache.xmlgraphics%22%20AND%20a:%22fop%22) : https://xmlgraphics.apache.org/fop/

### 6.6. MekHQ Dependencies
Javax Vecmath : 1.5.2 [![Maven Central](https://img.shields.io/maven-central/v/javax.vecmath/vecmath.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22javax.vecmath%22%20AND%20a:%22vecmath%22)

Joda Time : 2.12.1 [![Maven Central](https://img.shields.io/maven-central/v/joda-time/joda-time.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22joda-time%22%20AND%20a:%22joda-time%22) : https://www.joda.org/joda-time/

Apache Commons CSV : 1.9.0 [![Maven Central](https://img.shields.io/maven-central/v/org.apache.commons/commons-csv.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22org.apache.commons%22%20AND%20a:%22commons-csv%22) : https://commons.apache.org/proper/commons-csv/

Apache Commons Math3 : 3.6.1 [![Maven Central](https://img.shields.io/maven-central/v/org.apache.commons/commons-math3.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22org.apache.commons%22%20AND%20a:%22commons-math3%22) : https://commons.apache.org/proper/commons-math/

Apache Commons Text : 1.10.0 [![Maven Central](https://img.shields.io/maven-central/v/org.apache.commons/commons-text.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22org.apache.commons%22%20AND%20a:%22commons-text%22) : https://commons.apache.org/proper/commons-text/

Commonmark : 0.21.0 [![Maven Central](https://img.shields.io/maven-central/v/org.commonmark/commonmark.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22org.commonmark%22%20AND%20a:%22commonmark%22) : https://github.com/commonmark/commonmark-java

JFreechart : 1.5.3 [![Maven Central](https://img.shields.io/maven-central/v/org.jfree/jfreechart.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22org.jfree%22%20AND%20a:%22jfreechart%22) : https://www.jfree.org/jfreechart/

Joda Money (Finances) : 1.0.2 [![Maven Central](https://img.shields.io/maven-central/v/org.joda/joda-money.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22org.joda%22%20AND%20a:%22joda-money%22) : https://www.joda.org/joda-money/

Mockito JUnit Jupiter (Unit Testing) : 4.9.0 [![Maven Central](https://img.shields.io/maven-central/v/org.mockito/mockito-junit-jupiter.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22org.mockito%22%20AND%20a:%22mockito-junit-jupiter%22) : https://site.mockito.org/
