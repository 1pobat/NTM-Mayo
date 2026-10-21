package dev.hbmmayo.neoforge;

import com.hbm.registration.NeoForgeRegistrar;
import dev.hbmmayo.core.ConfigFile;
import dev.hbmmayo.core.MayoConfig;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Mod(HbmMayonnaise.ID)
public final class HbmMayonnaise {
    public static final String ID = "hbmmayo";
    public static MayoConfig config = MayoConfig.defaults();
    public static int hudX = 8, hudY = 8;
    public HbmMayonnaise(IEventBus bus) {
        loadConfig();
        NeoForgeRegistrar.addon(ID, bus, MayoRegistry::register);
        bus.addListener(MayoPayload::register);
        NeoForge.EVENT_BUS.addListener(MayoRuntime::levelTick);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, false, MayoRuntime::death);
        NeoForge.EVENT_BUS.addListener(MayoRuntime::login);
        NeoForge.EVENT_BUS.addListener(MayoRuntime::respawn);
        NeoForge.EVENT_BUS.addListener(MayoRuntime::dimension);
    }
    private static void loadConfig() {
        Path file = FMLPaths.CONFIGDIR.get().resolve("hbmmayo.properties");
        try {
            if (!Files.exists(file)) {
                Files.createDirectories(file.getParent());
                try (var in=HbmMayonnaise.class.getResourceAsStream("/hbmmayo-default.properties")) {
                    if(in==null) throw new IOException("Missing bundled defaults");
                    Files.copy(in,file);
                }
            }
            var loaded=ConfigFile.load(file);
            config=loaded.server(); hudX=loaded.hudX(); hudY=loaded.hudY();
        } catch(IOException e) { throw new IllegalStateException("Cannot load "+file,e); }
    }
}
