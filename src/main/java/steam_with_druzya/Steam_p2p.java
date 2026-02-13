package steam_with_druzya;

import com.codedisaster.steamworks.*;

public class YourMod implements ModInitializer {
    private SteamAPI steamAPI;
    private SteamNetworking networking;

    @Override
    public void onInitialize() {
        if (SteamAPI.loadLibraries()) {
            if (SteamAPI.init()) {
                networking = new SteamNetworking(new SteamNetworkingCallback() {
                    // Реализуйте коллбеки для P2P: onP2PSessionRequest, onP2PSessionConnectFail и т.д.
                });
                // Здесь инициализируйте P2P-сессии
            } else {
                // Ошибка инициализации
            }
        }
    }
}