package cn.windmourn.operator.listeners;

import cn.windmourn.operator.events.FarmlandChangeEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class FarmlandChange {

    @SubscribeEvent
    public void onFarmlandChange(FarmlandChangeEvent event) {
        if (event.getType().equals(FarmlandChangeEvent.Type.FALLEN)) event.setCanceled(true);
    }

}
