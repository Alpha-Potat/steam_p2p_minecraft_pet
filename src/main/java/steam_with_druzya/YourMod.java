package steam_with_druzya;

import com.codedisaster.steamworks.*;
import com.codedisaster.steamworks.SteamNetworking.P2PSessionError;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class YourMod implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("steam_with_druzya");
    private SteamNetworking networking;

    @Override
    public void onInitialize() {
        SteamManager.INSTANCE.init(networking, null);  // server set later via mixin
        try {
            // Загрузка нативов для LWJGL3 (Minecraft)
            SteamLibraryLoaderLwjgl3 loader = new SteamLibraryLoaderLwjgl3();
            // Опционально: loader.setLibraryPath("путь_к_бинарникам"); — не нужно, auto
            if (!SteamAPI.loadLibraries(loader)) {
                LOGGER.error("Failed to load Steam native libraries!");
                return;
            }

            if (!SteamAPI.init()) {
                LOGGER.error("Failed to initialize Steam API! (Steam not running? Check steam_appid.txt)");
                return;
            }

            // Инициализация Networking с коллбеками
            networking = new SteamNetworking(new SteamNetworkingCallback() {
                @Override
                public void onP2PSessionRequest(SteamID steamIDRemote) {
                    LOGGER.info("P2P session request from: " + steamIDRemote);
                    networking.acceptP2PSessionWithUser(steamIDRemote);  // Accept
                }

                @Override
                public void onP2PSessionConnectFail(SteamID steamIDRemote, P2PSessionError sessionError) {
                    LOGGER.error("P2P connect fail to {}: {}", steamIDRemote, sessionError);
                }
                // Добавь другие @Override если нужно (onP2PConnectionFailWithUser и т.д.)
            });

            LOGGER.info("Steam P2P initialized successfully!");
            // Здесь: SteamManager или lobby init

        } catch (SteamException e) {
            LOGGER.error("Steam init exception: ", e);
        }
    }
}