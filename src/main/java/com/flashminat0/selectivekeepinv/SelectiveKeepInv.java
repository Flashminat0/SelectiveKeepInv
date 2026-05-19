package com.flashminat0.selectivekeepinv;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppingEvent;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
    public static final String VERSION = "2.3";

    /** Directory where we save the VIP list and config.yml. */
    public static File configDir;
    /** Loaded runtime configuration. Populated in preInit, never null after that. */
    public static Config config = Config.defaults();
    /** Active message pool. Defaults until override-corny-msgs is enabled. */
    public static DeathMessageStore deathMessages = DeathMessageStore.defaults();

    /**
     * Warnings raised during config / death-msgs load. Broadcast to op
     * players on login so the admin sees them in chat. Console gets a
     * stack trace / WARN line on each entry too.
     */
    private static final List<String> startupErrors = new ArrayList<>();

    /** Adds a startup error and prints it to System.err with our prefix. */
    public static void reportStartupError(String message) {
        System.err.println("[SelectiveKeepInv] WARN: " + message);
        synchronized (startupErrors) {
            startupErrors.add(message);
        }
    }

    /** Snapshot of pending op-visible warnings. Defensive copy. */
    public static List<String> snapshotStartupErrors() {
        synchronized (startupErrors) {
            return Collections.unmodifiableList(new ArrayList<>(startupErrors));
        }
    }

    /**
     * Drain the queue. Package-private; only tests need to reset between
     * cases. Runtime code never calls this (the queue is read-only after
     * preInit).
     */
    static void clearStartupErrorsForTesting() {
        synchronized (startupErrors) {
            startupErrors.clear();
        }
    }

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        configDir = new File(event.getModConfigurationDirectory(), MODID);
        if (!configDir.exists()) configDir.mkdirs();

        config = Config.load(configDir);

        // Death-messages override: only touch the file when the flag is on.
        if (config.overrideCornyMsgs) {
            File msgsFile = new File(configDir, "death-msgs.yml");
            if (!msgsFile.exists()) {
                try {
                    DeathMessageStore.writeDefaultsToFile(msgsFile);
                } catch (Exception e) {
                    reportStartupError("Could not write death-msgs.yml: " + e.getMessage()
                            + ". Falling back to built-in messages.");
                    config.overrideCornyMsgs = false;
                }
            }
            if (config.overrideCornyMsgs) {
                try {
                    deathMessages = DeathMessageStore.fromFile(msgsFile);
                } catch (DeathMessageStore.InvalidDeathMessageFile e) {
                    reportStartupError("death-msgs.yml is invalid: " + e.getMessage()
                            + ". Using built-in messages instead. Fix the file and restart"
                            + " to enable custom messages.");
                    config.overrideCornyMsgs = false;
                    deathMessages = DeathMessageStore.defaults();
                }
            }
        }

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
