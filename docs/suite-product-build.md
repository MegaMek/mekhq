# Local suite product archive

MekHQ's normal `:MekHQ:buildAllPackages` remains unchanged without suite properties.
To build a coordinated suite, check out the four sibling repositories (`megamek`,
`megameklab`, `mekhq`, `mm-data`) at the selected commits with no tracked
modifications. Supply the same five properties to each product build:

```text
-PsuiteReleaseVersion=0.51.01
-PsuiteMegaMekCommit=<40 lowercase hex>
-PsuiteMegaMekLabCommit=<40 lowercase hex>
-PsuiteMekHQCommit=<40 lowercase hex>
-PsuiteMmDataCommit=<40 lowercase hex>
```

The version must be canonical padded `major.minor.patch`, with components in
Java's signed integer range. In suite mode, MekHQ uses it for its Gradle
dependency coordinates and archive name; MegaMek supplies the packaged
`Version.properties` used by the three applications at runtime. The task
checks sibling archive records and product jars before packaging. Each archive
contains `suite-build.properties` at its normal root (schema 1, product,
version, four commits and minimum Java 21). MekHQ's archive continues to
include all three launcher jars, scripts and bundled data, while excluding
user-specific settings.

For local verification, run `:MekHQ:verifySuiteMekHQArchive` with the same
properties. This builds the archive if absent; add
`-PsuiteArchiveFile=<path-to-MekHQ-version.tar.gz>` to inspect an existing
archive without rebuilding it. Run companion product archive verifiers as
well. Suite builds fail on dirty tracked inputs or mismatched pins, so a
working tree with uncommitted source edits is not suitable for producing a
publishable archive.
