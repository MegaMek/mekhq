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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
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

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import megamek.common.event.Subscribe;
import mekhq.MekHQ;
import mekhq.campaign.CampaignController;
import mekhq.campaign.RemoteCampaign;
import mekhq.campaign.event.GMModeEvent;
import mekhq.campaign.event.LocationChangedEvent;
import mekhq.campaign.event.NewDayEvent;
import mekhq.online.MekHQHostGrpc;
import mekhq.online.events.CampaignConnectedEvent;
import mekhq.online.events.CampaignDisconnectedEvent;
import mekhq.online.events.CampaignListUpdatedEvent;

public class MekHQServer {
    private final int port;
    private final MekHQHostService service;
    private final Server server;

    private ScheduledExecutorService pingExecutor;
    private ScheduledFuture<?> pings;

    public MekHQServer(int port, CampaignController controller) throws IOException {
        this(ServerBuilder.forPort(port), port, controller);
    }

    public MekHQServer(ServerBuilder<?> serverBuilder, int port, CampaignController controller) {
        this.port = port;
        service = new MekHQHostService(controller);
        server = serverBuilder.addService(service).build();
    }

    /** Start serving requests. */
    public void start() throws IOException {
        server.start();

        MekHQ.registerHandler(this);

        pingExecutor = Executors.newSingleThreadScheduledExecutor();
        pings = pingExecutor.scheduleAtFixedRate(() -> service.sendPings(), 1, 15, TimeUnit.SECONDS);

        MekHQ.getLogger().info(MekHQServer.class, "start()", "Server started, listening on " + port);
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                // Use stderr here since the logger may have been reset by its JVM shutdown
                // hook.
                System.err.println("*** shutting down gRPC server since JVM is shutting down");
                try {
                    MekHQServer.this.stop();
                } catch (InterruptedException e) {
                    e.printStackTrace(System.err);
                }
                System.err.println("*** server shut down");
            }
        });
    }

    /** Stop serving requests and shutdown resources. */
    public void stop() throws InterruptedException {
        if (server != null) {
            server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
        }
    }

    /**
     * Await termination on the main thread since the grpc library uses daemon
     * threads.
     */
    private void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }

    @Subscribe
    public void handle(NewDayEvent evt) {
        service.notifyDayChanged();
    }

    @Subscribe
    public void handle(GMModeEvent evt) {
        service.notifyGMModeChanged(evt.isGMMode());
    }

    @Subscribe
    public void handle(LocationChangedEvent evt) {
        service.notifyLocationChanged(evt.getLocation().getCurrentSystem().getId(), !evt.isKFJump());
    }

    private static class MekHQHostService extends MekHQHostGrpc.MekHQHostImplBase {
        private final DateTimeFormatter dateFormatter = ISODateTimeFormat.date();
        private final ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.MekHQ");

        private final CampaignController controller;

        private final ConcurrentMap<UUID, UUID> outstandingPings = new ConcurrentHashMap<>();
        private final ConcurrentMap<UUID, StreamObserver<ServerMessage>> messageBus = new ConcurrentHashMap<>();

        MekHQHostService(CampaignController controller) {
            this.controller = controller;
        }

        private mekhq.campaign.Campaign getCampaign() {
            return controller.getLocalCampaign();
        }

        private CampaignDetails getCampaignDetails() {
            return CampaignDetails.newBuilder()
                .setId(getCampaign().getId().toString())
                .setName(getCampaign().getName())
                .setDate(dateFormatter.print(getCampaign().getDateTime()))
                .setLocation(getCampaign().getLocation().getCurrentSystem().getId())
                .setIsActive(true)
                .build();
        }

        @Override
        public void connect(ConnectionRequest request, StreamObserver<ConnectionResponse> responseObserver) {
            String version = resourceMap.getString("Application.version");
            if (!version.equalsIgnoreCase(request.getVersion())) {
                responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription(String.format("Version mismatch. Host %s. Client %s.", version, request.getVersion()))
                    .augmentDescription("connect()")
                    .asRuntimeException());
                return;
            }

            CampaignDetails clientCampaign = request.getClient();

            UUID id = UUID.fromString(clientCampaign.getId());
            if (getCampaign().getId().equals(id)) {
                responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription(String.format("Campaign %s cannot both HOST and CONNECT", id))
                    .augmentDescription("connect()")
                    .asRuntimeException());
                return;
            }

            ConnectionResponse response = ConnectionResponse.newBuilder().setVersion(version)
                    .setHost(getCampaignDetails())
                    .addAllCampaigns(convert(controller.getRemoteCampaigns())).build();
            responseObserver.onNext(response);

            controller.addActiveCampaign(id);
            controller.addRemoteCampaign(id, clientCampaign.getName(), DateTime.parse(clientCampaign.getDate()),
                clientCampaign.getLocation(), clientCampaign.getIsGMMode());

            responseObserver.onCompleted();

            MekHQ.triggerEvent(new CampaignConnectedEvent(id));
            MekHQ.triggerEvent(new CampaignListUpdatedEvent());
        }

        @Override
        public void disconnect(DisconnectionRequest request, StreamObserver<DisconnectionResponse> responseObserver) {
            UUID id = UUID.fromString(request.getId());

            controller.removeActiveCampaign(id);

            handleDisconnection(id);

            DisconnectionResponse response = DisconnectionResponse.newBuilder().setId(getCampaign().getId().toString())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

            MekHQ.triggerEvent(new CampaignDisconnectedEvent(id));
            MekHQ.triggerEvent(new CampaignListUpdatedEvent());
        }

        @Override
        public StreamObserver<ClientMessage> messageBus(StreamObserver<ServerMessage> responseObserver) {
            return new StreamObserver<ClientMessage>() {
                private UUID clientId;

                @Override
                public void onNext(ClientMessage message) {
                    clientId = UUID.fromString(message.getId());
                    Any payload = message.getMessage();
                    try {
                        if (payload.is(Ping.class)) {
                            handlePing(responseObserver, clientId, payload.unpack(Ping.class));
                        } else if (payload.is(Pong.class)) {
                            handlePong(responseObserver, clientId, payload.unpack(Pong.class));
                        } else if (payload.is(CampaignDateChanged.class)) {
                            handleCampaignDateChanged(responseObserver, clientId, payload.unpack(CampaignDateChanged.class));
                        } else if (payload.is(GMModeChanged.class)) {
                            handleGMModeChanged(responseObserver, clientId, payload.unpack(GMModeChanged.class));
                        } else if (payload.is(LocationChanged.class)) {
                            handleLocationChanged(responseObserver, clientId, payload.unpack(LocationChanged.class));
                        }
                    } catch (InvalidProtocolBufferException e) {
                        MekHQ.getLogger().error(MekHQHostService.class, "messageBus::onNext()", "RPC protocol error: " + e.getMessage(), e);
                        responseObserver.onError(Status.INTERNAL
                            .withDescription(e.getMessage())
                            .augmentDescription("messageBus()")
                            .withCause(e) // This can be attached to the Status locally, but NOT transmitted to the client!
                            .asRuntimeException());

                        MekHQ.triggerEvent(new CampaignDisconnectedEvent(clientId));
                    }
                }

                @Override
                public void onError(Throwable t) {
                    MekHQ.getLogger().error(MekHQHostService.class, "messageBus::onError()",
                        String.format("RPC protocol error from client %s: %s", clientId, t.getMessage()), t);

                    messageBus.remove(clientId);

                    if (clientId != null) {
                        MekHQ.triggerEvent(new CampaignDisconnectedEvent(clientId));
                    }
                }

                @Override
                public void onCompleted() {
                    messageBus.remove(clientId);
                    responseObserver.onCompleted();

                    if (clientId != null) {
                        MekHQ.triggerEvent(new CampaignDisconnectedEvent(clientId));
                    }
                }
            };
        }

        private void addMessageBus(UUID campaignId, StreamObserver<ServerMessage> responseObserver) {
            messageBus.putIfAbsent(Objects.requireNonNull(campaignId), responseObserver);
        }

        private void handleDisconnection(UUID campaignId) {
            StreamObserver<ServerMessage> existingMessageBus = messageBus.remove(campaignId);
            if (existingMessageBus != null) {
                existingMessageBus.onCompleted();
            }

            MekHQ.triggerEvent(new CampaignDisconnectedEvent(campaignId));
        }

        public void sendPings() {
            Ping ping = Ping.newBuilder()
                .setCampaign(getCampaignDetails())
                .build();

            List<UUID> toRemove = new ArrayList<>();
            for (Map.Entry<UUID, StreamObserver<ServerMessage>> client : messageBus.entrySet()) {
                UUID clientId = client.getKey();

                MekHQ.getLogger().info(MekHQHostService.class, "handlePing()", "<- PING: " + clientId);

                // If we have a PING without a PONG, this client is no longer active.
                if (null != outstandingPings.put(clientId, clientId)) {
                    controller.removeActiveCampaign(clientId);
                }

                try {
                    client.getValue().onNext(buildResponse(Ping.newBuilder(ping).build()));
                } catch (Exception e) {
                    MekHQ.getLogger().error(MekHQHostService.class, "handlePing()", "Failed to ping campaign " + clientId, e);
                    toRemove.add(clientId);
                }
            }

            for (UUID disconnectedCampaignId : toRemove) {
                handleDisconnection(disconnectedCampaignId);
            }

            if (!toRemove.isEmpty()) {
                MekHQ.triggerEvent(new CampaignListUpdatedEvent());
            }
        }

        private void handlePing(StreamObserver<ServerMessage> responseObserver, UUID campaignId, Ping ping) {
            addMessageBus(campaignId, responseObserver);

            CampaignDetails clientCampaign = ping.getCampaign();

            MekHQ.getLogger().info(MekHQHostService.class, "handlePing()", String.format("-> PING: %s %s %s", campaignId, clientCampaign.getDate(), clientCampaign.getLocation()));

            Pong pong = Pong.newBuilder()
                .setCampaign(getCampaignDetails())
                .addAllCampaigns(convert(controller.getRemoteCampaigns()))
                .build();
            responseObserver.onNext(buildResponse(pong));
        }

        private void handlePong(StreamObserver<ServerMessage> responseObserver, UUID campaignId, Pong pong) {
            outstandingPings.remove(campaignId);

            CampaignDetails clientCampaign = pong.getCampaign();

            MekHQ.getLogger().info(MekHQHostService.class, "handlePing()", String.format("-> PONG: %s %s %s", campaignId, clientCampaign.getDate(), clientCampaign.getLocation()));
        }

        protected void handleCampaignDateChanged(StreamObserver<ServerMessage> responseObserver, UUID clientId,
                CampaignDateChanged campaignDateChanged) {

            controller.setRemoteCampaignDate(clientId, dateFormatter.parseDateTime(campaignDateChanged.getDate()));

            sendToAllExcept(clientId, campaignDateChanged);
        }

        protected void handleGMModeChanged(
                StreamObserver<ServerMessage> responseObserver,
                UUID clientId,
                GMModeChanged gmModeChanged) {

            controller.setRemoteCampaignGMMode(clientId, gmModeChanged.getValue());

            sendToAllExcept(clientId, gmModeChanged);
        }

        protected void handleLocationChanged(StreamObserver<ServerMessage> responseObserver,
                UUID clientId,
                LocationChanged locationChanged) {

            controller.setRemoteCampaignLocation(clientId, locationChanged.getLocation());

            sendToAllExcept(clientId, locationChanged);
        }

        public void notifyDayChanged() {
            CampaignDateChanged dateChanged = CampaignDateChanged.newBuilder()
                .setId(getCampaign().getId().toString())
                .setDate(dateFormatter.print(getCampaign().getDateTime()))
                .build();

            sendToAll(dateChanged);
        }

        public void notifyGMModeChanged(boolean gmMode) {
            GMModeChanged gmModeChanged = GMModeChanged.newBuilder()
                .setId(getCampaign().getId().toString())
                .setValue(gmMode)
                .build();

            sendToAll(gmModeChanged);
        }

        public void notifyLocationChanged(String locationId, boolean isGMMovement) {
            LocationChanged locationChanged = LocationChanged.newBuilder()
                .setId(getCampaign().getId().toString())
                .setLocation(locationId)
                .setIsGmMovement(isGMMovement)
                .build();

            sendToAll(locationChanged);
        }

        private void sendToAll(Message message) {
            for (Map.Entry<UUID, StreamObserver<ServerMessage>> client : messageBus.entrySet()) {
                client.getValue().onNext(buildResponse(message));
            }
        }

        private void sendToAllExcept(UUID exceptId, Message message) {
            for (Map.Entry<UUID, StreamObserver<ServerMessage>> client : messageBus.entrySet()) {
                if (!client.getKey().equals(exceptId)) {
                    client.getValue().onNext(buildResponse(message));
                }
            }
        }

        private ServerMessage buildResponse(Message payload) {
            return ServerMessage.newBuilder()
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

        private Collection<CampaignDetails> convert(Collection<RemoteCampaign> remoteCampaigns) {
            List<CampaignDetails> converted = new ArrayList<>();
            for (RemoteCampaign remoteCampaign : remoteCampaigns) {
                converted.add(
                    CampaignDetails.newBuilder()
                        .setId(remoteCampaign.getId().toString())
                        .setName(remoteCampaign.getName())
                        .setDate(dateFormatter.print(remoteCampaign.getDate()))
                        .setLocation(remoteCampaign.getLocation().getId())
                        .setIsGMMode(remoteCampaign.isGMMode())
                        .setIsActive(controller.isCampaignActive(remoteCampaign.getId()))
                        .build());
            }
            return converted;
        }
    }
}
