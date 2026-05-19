package com.flashminat0.selectivekeepinv;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppingEvent;

import java.io.File;

@Mod(
        modid = SelectiveKeepInv.MODID,
        name = SelectiveKeepInv.NAME,
        version = SelectiveKeepInv.VERSION,
        acceptableRemoteVersions = "*",
        serverSideOnly = false
)
public class SelectiveKeepInv {

    public static final String MODID   = "selectivekeepinv";
    public static final String NAME    = "Selective Keep Inventory";
    public static final String VERSION = "1.0";

    /** Directory where we save the VIP list (config/selectivekeepinv/). */
    public static File configDir;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        configDir = new File(event.getModConfigurationDirectory(), MODID);
        if (!configDir.exists()) configDir.mkdirs();

        PlayerList.load();
        MinecraftForge.EVENT_BUS.register(new EventHandler());
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new CommandKeepInv());
    }

    @Mod.EventHandler
    public void serverStopping(FMLServerStoppingEvent event) {
        PlayerList.save();
    }
}
