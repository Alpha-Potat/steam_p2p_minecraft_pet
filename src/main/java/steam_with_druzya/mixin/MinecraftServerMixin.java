package steam_with_druzya.mixin;

import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import steam_with_druzya.SteamManager;

@Mixin(MinecraftServer.class)
public class MinecraftServerMixin {
    @Inject(method = "loadWorld", at = @At("RETURN"))  // Yarn 1.20.1: loadWorld()
    private void onLoadWorld(CallbackInfo ci) {
        SteamManager.INSTANCE.hostLobby(8);
        SteamManager.LOGGER.info("Auto-hosted Steam lobby on server start!");
    }
}