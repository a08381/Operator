package cn.windmourn.operator.events;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.eventhandler.Cancelable;

import javax.annotation.Nullable;

@Cancelable
public class FarmlandChangeEvent extends BlockEvent {

    private Type type;

    public FarmlandChangeEvent(World world, BlockPos pos, IBlockState state, @Nullable Type type) {
        super(world, pos, state);
        this.type = type == null ? Type.NATRUAL : type;
    }

    public Type getType() {
        return type;
    }

    public enum Type {
        FALLEN, NATRUAL
    }

}
