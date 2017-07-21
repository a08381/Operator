package cn.windmourn.operator;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.ExplosionEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;

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

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new CommandOp());
        event.registerServerCommand(new CommandDeOp());
        event.registerServerCommand(new CommandBan());
        event.registerServerCommand(new CommandBanIP());
        event.registerServerCommand(new CommandUnBan());
        event.registerServerCommand(new CommandUnBanIP());
        event.registerServerCommand(new CommandVanish());
        event.registerServerCommand(new CommandFly());
    }

    @SubscribeEvent
    public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.player instanceof EntityPlayerMP) {
            MinecraftServer server = event.player.getServer();
            if (event.player.getName().equals(server.getServerOwner()))
                server.getPlayerList().addOp(event.player.getGameProfile());
        }
    }

    @SubscribeEvent
    public void onExplosion(ExplosionEvent.Detonate event) {
        event.getExplosion().clearAffectedBlockPositions();
    }

}
