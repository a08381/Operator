package cn.windmourn.operator.listeners;

import cn.windmourn.operator.utils.ReflectUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiDisconnected;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.multiplayer.GuiConnecting;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class PlayerDisconnect {

    private Minecraft mc = Minecraft.getMinecraft();

    @SubscribeEvent
    public void onDisconnect(GuiOpenEvent event) {
        if (event.getGui() instanceof GuiDisconnected) {
            GuiScreen preGui = ReflectUtil.getOfT(event.getGui(), GuiScreen.class);
            ServerData data = mc.getCurrentServerData();
            if (data != null) {
                event.setGui(new GuiConnecting(preGui, mc, mc.getCurrentServerData()));
            }
        }
    }

}
