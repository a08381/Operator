package cn.windmourn.operator;

import cn.windmourn.operator.commands.CommandsManager;
import cn.windmourn.operator.listeners.EntityExplosion;
import cn.windmourn.operator.listeners.ExplosionBreak;
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
        if (getConfig().isNeverExplosion()) MinecraftForge.EVENT_BUS.register(new EntityExplosion());
        if (getConfig().isExplosionNoBreak()) MinecraftForge.EVENT_BUS.register(new ExplosionBreak());
        if (getConfig().isOpsOwnerOnStart()) MinecraftForge.EVENT_BUS.register(new PlayerLogin());
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        if (event.getServer().isSinglePlayer()) CommandsManager.registerServerCommands(event);
        if (getConfig().isRegisterExtraCommands()) CommandsManager.registerExtraCommands(event);

        // add(55, "redstone_wire", new PandaRedstoneWire());
        // ReflectUtil.setStatic("REDSTONE_WIRE", Blocks.class, get("redstone_wire"));
    }

    /*

    @Mod.EventHandler
    public void serverStarted(FMLServerStartedEvent event) {
        getLogger().info(Blocks.REDSTONE_WIRE.getClass().getName());
    }

    */

    public ConfigManager getConfig() {
        return config;
    }

    public Logger getLogger() {
        return logger;
    }

    /*

    private static Block get(String s) {
        return Block.REGISTRY.getObject(new ResourceLocation(s));
    }

    @SuppressWarnings("deprecation")
    private static void add(int i, String s, Block block) {
        try {
            ResourceLocation rl = new ResourceLocation(s);
            Class clzRegistry = Class.forName("net.minecraftforge.fml.common.registry.PersistentRegistryManager$PersistentRegistry");
            Object eRegistry = Enum.valueOf(clzRegistry, "ACTIVE");
            FMLControlledNamespacedRegistry registry = ReflectUtil.invoke(eRegistry, "getRegistry", FMLControlledNamespacedRegistry.class, rl, null);
            ReflectUtil.invoke(registry, "addObjectRaw", i, rl, block);
        } catch (ReflectiveOperationException e) {
            e.printStackTrace();
        }
        for (IBlockState state : block.getBlockState().getValidStates()) {
            int k = Block.REGISTRY.getIDForObject(block) << 4 | block.getMetaFromState(state);

            Block.BLOCK_STATE_IDS.put(state, k);
        }
    }

    */

}
