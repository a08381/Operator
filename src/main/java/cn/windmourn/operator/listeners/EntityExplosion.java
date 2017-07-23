package cn.windmourn.operator.listeners;

import net.minecraftforge.event.world.ExplosionEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class EntityExplosion {

    @SubscribeEvent
    public void onExplosion(ExplosionEvent.Start event) {
        event.setCanceled(true);
    }

}
