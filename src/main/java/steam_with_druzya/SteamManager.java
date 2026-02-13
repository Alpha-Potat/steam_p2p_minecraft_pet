package steam_with_druzya;

import com.codedisaster.steamworks.*;
import com.codedisaster.steamworks.SteamMatchmaking.ChatMemberStateChange;
import com.codedisaster.steamworks.SteamMatchmaking.ChatEntryType;
import com.codedisaster.steamworks.SteamMatchmaking.LobbyType;
import io.netty.buffer.Unpooled;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;

import org.slf4j.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SteamManager {
    public static final SteamManager INSTANCE = new SteamManager();
    private static final Logger LOGGER = YourMod.LOGGER;
    private static final int CHANNEL = 1;
    private static final int BUFFER_SIZE = 4096;

    private SteamNetworking networking;
    private SteamMatchmaking matchmaking;
    private SteamUser user;
    private MinecraftServer server;
    private final Map<SteamID, SteamConnection> connections = new ConcurrentHashMap<>();
    private SteamID lobbyID;
    private ScheduledExecutorService pollExecutor;

    public void init(SteamNetworking net, MinecraftServer srv) {
        networking = net;
        matchmaking = new SteamMatchmaking(new SteamMatchmakingCallbackAdapter());
        user = new SteamUser(new SteamUserCallbackAdapter());
        server = srv;
        pollExecutor = Executors.newScheduledThreadPool(1);
        pollExecutor.scheduleAtFixedRate(this::pollPackets, 0, 16, TimeUnit.MILLISECONDS);  // 60 TPS
        LOGGER.info("SteamManager ready!");
    }

    public void hostLobby() {
        matchmaking.createLobby(LobbyType.FriendsOnly, 8);
    }

    public void joinLobby(SteamID lobby) {
        matchmaking.joinLobby(lobby);
    }

    public void acceptSession(SteamID remote) {
        networking.acceptP2PSessionWithUser(remote);
        connections.put(remote, new SteamConnection(remote, this));
        if (server != null) {
            server.getPlayerManager().broadcast(Text.literal("Player joined via Steam: " + remote), false);
        }
        LOGGER.info("Accepted P2P session: " + remote);
    }

    private void pollPackets() {
        int[] size = new int[1];
        while (networking.isP2PPacketAvailable(size, CHANNEL)) {
            if (size[0] > BUFFER_SIZE) {
                LOGGER.warn("Packet too large: " + size[0]);
                continue;
            }
            byte[] data = new byte[size[0]];
            SteamID remote = new SteamID();
            int[] bytesRead = new int[1];
            int[] channelOut = new int[1];
            SteamNetworking.P2PSessionError[] err = new SteamNetworking.P2PSessionError[1];
            if (networking.readP2PPacket(remote, data, bytesRead, channelOut, err)) {
                if (err[0] == SteamNetworking.P2PSessionError.None) {
                    PacketByteBuf buf = new PacketByteBuf(Unpooled.wrappedBuffer(data, 0, bytesRead[0]));
                    SteamConnection conn = connections.get(remote);
                    if (conn != null) {
                        conn.handlePacket(buf);
                    }
                } else {
                    LOGGER.error("P2P read error: " + err[0]);
                }
            }
        }
    }

    public void sendPacket(SteamID remote, PacketByteBuf buf) {
        byte[] data = new byte[buf.readableBytes()];
        buf.readBytes(data);
        networking.sendP2PPacket(remote, data, data.length, SteamNetworking.P2PSend.Reliable, CHANNEL);
    }

    // Getters
    public SteamID getMyID() { return user.getSteamID(); }
    public SteamID getLobbyID() { return lobbyID; }

    private class SteamMatchmakingCallbackAdapter implements SteamMatchmakingCallback {
        @Override
        public void onFavoritesListChanged(int ip, int queryPort, int connPort, int appID, int flags, boolean add, int accountID) {}

        @Override
        public void onLobbyInvite(SteamID inviter, SteamID lobby, long gameID) {}

        @Override
        public void onLobbyEnter(SteamID lobby, int chatPermissions, boolean locked, SteamResult result) {}

        @Override
        public void onLobbyDataUpdate(SteamID lobby, SteamID member, boolean success) {}

        @Override
        public void onLobbyChatUpdate(SteamID lobby, SteamID userChanged, SteamID makingChange, ChatMemberStateChange stateChange) {}

        @Override
        public void onLobbyChatMessage(SteamID lobby, SteamID user, ChatEntryType entryType, int chatID) {}

        @Override
        public void onLobbyGameCreated(SteamID lobby, SteamID gameServer, int ip, short port) {}

        @Override
        public void onLobbyMatchList(int lobbiesMatching) {}

        @Override
        public void onLobbyKicked(SteamID lobby, SteamID admin, boolean dueToDisconnect) {}

        @Override
        public void onLobbyCreated(SteamResult result, SteamID lobby) {
            if (result == SteamResult.OK) {
                lobbyID = lobby;
                LOGGER.info("Steam lobby created: " + lobbyID);
            }
        }
    }

    private class SteamUserCallbackAdapter implements SteamUserCallback {
        @Override
        public void onValidateAuthTicket(SteamID steamID, SteamAuth.AuthSessionResponse authSessionResponse, SteamID ownerSteamID) {}

        @Override
        public void onMicroTxnAuthorization(int appID, long orderID, boolean authorized) {}

        @Override
        public void onEncryptedAppTicket(SteamResult result) {}
    }

    public static class SteamConnection {
        private final SteamID remote;
        private final SteamManager manager;

        public SteamConnection(SteamID remote, SteamManager manager) {
            this.remote = remote;
            this.manager = manager;
        }

        public void handlePacket(PacketByteBuf buf) {
            LOGGER.info("Received packet from " + remote + " (" + buf.readableBytes() + " bytes)");
            // TODO: Dispatch to Minecraft netcode
        }
    }
}