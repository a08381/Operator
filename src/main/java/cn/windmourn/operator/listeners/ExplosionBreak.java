package cn.windmourn.operator.listeners;

import net.minecraftforge.event.world.ExplosionEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class ExplosionBreak {

    @SubscribeEvent
    public void onExplosion(ExplosionEvent.Detonate event) {
        event.getExplosion().clearAffectedBlockPositions();
    }

}
