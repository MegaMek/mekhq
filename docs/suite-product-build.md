# Local suite product archive

MekHQ's normal `:MekHQ:buildAllPackages` remains unchanged without suite properties.
To build a coordinated suite, check out the four sibling repositories (`megamek`,
`megameklab`, `mekhq`, `mm-data`) at the selected commits with no tracked
modifications or unknown package inputs. Supply the same five properties to each product build:

```text
-PsuiteReleaseVersion=0.51.01
-PsuiteMegaMekCommit=<40 lowercase hex>
-PsuiteMegaMekLabCommit=<40 lowercase hex>
-PsuiteMekHQCommit=<40 lowercase hex>
-PsuiteMmDataCommit=<40 lowercase hex>
```

The version must be canonical padded `major.minor.patch`, with components in
Java's signed integer range. Optional `suiteMegaMekVersion`,
`suiteMegaMekLabVersion`, and `suiteMekHQVersion` inputs each default to
`suiteReleaseVersion` and must have the same canonical format. For a Lab-only
change, reuse the old MegaMek release without publishing a newly numbered
MegaMek artifact; build Lab and MekHQ at the new suite version. MekHQ's
identity records the two bundled component versions and their source commits;
verification checks the included archives' own versions and dependency
source/data pins rather than requiring their unrelated Lab/HQ pins to match
the newer suite. Pass `-PsuiteMegaMekArchiveFile=<existing-archive>` or
`-PsuiteMegaMekLabArchiveFile=<existing-archive>` to verify and reuse an
unchanged product's archive without running its `assembleDist` task; its
bundled jar must still match the pinned companion jar used in MekHQ.
In suite mode, MekHQ uses its own product version for its Gradle
dependency coordinates and archive name; MegaMek supplies the packaged
`Version.properties` used by the three applications at runtime. The task
checks sibling archive records and product jars before packaging. Each archive
contains `suite-build.properties` at its normal root (schema 1, product,
version, four commits and minimum Java 21). MekHQ's archive continues to
include all three launcher jars, scripts and bundled data, while excluding
user-specific settings and user data.

For local verification, run `:MekHQ:verifySuiteMekHQArchive` with the same
properties. This builds the archive if absent; add
`-PsuiteArchiveFile=<path-to-MekHQ-version.tar.gz>` to inspect an existing
archive without rebuilding it. This is offline structural inspection against
the declared versions, pins and any supplied reused companion archives: it
compares root and lib JARs within the archive, not locally built JARs (whose
Build-Date may differ), and does not attest to the state of local checkouts.
Run companion product archive verifiers as well. Producing a publishable
archive instead requires `distTar` without `suiteArchiveFile`: before any
producer runs it checks pins, tracked changes, and untracked or ignored source
inputs in all four repositories. Ignored local data mirrors are allowed only
when identical to tracked mm-data files; build and Gradle cache outputs are
outside the source check. Use clean checkouts for final packaging.
