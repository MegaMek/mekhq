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
3. If the Remote Campaign is compatible with the Host Campaign, the Remote Campaign sends a Message stream.
4. The Host Campaign responds with a Message stream.
5. Gameplay messages flow over these streams.

Notes:
- If a Host Campaign cannot support the Remote Campaign version the connection is terminated.
- If a Remote Campaign cannot support the Host Campaign version the connection is terminated.
- If the Remote Campaign does not use the correct password the connection is terminated.

### Security
As Distributed MekHQ uses gRPC, TLS will be made available as an option to secure the underlying transport. Further, a ConnectionRequest may include an optional password. Messages sent over the streams will use Protobuf and not support arbitrary serialization.

### ConnectionRequest
The ConnectionRequest message is sent by the Remote Campaign when it first connects to the Host Campaign.

The ConnectionRequest message has the following properties:
- Remote Campaign Version
- Remote Campaign Details
- Password (*OPTIONAL*)

The Host Campaign shall ignore additional ConnectRequest messages once a connection has been established for a Remote Campaign.

### ConnectionResponse
The ConnectionResponse message is sent by the Host Campaign when it accepts a connection from a Remote Campaign.

The ConnectionResponse message has the following properties:
- Host Campaign Version
- Host Campaign Details
- Remote Campaign List
    - Remote Campaign Details

### Message Bus Messages
The Message Bus contains all client-to-server messages to be processed by the server and either redistributed or transformed.

Each message is encapsulated over a bidirectional gRPC stream with the following format:
- Timestamp
- Campaign UUID
- Metadata Map (String -> String)
- Message Bytes (protobuf)

#### CampaignDetails
The CampaignDetails message is not a message sent over the message bus, but rather a fundamental part of the message bus.
This message is included with most messages as a way to quickly convey the status of a given campaign. The details are
sufficient to keep up with the basic accounting required for distributed play.

The CampaignDetails message has the following properties:
- Campaign UUID
- Campaign Name
- Campaign Date
- Campaign Location
- Campaign GM Mode
- Campaign Activity (Optional)

CampaignDetails messages generated by the Host Campaign will contain a value indicating whether or not a campaign is
actively participating in the session. Messages sent by Remote Campaigns will not, and any value sent by them regarding
activity should be ignored.

#### Ping
The Ping message is sent from the Host Campaign or a Remote Campaign to determine liveness of each end of the message bus.

The Ping message has the following properties:
- Campaign Details

If the Host or a Remote Campaign receives a Ping message it should reply as soon as possible with a Pong message.

The Host will send a Ping message at regular intervals to each connected Remote Campaign.

Each Remote Campaign will send a Ping message at regular intervals to the Host Campaign.

#### Pong
The Pong message is sent from the Host Campaign or a Remote Campaign in response to a Ping message to confirm liveness of the campaign.

The Pong message has the following properties:
- Campaign Details
- Remote Campaign List (Host Only)
    - Remote Campaign Details

The Host Campaign or a Remote Campaign will respond with their own campaign details in the Pong message.

The Host Campaign will include Campaign Details for Remote Campaigns, and may include details for Campaigns not actively participating
in the current session.

#### CampaignDateChanged
The CampaignDateChanged message notifies the Host Campaign that a Remote Campaign has advanced days.

The CampaignDateChanged message has the following properties:
- Campaign UUID
- Campaign Date

This message is redistributed to all connected campaigns.

#### GMModeChanged
The GMModeChanged message notifies the Host Campaign that a Remote Campaign has changed their GM Mode.

The GMModeChanged message has the following properties:
- Campaign UUID
- GM Mode

This message is redistributed to all connected campaigns.

#### LocationChanged
The LocationChanged message notifies the Host Campaign that a Remote Campaign has changed their location.

The LocationChanged message has the following properties:
- Campaign UUID
- New Location
- GM Movement

Campaigns should indicate whether or not the movement occurred due to a GM action.

This message is redistributed to all connected campaigns.

#### TeamAssignment
The TeamAssignment message notifies a Remote Campaign of team assignments.

The TeamAssignment message has the following properties:
- Team List
    - Remote Campaign UUID
    - Remote Campaign Team

The Host Campaign is always Team 1.

This message is only sent by the Host Campaign.

#### DailyLog
The DailyLog message notifies the Host Campaign of a daily log activity.

The DailyLog message has the following properties:
- Campaign UUID
- Log Date
- Log List
    - Log Entry

This message may be sent at any point in a day and may be sent multiple times, but implementations could delay sending this message until a day has completed.

This message is redistributed to all Remote Campaigns on the same team as the sending Campaign.

#### WireTransferRequest
The WireTransferRequest message notifies the Host Campaign of an intent to perform a wire transfer between two Campaigns.

The WireTransferRequest message has the following properties:
- Source Campaign UUID
- Target Campaign UUID
- Request UUID
- Money Amount

The amount may be positive (debiting the source campaign) or negative (debiting the target campaign).

The source Campaign must move any debited funds into a Pending account and may not use those funds until the transfer has been completed.

This message is redistributed only to the target Campaign.

#### WireTransferResponse
The WireTransferResponse message notifies the Host Campaign of the response to a wire transfer request.

The WireTransferResponse message has the following properties:
- Source Campaign UUID
- Target Campaign UUID
- Request UUID
- Accepted?

The target Campaign must send a rejection response if the request if it has insufficient funds to process a debit.

The target Campaign must move any debited funds into a Pending account and may not use those funds until the transfer has been completed.

The Host Campaign must send a rejection WireTransferCompleted message if the request is not recognized.

This message is only consumed by the Host Campaign.

#### WireTransferCompleted
The WireTransferCompleted message notifies two participating Campaigns that their wire transfer has been accepted by the Host.

The WireTransferCompleted message has the following properties:
- Source Campaign UUID
- Target Campaign UUID
- Request UUID
- Accepted?

This message must be rejected by the source or target Campaigns if they do not recognize it.

This message is sent to the originating source and target Campaigns upon receipt of a WireTransferResponse.

#### TradeRequest
The TradeRequest message notifies the Host Campaign of an intent to trade between two campaigns.

The TradeRequest message has the following properties:
- Source Campaign UUID
- Target Campaign UUID
- Request UUID
- ?

Details TBD

If the Source and Target Campaign already have an active trade, the Host Campaign shall send a rejection TradeCompleted to the Source Campaign.

This message is redistributed to the target campaign by the Host Campaign.

#### CancelTradeRequest
The CancelTradeRequest message notifies the Host Campaign that a trade partner has cancelled a request.

The CancelTradeRequest message has the following properties:
- Request UUID

The Host Campaign accepts this message and then sends a rejection TradeCompleted message to the Source and Target of the trade request.

#### CancelledTradeRequests
The CancelledTradeRequests message notifies the Host Campaign or a Remote Campaign of a set of trade requests that were previously cancelled.

The CancelledTradeRequests message has the following properties:
- Request UUID List

This message allows a campaign to identify trade requests they have cancelled. This message helps ensure eventual consistency among
campaigns that may not have received previous rejected TradeCompleted messages.

The duration rejected trade requests should be kept in the list is not defined, but should be long enough ensure all remote campaigns
are aware of the cancellation.

This message is redistributed to every campaign by the Host Campaign.

#### TradeDetails
The TradeDetails message notifies the Host Campaign that a trade partner has updated the details of their trade.

The TradeDetails message has the following properties:
- Campaign UUID
- Request UUID
- ?

Details TBD

If the Request UUID is not recognized this message should be ignored.

If the Campaign UUID is not one of the trading partners for the request, this message should be ignored.

This message is redistributed to the trading partners by the Host Campaign.

#### TradeAcceptance
The TradeAcceptance message notifies the Host Campaign that a trade partner has accepted the terms of the trade.

The TradeAcceptance message has the following properties:
- Campaign UUID
- Request UUID
- Accepted?

If the Request UUID is not recognized this message should be ignored.

If the Campaign UUID is not one of the trading partners for the request, this message should be ignored.

#### AcceptedTradeRequests
The AcceptedTradeRequests message notifies the Host Campaign or a Remote Campaign of a set of trade requests that were previously accepted.

The AcceptedTradeRequests message has the following properties:
- Request UUID List

This message allows a campaign to identify trade requests they have been accepted. This message helps ensure eventual consistency among
campaigns that may not have received previous acceptance TradeCompleted messages.

The duration accepted trade requests should be kept in the list is not defined, but should be long enough ensure all remote campaigns
are aware of the acceptance.

This message is redistributed to every campaign by the Host Campaign.

#### TradeCompleted
The TradeCompleted message notifies the source and target campaigns that a trade has been either succesfully completed or rejected.

The TradeCompleted message has the following properties:
- Source Campaign UUID
- Target Campaign UUID
- Request UUID
- Accepted?

If the Request UUID is not recognized this message should be ignored.

This message is sent by the Host Campaign to the Source and Target Campaigns of a trade request after either:
1. A trading partner has cancelled the trade (CancelTradeRequest)
2. Or, both trading partners have accepted the trade (TradeAcceptance)
