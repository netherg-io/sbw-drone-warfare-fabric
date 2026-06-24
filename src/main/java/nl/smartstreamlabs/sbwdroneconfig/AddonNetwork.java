package nl.smartstreamlabs.sbwdroneconfig;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class AddonNetwork {
    private static final String PROTOCOL_VERSION = "7";
    public static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(new ResourceLocation(SbwDroneRangeConfig.MOD_ID, "main"))
            .networkProtocolVersion(() -> PROTOCOL_VERSION)
            .clientAcceptedVersions(PROTOCOL_VERSION::equals)
            .serverAcceptedVersions(PROTOCOL_VERSION::equals)
            .simpleChannel();

    private static int nextMessageId;
    private static boolean registered;

    private AddonNetwork() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        CHANNEL.messageBuilder(DroneJamSyncMessage.class, nextMessageId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(DroneJamSyncMessage::encode)
                .decoder(DroneJamSyncMessage::decode)
                .consumerMainThread(DroneJamSyncMessage::handle)
                .add();
        CHANNEL.messageBuilder(DroneEnergySyncMessage.class, nextMessageId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(DroneEnergySyncMessage::encode)
                .decoder(DroneEnergySyncMessage::decode)
                .consumerMainThread(DroneEnergySyncMessage::handle)
                .add();
        CHANNEL.messageBuilder(LucasFuelSyncMessage.class, nextMessageId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(LucasFuelSyncMessage::encode)
                .decoder(LucasFuelSyncMessage::decode)
                .consumerMainThread(LucasFuelSyncMessage::handle)
                .add();
        CHANNEL.messageBuilder(DroneFiberOpticSyncMessage.class, nextMessageId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(DroneFiberOpticSyncMessage::encode)
                .decoder(DroneFiberOpticSyncMessage::decode)
                .consumerMainThread(DroneFiberOpticSyncMessage::handle)
                .add();
        CHANNEL.messageBuilder(DroneOperatorVisibilityMessage.class, nextMessageId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(DroneOperatorVisibilityMessage::encode)
                .decoder(DroneOperatorVisibilityMessage::decode)
                .consumerMainThread(DroneOperatorVisibilityMessage::handle)
                .add();
        CHANNEL.messageBuilder(DroneSpotlightStateMessage.class, nextMessageId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(DroneSpotlightStateMessage::encode)
                .decoder(DroneSpotlightStateMessage::decode)
                .consumerMainThread(DroneSpotlightStateMessage::handle)
                .add();
        CHANNEL.messageBuilder(OpenSirenConfigMessage.class, nextMessageId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(OpenSirenConfigMessage::encode)
                .decoder(OpenSirenConfigMessage::decode)
                .consumerMainThread(OpenSirenConfigMessage::handle)
                .add();
        CHANNEL.messageBuilder(OpenJammerConfigMessage.class, nextMessageId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(OpenJammerConfigMessage::encode)
                .decoder(OpenJammerConfigMessage::decode)
                .consumerMainThread(OpenJammerConfigMessage::handle)
                .add();
        CHANNEL.messageBuilder(UpdateSirenBlacklistMessage.class, nextMessageId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(UpdateSirenBlacklistMessage::encode)
                .decoder(UpdateSirenBlacklistMessage::decode)
                .consumerMainThread(UpdateSirenBlacklistMessage::handle)
                .add();
        CHANNEL.messageBuilder(UpdateJammerRangeMessage.class, nextMessageId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(UpdateJammerRangeMessage::encode)
                .decoder(UpdateJammerRangeMessage::decode)
                .consumerMainThread(UpdateJammerRangeMessage::handle)
                .add();
        CHANNEL.messageBuilder(UpdateJammerWhitelistMessage.class, nextMessageId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(UpdateJammerWhitelistMessage::encode)
                .decoder(UpdateJammerWhitelistMessage::decode)
                .consumerMainThread(UpdateJammerWhitelistMessage::handle)
                .add();
        CHANNEL.messageBuilder(ToggleSpotlightMessage.class, nextMessageId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ToggleSpotlightMessage::encode)
                .decoder(ToggleSpotlightMessage::decode)
                .consumerMainThread(ToggleSpotlightMessage::handle)
                .add();
    }

    public static void sendDroneEnergy(ServerPlayer player, DroneEnergySyncMessage message) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
    }

    public static void sendLucasFuel(ServerPlayer player, LucasFuelSyncMessage message) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
    }

    public static void sendDroneJam(ServerPlayer player, DroneJamSyncMessage message) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
    }

    public static void sendDroneFiberOptic(ServerPlayer player, DroneFiberOpticSyncMessage message) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
    }

    public static void openSirenConfig(ServerPlayer player, OpenSirenConfigMessage message) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
    }

    public static void openJammerConfig(ServerPlayer player, OpenJammerConfigMessage message) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
    }

    public static void syncDroneOperatorVisibility(DroneOperatorVisibilityMessage message) {
        CHANNEL.send(PacketDistributor.ALL.noArg(), message);
    }

    public static void syncDroneSpotlightState(DroneSpotlightStateMessage message) {
        CHANNEL.send(PacketDistributor.ALL.noArg(), message);
    }

    public static void sendDroneSpotlightState(ServerPlayer player, DroneSpotlightStateMessage message) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
    }

    public static void updateSirenBlacklist(UpdateSirenBlacklistMessage message) {
        CHANNEL.sendToServer(message);
    }

    public static void updateJammerRange(UpdateJammerRangeMessage message) {
        CHANNEL.sendToServer(message);
    }

    public static void updateJammerWhitelist(UpdateJammerWhitelistMessage message) {
        CHANNEL.sendToServer(message);
    }

    public static void toggleSpotlight() {
        CHANNEL.sendToServer(new ToggleSpotlightMessage());
    }
}
