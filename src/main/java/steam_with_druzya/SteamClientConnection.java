package steam_with_druzya;

import com.codedisaster.steamworks.SteamID;
import net.minecraft.network.PacketByteBuf;

public class SteamClientConnection {
    private final SteamID remote;

    public SteamClientConnection(SteamID remote) {
        this.remote = remote;
    }

    public void handleIncoming(PacketByteBuf buf) {
        SteamManager.LOGGER.info("Received packet from " + remote + ", size: " + buf.readableBytes());
        // Здесь будет логика обработки входящих Minecraft-пакетов
    }

    public void send(PacketByteBuf buf) {
        // Пока пусто — реализуем позже через SteamManager
    }
}