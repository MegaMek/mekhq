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
Distributed MekHQ will be a standard Client-Server architecture, with one instance
serving as the host of the session. The underlying protocol will be gRPC and the
message flow will be roughly PUBSUB.

The general message flow is as follows:
1. A Remote Campaign connects to a Host Campaign and sends a ConnectionRequest.
2. If the Host Campaign is compatible with the Remote Campaign, the Host Campaign responds with a ConnectionResponse.
3. If the Remote Campaign is compatible with the Host Campaign, the Remote Campaign sends a ClientMessageBusStream.
4. The Host Campaign responds with a ServerMessageBusStream.
5. Gameplay messages flow over these streams.

Notes:
- If a Host Campaign cannot support the Remote Campaign version the connection is terminated.
- If a Remote Campaign cannot support the Host Campaign version the connection is terminated.
- If the Remote Campaign does not use the correct password the connection is terminated.

### Security
As Distributed MekHQ uses gRPC, TLS will be made available as an option to secure the underlying transport. Further, a ConnectionRequest may include an optional password. Messages sent over the streams will use Protobuf and not support arbitrary serialization.

### ConnectionRequest
The ConnectionRequest message has the following properties:
- Remote Campaign Version
- Remote Campaign Name
- Remote Campaign UUID
- Remote Campaign Date
- Password (*OPTIONAL*)

### ConnectionResponse
The ConnectionResponse message has the following properties:
- Host Campaign Version
- Host Campaign Name
- Host Campaign UUID
- Host Campaign Date
- Remote Campaign List
    - Remote Campaign Name
    - Remote Campaign UUID
    - Remote Campaign Date
    - Remote Campaign Team

### Message Bus Messages
The Message Bus contains all client-to-server messages to be processed by the server and either redistributed or transformed.

#### CampaignDateChanged
The CampaignDateChanged message notifies the Host Campaign that a Remote Campaign has advanced days.
- Campaign UUID
- Campaign Date

This message is redistributed to all connected campaigns.

#### GMModeChanged
The GMModeChanged message notifies the Host Campaign that a Remote Campaign has changed their GM Mode.
- Campaign UUID
- Is GM?

This message is redistributed to all connected campaigns.

#### TeamAssignment
The TeamAssignment message notifies a Remote Campaign of team assignments.
- Team List
    - Remote Campaign UUID
    - Remote Campaign Team

The Host Campaign is always Team 1.

This message is only sent by the Host Campaign.

#### DailyLog
The DailyLog message notifies the Host Campaign of a daily log activity.
- Campaign UUID
- Log Date
- Log List
    - Log Entry

This message may be sent at any point in a day and may be sent mulitple times, but implementations could delay sending this message until a day has completed.

This message is redistributed to all Remote Campaigns on the same team as the sending Campaign.

#### WireTransferRequest
The WireTransferRequest message notifies the Host Campaign of an intent to perform a wire transfer between two Campaigns.
- Source Campaign UUID
- Target Campaign UUID
- Request UUID
- Money Amount

The amount may be positive (credit) or negative (debit).

This message is redistributed only to the target Campaign.

#### WireTransferResponse
The WireTransferResponse message notifies the Host Campaign of the response to a wire transfer request.
- Request UUID
- Accepted?

The Remote Campaign must send a rejection response if the request if it has insufficient funds.

This message is only consumed by the Host Campaign.

#### WireTransferCompleted
The WireTransferCompleted message notifies two participating Campaigns that their wire transfer has been accepted by the Host.
- Request UUID
- Accepted?

This message is sent to the originating source and target Campaigns upon receipt of a WireTransferResponse.
