package cn.windmourn.operator;

import cn.windmourn.operator.commands.CommandsManager;
import cn.windmourn.operator.listeners.ExplosionBreak;
import cn.windmourn.operator.listeners.FarmlandChange;
import cn.windmourn.operator.listeners.PlayerDisconnect;
import cn.windmourn.operator.listeners.PlayerLogin;
import cn.windmourn.operator.utils.ConfigManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import org.apache.logging.log4j.Logger;

/**
 * Created by lenovo on 2017/7/17.
 */
@Mod(name = Operator.NAME, modid = Operator.MODID, version = Operator.VERSION, acceptableRemoteVersions = "*")
public class Operator {

    @Mod.Instance
    public static Operator INSTANCE;

    public static final String NAME = "Operator";
    public static final String MODID = "operator";
    public static final String VERSION = "1.0";

    private Logger logger;
    private ConfigManager config;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        logger = event.getModLog();
        config = ConfigManager.loadConfig(event.getSuggestedConfigurationFile());
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (getConfig().isCreeperHealMode()) MinecraftForge.EVENT_BUS.register(new ExplosionBreak());
        if (getConfig().isOpsOwnerOnStart()) MinecraftForge.EVENT_BUS.register(new PlayerLogin());
        if (getConfig().isNoFallenOnFarmland()) MinecraftForge.EVENT_BUS.register(new FarmlandChange());
        MinecraftForge.EVENT_BUS.register(new PlayerDisconnect());
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        if (event.getServer().isSinglePlayer()) CommandsManager.registerServerCommands(event);
        if (getConfig().isRegisterExtraCommands()) CommandsManager.registerExtraCommands(event);
    }

    public ConfigManager getConfig() {
        return config;
    }

    public Logger getLogger() {
        return logger;
    }

}
