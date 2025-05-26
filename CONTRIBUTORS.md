# Authors and Contributors

We are grateful for all the contributions we have received over the years and wanted to make sure we included all
possible ones.

## Original author

Jay Lawson <jaylawson39@yahoo.com>

## Current Maintainer

MegaMek GitHub Organization <https://github.com/MegaMek> with the main [MegaMek](https://megamek.org)

## How we generated this list

This list is taken from the API, filtered to just pull the login name and GitHub URL, sorted, then added here. The
commands that were used to
generate this list are as follows:

```bash
gh api -H "Accept: application/vnd.github+json"  -H "X-GitHub-Api-Version: 2022-11-28" '/repos/megamek/mekhq/stats/contributors' > contributors.json
```

From this list, we used `irb` (Interactive Ruby) to process and output that is below:

```ruby
contrib = JSON.parse(File.read('contributors.json'))
filter = contrib.filter_map { |record| [record['author']['login'], record['author']['html_url']] unless record == nil || record['author'] == nil }
filter.sort_by { |user, _| user }.each { |user_name, url| puts "- #{user_name} <#{url}>\n" }
```

## Contributors

Last updated: 2025-05-19

- AaronGullickson <https://github.com/AaronGullickson>
- Akjosch <https://github.com/Akjosch>
- Algebro7 <https://github.com/Algebro7>
- BLOODWOLF333 <https://github.com/BLOODWOLF333>
- BlindGuyNW <https://github.com/BlindGuyNW>
- Bonepart <https://github.com/Bonepart>
- BullseyeSmith <https://github.com/BullseyeSmith>
- ChaoticInsanity <https://github.com/ChaoticInsanity>
- DM0000 <https://github.com/DM0000>
- Dark-Hobbit <https://github.com/Dark-Hobbit>
- Dylan-M <https://github.com/Dylan-M>
- HammerGS <https://github.com/HammerGS>
- HoneySkull <https://github.com/HoneySkull>
- IllianiBird <https://github.com/IllianiBird>
- Krashner <https://github.com/Krashner>
- Kurios <https://github.com/Kurios>
- LadyAdia <https://github.com/LadyAdia>
- Lapras <https://github.com/Lapras>
- LaserEye32 <https://github.com/LaserEye32>
- NickAragua <https://github.com/NickAragua>
- ObviousTech <https://github.com/ObviousTech>
- Qwertronix <https://github.com/Qwertronix>
- RAldrich <https://github.com/RAldrich>
- Rewstyr <https://github.com/Rewstyr>
- RexPearce <https://github.com/RexPearce>
- SJuliez <https://github.com/SJuliez>
- Saklad5 <https://github.com/Saklad5>
- Scoppio <https://github.com/Scoppio>
- Setsul <https://github.com/Setsul>
- Skoraks <https://github.com/Skoraks>
- Sleet01 <https://github.com/Sleet01>
- Taharqa <https://github.com/Taharqa>
- TenkawaBC <https://github.com/TenkawaBC>
- Thom293 <https://github.com/Thom293>
- UlyssesSockdrawer <https://github.com/UlyssesSockdrawer>
- UrsKR <https://github.com/UrsKR>
- VicenteCartas <https://github.com/VicenteCartas>
- WeaverThree <https://github.com/WeaverThree>
- Windchild292 <https://github.com/Windchild292>
- arlith <https://github.com/arlith>
- aunoor <https://github.com/aunoor>
- azure-pipelines[bot] <https://github.com/apps/azure-pipelines>
- bandildo <https://github.com/bandildo>
- binaryspica <https://github.com/binaryspica>
- dantmurph <https://github.com/dantmurph>
- dependabot[bot] <https://github.com/apps/dependabot>
- dericpage <https://github.com/dericpage>
- dtsosie <https://github.com/dtsosie>
- firefly2442 <https://github.com/firefly2442>
- fmoody <https://github.com/fmoody>
- gcoopercos <https://github.com/gcoopercos>
- giorgiga <https://github.com/giorgiga>
- goron111 <https://github.com/goron111>
- jackreichelt <https://github.com/jackreichelt>
- jayof9s <https://github.com/jayof9s>
- joshua-plautz <https://github.com/joshua-plautz>
- jschmetzer <https://github.com/jschmetzer>
- kipstafoo <https://github.com/kipstafoo>
- kuronekochomusuke <https://github.com/kuronekochomusuke>
- laptopsftw <https://github.com/laptopsftw>
- luiges90 <https://github.com/luiges90>
- mkerensky <https://github.com/mkerensky>
- mstjohn <https://github.com/mstjohn>
- nderwin <https://github.com/nderwin>
- neoancient <https://github.com/neoancient>
- nutritiousemployee <https://github.com/nutritiousemployee>
- pavelbraginskiy <https://github.com/pavelbraginskiy>
- pheonixstorm <https://github.com/pheonixstorm>
- psikomonkie <https://github.com/psikomonkie>
- repligator <https://github.com/repligator>
- rjhancock <https://github.com/rjhancock>
- savanik <https://github.com/savanik>
- sixlettervariables <https://github.com/sixlettervariables>
- slater-jay <https://github.com/slater-jay>
- sldfgunslinger2766 <https://github.com/sldfgunslinger2766>
- stonewall072 <https://github.com/stonewall072>
- swang300 <https://github.com/swang300>
- tombloor <https://github.com/tombloor>
- wildj79 <https://github.com/wildj79>
