package steam_with_druzya.mixin;

import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public class ExampleMixin {
    @Inject(method = "loadWorld()V", at = @At("RETURN"))  // Use "loadWorld" here
    private void init(CallbackInfo info) {
        // Your code, e.g., SteamManager.INSTANCE.hostLobby(8);
    }
}