package steam_with_druzya;

import com.codedisaster.steamworks.*;
import com.codedisaster.steamworks.SteamMatchmaking.LobbyType;
import com.codedisaster.steamworks.SteamNetworking.P2PSend;
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
    private SteamNetworking networking;
    private SteamMatchmaking matchmaking;
    private SteamID lobbyID;  // ID текущего лобби (для хоста)
    private final Map<SteamID, SteamClientConnection> connections = new HashMap<>();
    private final ExecutorService pollExecutor = Executors.newSingleThreadExecutor();

    public SteamManager() {  // ← Здесь была ошибка: убрали "steamManager()"
        networking = new SteamNetworking(new SteamNetworkingCallback() {
            @Override
            public void onP2PSessionRequest(SteamID steamIDRemote) {
                LOGGER.info("P2P session request from: " + steamIDRemote);
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
                    LOGGER.info("Lobby created: " + lobbyID);
                } else {
                    LOGGER.error("Lobby creation failed: " + result);
                }
            }

            @Override
            public void onLobbyEntered(SteamID steamIDLobby, int chatPermissions, boolean blocked, SteamResult response)
             {
                LOGGER.info("Joined lobby: " + steamIDLobby + ", success: " + response);
                // Здесь можно сохранить lobbyID = steamIDLobby; если нужно
            }
        });
    }

    public void hostLobby(int maxPlayers) {
        matchmaking.createLobby(LobbyType.Public, maxPlayers);
    }

    public void joinLobby(long lobbySteamIDLong) {
        SteamID lobbySteamID = new SteamID().createFromNativeHandle(lobbySteamIDLong);           // Создаём пустой SteamID  // Устанавливаем значение (если метод существует)
        // Альтернатива, если setFromUInt64 нет: используй matchmaking.joinLobby с уже существующим SteamID из другого источника
        matchmaking.joinLobby(lobbySteamID);
    }

    public void sendPacket(SteamID remote, PacketByteBuf buf) throws SteamException {
        byte[] data = new byte[buf.readableBytes()];
        buf.readBytes(data);
        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        networking.sendP2PPacket(remote, byteBuffer, P2PSend.ReliableWithBuffering, 0);
    }

    public void startPolling() {
        pollExecutor.submit(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                int[] available = new int[1];
                // Правильный порядок: канал → массив available
                if (networking.isP2PPacketAvailable(0, available)) {
                    ByteBuffer data = ByteBuffer.allocate(available[0]);
                    SteamID remote = new SteamID();
                    int len = networking.readP2PPacket(remote, data, 0);
                    if (len > 0) {
                        data.position(0);
                        PacketByteBuf buf = new PacketByteBuf(Unpooled.wrappedBuffer(data.array(), 0, len));
                        SteamClientConnection conn = connections.get(remote);
                        if (conn != null) {
                            conn.handleIncoming(buf);
                        }
                    }
                }
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
    }
    

    private void createConnectionForPeer(SteamID remote) {
        SteamClientConnection conn = new SteamClientConnection(remote);
        connections.put(remote, conn);
    }

    public void shutdown() {
        pollExecutor.shutdownNow();
    }

    // Минимальная заглушка для SteamClientConnection (создай отдельный файл позже)
    public static class SteamClientConnection {
        private final SteamID remote;

        public SteamClientConnection(SteamID remote) {
            this.remote = remote;
        }

        public void handleIncoming(PacketByteBuf buf) {
            SteamManager.LOGGER.info("Received packet from " + remote + ", size: " + buf.readableBytes());
            // TODO: здесь будет разбор пакета и передача в Minecraft netcode
        }

        // Добавь позже: send, disconnect и т.д.
    }

    public SteamID getLobbyID() {
        return lobbyID;
    }
}