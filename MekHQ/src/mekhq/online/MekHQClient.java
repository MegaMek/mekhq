package mekhq.online;

import java.util.ResourceBundle;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
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
import mekhq.campaign.event.NewDayEvent;
import mekhq.online.MekHQHostGrpc.MekHQHostBlockingStub;
import mekhq.online.MekHQHostGrpc.MekHQHostStub;

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

    public MekHQClient(Channel channel, CampaignController controller) {
        blockingStub = MekHQHostGrpc.newBlockingStub(channel);
        asyncStub = MekHQHostGrpc.newStub(channel);
        this.controller = controller;
    }

    protected mekhq.campaign.Campaign getCampaign() {
        return controller.getLocalCampaign();
    }

    public void connect() {
        ConnectionRequest request = ConnectionRequest.newBuilder().setId(getCampaign().getId().toString())
                .setName(getCampaign().getName()).setVersion(resourceMap.getString("Application.version"))
                .setDate(dateFormatter.print(getCampaign().getDateTime()))
                .setLocation(getCampaign().getLocation().getCurrentSystem().getId()).build();

        ConnectionResponse response;
        try {
            response = blockingStub.connect(request);
        } catch (StatusRuntimeException e) {
            MekHQ.getLogger().warning(MekHQClient.class, "connect()", "RPC failed: " + e.getStatus());
            return;
        }

        MekHQ.getLogger().info(MekHQClient.class, "connect()",
                "Connected to Campaign: " + response.getId() + " " + response.getDate());

        controller.setHost(UUID.fromString(response.getId()));
        controller.setHostName(response.getName());
        controller.setHostDate(DateTime.parse(response.getDate(), dateFormatter));
        controller.setHostLocation(response.getLocation());

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

                    } else if (payload.is(LocationChanged.class)) {

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

    protected void handleCampaignDateChanged(UUID id, CampaignDateChanged dateChanged) {
        String date = dateChanged.getDate();
        controller.setHostDate(dateFormatter.parseDateTime(date));
        MekHQ.getLogger().info(MekHQClient.class, "handleCampaignDateChanged()", String.format("<- HOST CampaignDateChanged: %s", date));
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
            .setDate(dateFormatter.print(getCampaign().getDateTime()))
            .setLocation(getCampaign().getLocation().getCurrentSystem().getId())
            .build();
        messageBus.onNext(buildMessage(ping));
    }

    protected void handlePing(UUID id, Ping ping) {
        Pong pong = Pong.newBuilder()
            .setDate(dateFormatter.print(getCampaign().getDateTime()))
            .setLocation(getCampaign().getLocation().getCurrentSystem().getId())
            .build();

        MekHQ.getLogger().info(MekHQClient.class, "handlePing()", String.format("-> PING: %s %s %s", id, ping.getDate(), ping.getLocation()));

        messageBus.onNext(buildMessage(pong));
    }

    protected void handlePong(UUID id, Pong pong) {
        String date = pong.getDate();
        DateTime hostDate = dateFormatter.parseDateTime(date);
        String locationId = pong.getLocation();

        MekHQ.getLogger().info(MekHQClient.class, "handlePong()", String.format("-> PONG: %s %s %s (%d connected)", id, hostDate, locationId, pong.getCampaignsCount()));

        controller.setHostDate(hostDate);
        controller.setHostLocation(locationId);

        for (Campaign campaign : pong.getCampaignsList()) {
            controller.addRemoteCampaign(UUID.fromString(campaign.getId()), campaign.getName(),
                dateFormatter.parseDateTime(campaign.getDate()), campaign.getLocation());
        }
    }

    @Subscribe
    public void handle(NewDayEvent evt) {
        CampaignDateChanged dateChanged = CampaignDateChanged.newBuilder()
            .setDate(dateFormatter.print(getCampaign().getDateTime()))
            .build();

        messageBus.onNext(buildMessage(dateChanged));
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
