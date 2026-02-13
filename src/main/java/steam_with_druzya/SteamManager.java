package steam_with_druzya;

import com.codedisaster.steamworks.*;
import com.codedisaster.steamworks.SteamMatchmaking.LobbyType;
import com.codedisaster.steamworks.SteamNetworking.P2PSessionError;
import io.netty.buffer.Unpooled;
import net.minecraft.network.PacketByteBuf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SteamManager {
    public static final Logger LOGGER = LoggerFactory.getLogger("steam_with_druzya");
    public static final SteamManager INSTANCE = new SteamManager();
    private SteamNetworking networking;
    private SteamMatchmaking matchmaking;
    private SteamID lobbyID;
    private final Map<SteamID, SteamClientConnection> connections = new HashMap<>();
    private final ExecutorService pollExecutor = Executors.newSingleThreadExecutor();

    public SteamManager() {
        networking = new SteamNetworking(new SteamNetworkingCallback() {
            @Override
            public void onP2PSessionRequest(SteamID steamIDRemote) {
                LOGGER.info("P2P session request from: {}", steamIDRemote);
                networking.acceptP2PSessionWithUser(steamIDRemote);
                createConnectionForPeer(steamIDRemote);
            }

            @Override
            public void onP2PSessionConnectFail(SteamID steamIDRemote, P2PSessionError sessionError) {
                LOGGER.error("P2P connect fail to {}: {}", steamIDRemote, sessionError);
            }
        });

        matchmaking = new SteamMatchmaking(new SteamMatchmakingCallback() {
            @Override
            public void onLobbyCreated(SteamResult result, SteamID steamIDLobby) {
                if (result == SteamResult.OK) {
                    lobbyID = steamIDLobby;
                    LOGGER.info("Lobby created: {}", lobbyID);
                } else {
                    LOGGER.error("Lobby creation failed: {}", result);
                }
            }

            
        });
        
    }
    public void init(SteamNetworking networking, Object server) {  // Object server - заглушка для mixin
        this.networking = networking;
        // Здесь добавьте логику, если нужно (e.g., startPolling() или lobby init)
        LOGGER.info("SteamManager initialized with networking");
        this.startPolling();  // Запустите поллинг, если нужно
    }
    

    public void hostLobby(int maxPlayers) {
        matchmaking.createLobby(LobbyType.Public, maxPlayers);  // No exception thrown, removed try-catch
    }

    public void joinLobby(long lobbyIDLong) {
        SteamID lobby = SteamID.createFromNativeHandle(lobbyIDLong);
        matchmaking.joinLobby(lobby);  // No exception thrown, removed try-catch
    }

    public void sendPacket(SteamID remote, PacketByteBuf buf) {
        byte[] data = new byte[buf.readableBytes()];
        buf.readBytes(data);
        ByteBuffer bb = ByteBuffer.wrap(data);
        try {
            networking.sendP2PPacket(remote, bb, SteamNetworking.P2PSend.Reliable, 0);
        } catch (SteamException e) {
            LOGGER.error("Failed to send P2P packet", e);
        }
    }

    public void startPolling() {
        pollExecutor.submit(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                int[] msgSize = new int[1];
                if (networking.isP2PPacketAvailable(0, msgSize)) {
                    ByteBuffer data = ByteBuffer.allocate(msgSize[0]);
                    SteamID remote = new SteamID();
                    try {
                        int bytesRead = networking.readP2PPacket(remote, data, 0);
                        if (bytesRead > 0) {
                            data.flip();
                            PacketByteBuf buf = new PacketByteBuf(Unpooled.wrappedBuffer(data));
                            SteamClientConnection conn = connections.get(remote);
                            if (conn != null) {
                                conn.handleIncoming(buf);
                            }
                        }
                    } catch (SteamException e) {
                        LOGGER.error("P2P read error", e);
                    }
                }
                try {
                    Thread.sleep(1);
                } catch (InterruptedException ignored) {}
            }
        });
    }

    private void createConnectionForPeer(SteamID remote) {
        connections.put(remote, new SteamClientConnection(remote));
    }

    public SteamID getLobbyID() {
        return lobbyID;
    }

    public void shutdown() {
        pollExecutor.shutdownNow();
    }
}