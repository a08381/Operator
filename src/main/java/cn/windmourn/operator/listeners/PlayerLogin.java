package cn.windmourn.operator.listeners;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;

public class PlayerLogin {

    @SubscribeEvent
    public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.player instanceof EntityPlayerMP) {
            MinecraftServer server = event.player.getServer();
            if (event.player.getName().equals(server.getServerOwner()))
                server.getPlayerList().addOp(event.player.getGameProfile());
        }
    }

}
