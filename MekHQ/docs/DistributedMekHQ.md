# Distributed MekHQ
Distributed MekHQ is an idea to allow a slightly more sophisticated verison
of "MekHQ by E-Mail". This document describes the design and architecture
for Distributed MekHQ.

## Objectives and Non-Goals
This section describes the objectives of Distributed MekHQ and what will
not be part of the design.

### Objectives
1. Support a common timeline between Campaigns.
2. Support common scenarios and contracts between Campaigns.
3. Support trading of parts, vehicles, and personnel between Campaigns.
4. Support centralized contract, unit, and personnel markets.
5. Support generation of OpFor from other Campaigns.

### Non-Goals
1. Distributed MekHQ will not support shared Campaigns.
2. Distributed MekHQ will not support shared parts, vehicles, or personnel.
3. Distributed MekHQ will not support controlling other Campaigns.
4. Distributed MekHQ will not support chat.

## Distributed MekHQ Design
This section details the high level design of Distributed MekHQ and serves
as the Use-Case basis for the architecture.

### Concepts
- **Host Campaign**: the Campaign which launched the distributed MekHQ session.
- **Remote Campaign**: any Campaign not running as part of the local MekHQ client.
- **Timeline**: the latest date among the connected Campaigns.
- **Teams**: connected Campaigns which cannot be generated as OpFor in a contract.
- **Trading**: a mechanism by which connected Campaigns can exchange parts, units,
personnel, and money in exchange for any combination of the same.

### Common Timeline
The fundamental feature of Distributed MekHQ is a common timeline shared among
the connected campaigns. Ultimately, each campaign will be managed and operated
independently, EXCEPT for advancing time. This is accomplished through one simple
invariant:
> No campaign may be further along in time than the Host campaign.

This invariant is held through the following means:
1. Only the Host Campaign may advance the day without restrictions.
2. Remote Campaigns may advance the day without contacting the Host Campaign if
   they are at least one day behind the Host Campaign.
3. Remote Campaigns may inform the Host Campaign they are ready to advance
   the day, but their local campaign will not advance until their client has
   been informed that the Host Campaign has moved beyond their local date.

#### Corner Cases
1. A Remote Campaign connects and is AHEAD of the Host Campaign.
    a. The Remote Campaign SHALL NOT advance time until the Host Campaign
       has moved beyond their local date.
2. A Remote Campaign connects and is markedly behind the Host Campaign.
    a. The Remote Campaign MAY advance time in bulk, but not past the
       Host Campaign.
    b. The Remote Campaign MAY be given the option to "Jump" to the Host
       Campaign's timeline, skipping all the usual checks. This should
       only be an option in GM Mode.
3. The Host Campaign disconnects.
    a. The Remote Campaign will be given the option to continue playing
       without a Host Campaign. If the timelines diverge before they
       reconnect, the invariants will be held through the aforementioned
       means.

### Common Scenarios and Contracts
This objective is TBD on its design details.

### Trading
If you give a mouse a cookie, it is said that they will want a glass of milk.
Trading is the natural extension to Distributed MekHQ.

Initial support will be limited to the following actions:
- Same-day wiring of funds.
- JumpPath distance + 1 day shipping of parts for a fee.
- JumpPath distance + 1 day shipping of STOCK units for a fee.
- JumpPath distance + 1 day transit of personnel for a fee.

Subsequent support will be added for the following actions:
- JumpPath distance + 1 day shipping of CUSTOM units for a fee.

Support may be considered for the following actions:
- Usage of company JumpShips and DropShips to transport traded items.

### Centralized Markets
Once Campaigns are connected they should operate under the same markets.
First-come, first-serve will be used, along with diversifying the selections
by having per-area markets. This will be accomplished by one simple invariant:
> The Host Campaign will be the arbiter of all sales and hires.

This invariant is held through the following means:
1. All purchase or hiring will be made as requests through the Host Campaign.
2. All purchase or hiring requests will be timestamped.
3. The Host Campaign will make requests through the same mechanism.
4. The Host Campaign will advertise all market changes to the Remote Campaigns.

Expansions to the existing markets will be needed to ensure sufficient diversity
exists to make this mode feel interesting.

### Generating OpFor from Remote Campaigns
This objective is TBD on its design details.

## Distributed MekHQ Architecture
TBD
