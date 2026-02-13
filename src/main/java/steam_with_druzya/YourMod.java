package steam_with_druzya;

import com.codedisaster.steamworks.*;
import com.codedisaster.steamworks.SteamNetworking.P2PSessionError;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback; // Updated to v2
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.function.Supplier;

public class YourMod implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("steam_with_druzya");
    private SteamNetworking networking;

    @Override
    public void onInitialize() {
        try {
            // Загрузка нативов для LWJGL3 (Minecraft)
            SteamLibraryLoaderLwjgl3 loader = new SteamLibraryLoaderLwjgl3();
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

            // Теперь networking готов - инициализируй SteamManager
            SteamManager.INSTANCE.init(networking, null);  // server set later via mixin

            LOGGER.info("Steam P2P initialized successfully!");

        } catch (SteamException e) {
            LOGGER.error("Steam init exception: ", e);
        }

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("hostlobby")
                .executes(ctx -> {
                    SteamManager.INSTANCE.hostLobby(8);  // 8 слотов
                    ctx.getSource().sendFeedback(() -> Text.literal("Steam lobby created! ID: " + SteamManager.INSTANCE.getLobbyID()), false);
                    return 1;
                })
                .then(CommandManager.argument("slots", IntegerArgumentType.integer(2, 32))
                    .executes(ctx -> {
                        int slots = IntegerArgumentType.getInteger(ctx, "slots");
                        SteamManager.INSTANCE.hostLobby(slots);
                        ctx.getSource().sendFeedback(() -> Text.literal("Steam lobby created! ID: " + SteamManager.INSTANCE.getLobbyID()), false);
                        return 1;
                    })));
        });
    }
}