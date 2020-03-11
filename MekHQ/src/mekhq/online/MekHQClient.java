/*
 * Copyright (c) 2020 The MegaMek Team.
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ.  If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.online;

import java.util.HashSet;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import io.grpc.Channel;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import megamek.common.event.Subscribe;
import mekhq.MekHQ;
import mekhq.campaign.CampaignController;
import mekhq.campaign.event.GMModeEvent;
import mekhq.campaign.event.LocationChangedEvent;
import mekhq.campaign.event.NewDayEvent;
import mekhq.online.MekHQHostGrpc.MekHQHostBlockingStub;
import mekhq.online.MekHQHostGrpc.MekHQHostStub;
import mekhq.online.events.CampaignListUpdatedEvent;

public class MekHQClient {
    private final DateTimeFormatter dateFormatter = ISODateTimeFormat.date();
    private final ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.MekHQ");

    private final MekHQHostBlockingStub blockingStub;
    private final MekHQHostStub asyncStub;
    private final CampaignController controller;

    private final CountDownLatch finishLatch = new CountDownLatch(1);
    private StreamObserver<ClientMessage> messageBus;
    private ScheduledExecutorService pingExecutor;
    private ScheduledFuture<?> pings;

    private final Set<UUID> knownCampaigns = new HashSet<>();

    public MekHQClient(Channel channel, CampaignController controller) {
        blockingStub = MekHQHostGrpc.newBlockingStub(channel);
        asyncStub = MekHQHostGrpc.newStub(channel);
        this.controller = controller;
    }

    protected mekhq.campaign.Campaign getCampaign() {
        return controller.getLocalCampaign();
    }

    protected CampaignDetails getCampaignDetails() {
        return CampaignDetails.newBuilder()
            .setId(getCampaign().getId().toString())
            .setName(getCampaign().getName())
            .setDate(dateFormatter.print(getCampaign().getDateTime()))
            .setLocation(getCampaign().getLocation().getCurrentSystem().getId())
            .setIsGMMode(getCampaign().isGM())
            .build();
    }

    public void connect() {

        ConnectionRequest request = ConnectionRequest.newBuilder()
            .setVersion(resourceMap.getString("Application.version"))
            .setClient(getCampaignDetails())
            .build();

        ConnectionResponse response;
        try {
            response = blockingStub.connect(request);
        } catch (StatusRuntimeException e) {
            MekHQ.getLogger().warning(MekHQClient.class, "connect()", "RPC failed: " + e.getStatus());
            return;
        }

        CampaignDetails hostCampaign = response.getHost();

        MekHQ.getLogger().info(MekHQClient.class, "connect()",
                "Connected to Campaign: " + hostCampaign.getId() + " " + hostCampaign.getDate());

        controller.setHost(UUID.fromString(hostCampaign.getId()));
        controller.setHostName(hostCampaign.getName());
        controller.setHostDate(dateFormatter.parseDateTime(hostCampaign.getDate()));
        controller.setHostLocation(hostCampaign.getLocation());
        controller.setHostIsGMMode(hostCampaign.getIsGMMode());

        knownCampaigns.add(controller.getHost());
        for (CampaignDetails details : response.getCampaignsList()) {
            UUID clientId = UUID.fromString(details.getId());

            knownCampaigns.add(clientId);

            controller.addRemoteCampaign(clientId, details.getName(),
                dateFormatter.parseDateTime(details.getDate()), details.getLocation(), details.getIsGMMode());
        }

        createMessageBus();

        pingExecutor = Executors.newSingleThreadScheduledExecutor();
        pings = pingExecutor.scheduleAtFixedRate(() -> sendPing(), 0, 15, TimeUnit.SECONDS);

        MekHQ.registerHandler(this);
    }

    public void disconnect() {
        DisconnectionRequest request = DisconnectionRequest.newBuilder().setId(getCampaign().getId().toString())
                .build();

        try {
            DisconnectionResponse response = blockingStub.disconnect(request);
            MekHQ.getLogger().info(MekHQClient.class, "disconnect()",
                    "Disconnected from Campaign: " + response.getId());
        } catch (StatusRuntimeException e) {
            MekHQ.getLogger().warning(MekHQClient.class, "disconnect()", "RPC failed: " + e.getStatus());
        }

        controller.setHost(getCampaign().getId());
    }

    private void createMessageBus() {
        messageBus = asyncStub.messageBus(new StreamObserver<ServerMessage>() {
            @Override
            public void onNext(ServerMessage message) {
                UUID id = UUID.fromString(message.getId());
                Any payload = message.getMessage();
                try {
                    if (payload.is(Ping.class)) {
                        handlePing(id, payload.unpack(Ping.class));
                    } else if (payload.is(Pong.class)) {
                        handlePong(id, payload.unpack(Pong.class));
                    } else if (payload.is(CampaignDateChanged.class)) {
                        handleCampaignDateChanged(id, payload.unpack(CampaignDateChanged.class));
                    } else if (payload.is(GMModeChanged.class)) {
                        handleGMModeChanged(id, payload.unpack(GMModeChanged.class));
                    } else if (payload.is(LocationChanged.class)) {
                        handleLocationChanged(id, payload.unpack(LocationChanged.class));
                    }
                } catch (InvalidProtocolBufferException e) {
                    MekHQ.getLogger().error(MekHQClient.class, "messageBus::onNext()", "RPC protocol error: " + e.getMessage(), e);
                    handleRpcException(e);
                }
            }

            @Override
            public void onError(Throwable t) {
                MekHQ.getLogger().error(MekHQClient.class, "messageBus::onNext()", "RPC protocol error: " + t.getMessage(), t);
                finishLatch.countDown();
            }

            @Override
            public void onCompleted() {
                finishLatch.countDown();
            }
        });
    }

    protected void handleRpcException(Throwable t) {
        messageBus.onError(Status.INTERNAL
            .withDescription(t.getMessage())
            .augmentDescription("messageBus()")
            .withCause(t) // This can be attached to the Status locally, but NOT transmitted to the client!
            .asRuntimeException());
    }

    private void sendPing() {
        Ping ping = Ping.newBuilder()
            .setCampaign(getCampaignDetails())
            .build();
        messageBus.onNext(buildMessage(ping));
    }

    protected void handlePing(UUID id, Ping ping) {
        Pong pong = Pong.newBuilder()
            .setCampaign(getCampaignDetails())
            .build();

        CampaignDetails hostCampaign = ping.getCampaign();

        MekHQ.getLogger().info(MekHQClient.class, "handlePing()", String.format("-> PING: %s %s %s", id, hostCampaign.getDate(), hostCampaign.getLocation()));

        messageBus.onNext(buildMessage(pong));
    }

    protected void handlePong(UUID id, Pong pong) {
        CampaignDetails hostCampaign = pong.getCampaign();

        String date = hostCampaign.getDate();
        DateTime hostDate = dateFormatter.parseDateTime(date);
        String locationId = hostCampaign.getLocation();

        MekHQ.getLogger().info(MekHQClient.class, "handlePong()", String.format("-> PONG: %s %s %s (%d connected)", id, hostDate, locationId, pong.getCampaignsCount()));

        controller.setHostDate(hostDate);
        controller.setHostLocation(locationId);
        controller.setHostIsGMMode(hostCampaign.getIsGMMode());

        Set<UUID> foundCampaigns = new HashSet<>();
        foundCampaigns.add(id);

        for (CampaignDetails campaign : pong.getCampaignsList()) {
            UUID clientId = UUID.fromString(campaign.getId());

            foundCampaigns.add(clientId);

            controller.addRemoteCampaign(clientId, campaign.getName(),
                dateFormatter.parseDateTime(campaign.getDate()), campaign.getLocation(), campaign.getIsGMMode());
        }

        controller.setActiveCampaigns(foundCampaigns);

        MekHQ.triggerEvent(new CampaignListUpdatedEvent());
    }

    protected void handleCampaignDateChanged(UUID hostId, CampaignDateChanged dateChanged) {
        String date = dateChanged.getDate();
        DateTime campaignDate = dateFormatter.parseDateTime(date);
        UUID campaignId = UUID.fromString(dateChanged.getId());
        if (hostId.equals(campaignId)) {
            controller.setHostDate(campaignDate);
            MekHQ.getLogger().info(MekHQClient.class, "handleCampaignDateChanged()", String.format("<- HOST CampaignDateChanged: %s", date));
        } else {
            controller.setRemoteCampaignDate(campaignId, campaignDate);
        }

        MekHQ.triggerEvent(new CampaignListUpdatedEvent());
    }

    protected void handleGMModeChanged(UUID hostId, GMModeChanged gmModeChanged) {
        boolean isGMMode = gmModeChanged.getValue();
        UUID campaignId = UUID.fromString(gmModeChanged.getId());
        if (hostId.equals(campaignId)) {
            controller.setHostIsGMMode(isGMMode);
            MekHQ.getLogger().info(MekHQClient.class, "handleGMModeChanged()", String.format("<- HOST GMModeChanged: %s", isGMMode ? "ON" : "OFF"));
        } else {
            controller.setRemoteCampaignGMMode(campaignId, isGMMode);
        }

        MekHQ.triggerEvent(new CampaignListUpdatedEvent());
    }

    protected void handleLocationChanged(UUID hostId, LocationChanged locationChanged) {
        String locationId = locationChanged.getLocation();
        boolean isGM = locationChanged.getIsGmMovement();
        UUID campaignId = UUID.fromString(locationChanged.getId());
        if (hostId.equals(campaignId)) {
            controller.setHostLocation(locationId);
            MekHQ.getLogger().info(MekHQClient.class, "handleLocationChanged()", String.format("<- HOST LocationChanged: %s (%s)", locationId, isGM ? "GM Moved" : "KF Jump"));
        } else {
            controller.setRemoteCampaignLocation(campaignId, locationId);
        }

        MekHQ.triggerEvent(new CampaignListUpdatedEvent());
    }

    @Subscribe
    public void handle(NewDayEvent evt) {
        CampaignDateChanged dateChanged = CampaignDateChanged.newBuilder()
            .setId(getCampaign().getId().toString())
            .setDate(dateFormatter.print(getCampaign().getDateTime()))
            .build();

        messageBus.onNext(buildMessage(dateChanged));
    }

    @Subscribe
    public void handle(GMModeEvent evt) {
        GMModeChanged gmModeChanged = GMModeChanged.newBuilder()
            .setId(getCampaign().getId().toString())
            .setValue(evt.isGMMode())
            .build();

        messageBus.onNext(buildMessage(gmModeChanged));
    }

    @Subscribe
    public void handle(LocationChangedEvent evt) {
        LocationChanged locationChanged = LocationChanged.newBuilder()
            .setId(getCampaign().getId().toString())
            .setLocation(evt.getLocation().getCurrentSystem().getId())
            .setIsGmMovement(!evt.isKFJump())
            .build();

        messageBus.onNext(buildMessage(locationChanged));
    }

    private ClientMessage buildMessage(Message payload) {
        return ClientMessage.newBuilder()
            .setTimestamp(getTimestamp())
            .setId(getCampaign().getId().toString())
            .setMessage(Any.pack(payload))
            .build();
    }

    private static Timestamp getTimestamp() {
        long millis = System.currentTimeMillis();
        return Timestamp.newBuilder().setSeconds(millis / 1000)
            .setNanos((int) ((millis % 1000) * 1000000))
            .build();
    }
}
