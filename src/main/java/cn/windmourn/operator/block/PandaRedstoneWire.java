package cn.windmourn.operator.block;

import cn.windmourn.operator.utils.ReflectUtil;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import net.minecraft.block.*;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import org.apache.commons.lang3.ArrayUtils;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

/*
 * @author Panda4994
 */

public class PandaRedstoneWire extends BlockRedstoneWire {

    /*
     * 作者说用LinkedHashSet替代arraylist没有明显的性能提示，红石设备不多的时候的确如此
     * 但是实测在红石设备数量很大的时候，有2~5%的性能提升（基于PaperSpigot1.10.2测试），所以还是改用LinkedHashSet来实现
     */
    // 需要被断路的红石位置
    private Set<BlockPos> turnOff = Sets.newLinkedHashSet();
    // 需要被激活的红石位置
    private Set<BlockPos> turnOn = Sets.newLinkedHashSet();
    private final Set<BlockPos> updatedRedstoneWire = Sets.newLinkedHashSet();
    private static final EnumFacing[] facingsHorizontal = {EnumFacing.WEST, EnumFacing.EAST, EnumFacing.NORTH, EnumFacing.SOUTH};
    private static final EnumFacing[] facingsVertical = {EnumFacing.DOWN, EnumFacing.UP};
    private static final EnumFacing[] facings = (EnumFacing[]) ArrayUtils.addAll(facingsVertical, facingsHorizontal);
    private static final Vec3i[] surroundingBlocksOffset;

    static {
        Set<Vec3i> set = Sets.newLinkedHashSet();
        for (EnumFacing facing : facings) {
            set.add(ReflectUtil.getOfT(facing, Vec3i.class));
        }
        for (EnumFacing facing1 : facings) {
            Vec3i v1 = ReflectUtil.getOfT(facing1, Vec3i.class);
            for (EnumFacing facing2 : facings) {
                Vec3i v2 = ReflectUtil.getOfT(facing2, Vec3i.class);

                set.add(new BlockPos(v1.getX() + v2.getX(), v1.getY() + v2.getY(), v1.getZ() + v2.getZ()));
            }
        }
        set.remove(BlockPos.ORIGIN);
        surroundingBlocksOffset = set.toArray(new Vec3i[set.size()]);
    }

    public PandaRedstoneWire() {
        this.setHardness(0.0F);
        this.setSoundType(SoundType.STONE);
        this.setUnlocalizedName("redstoneDust");
        this.disableStats();
    }

    private boolean g = true;

    private void updateSurroundingRedstone(World worldIn, BlockPos pos, IBlockState state) {
        calculateCurrentChanges(worldIn, pos);
        Set<BlockPos> blocksNeedingUpdate = Sets.newLinkedHashSet();

        Iterator<BlockPos> iterator = this.updatedRedstoneWire.iterator();
        while (iterator.hasNext()) {
            addBlocksNeedingUpdate(worldIn, iterator.next(), blocksNeedingUpdate);
        }

        Iterator<BlockPos> blockPositionIterator = Lists.newLinkedList(this.updatedRedstoneWire).descendingIterator();
        while (blockPositionIterator.hasNext()) {
            this.addAllSurroundingBlocks(blockPositionIterator.next(), blocksNeedingUpdate);
        }

        blocksNeedingUpdate.removeAll(this.updatedRedstoneWire);
        this.updatedRedstoneWire.clear();

        for (BlockPos posi : blocksNeedingUpdate) {
            worldIn.neighborChanged(posi, this, pos);
        }
    }

    private void calculateCurrentChanges(World worldIn, BlockPos pos) {
        if (worldIn.getBlockState(pos).getBlock() == this) {
            this.turnOff.add(pos);
        } else {
            checkSurroundingWires(worldIn, pos);
        }

        while (!this.turnOff.isEmpty()) {
            Iterator<BlockPos> iter = this.turnOff.iterator();
            final BlockPos pos1 = iter.next();
            iter.remove();
            IBlockState state = worldIn.getBlockState(pos1);
            int oldPower = state.getValue(POWER);
            this.g = false;
            int blockPower = worldIn.isBlockIndirectlyGettingPowered(pos1);
            this.g = true;
            int wirePower = getSurroundingWirePower(worldIn, pos1);

            wirePower--;
            int newPower = Math.max(blockPower, wirePower);

            /*
            if (oldPower != newPower) {
                BlockRedstoneEvent event = new BlockRedstoneEvent(world.getWorld().getBlockAt(pos.getX(), pos.getY(), pos.getZ()), oldPower, newPower);
                world.getServer().getPluginManager().callEvent(event);

                newPower = event.getNewCurrent();
            }
            */

            if (newPower < oldPower) {
                if ((blockPower > 0) && (!this.turnOn.contains(pos1))) {
                    this.turnOn.add(pos1);
                }
                state = setWireState(worldIn, pos1, state, 0);
            } else if (newPower > oldPower) {
                state = setWireState(worldIn, pos1, state, newPower);
            }
            checkSurroundingWires(worldIn, pos1);
        }

        while (!this.turnOn.isEmpty()) {
            Iterator<BlockPos> iter = this.turnOn.iterator();
            final BlockPos pos1 = iter.next();
            iter.remove();
            IBlockState state = worldIn.getBlockState(pos1);
            int oldPower = state.getValue(POWER);
            this.g = false;
            int blockPower = worldIn.isBlockIndirectlyGettingPowered(pos1);
            this.g = true;
            int wirePower = getSurroundingWirePower(worldIn, pos1);

            wirePower--;
            int newPower = Math.max(blockPower, wirePower);

            /*
            if (oldPower != newPower) {
                BlockRedstoneEvent event = new BlockRedstoneEvent(world.getWorld().getBlockAt(pos.getX(), pos.getY(), pos.getZ()), oldPower, newPower);
                world.getServer().getPluginManager().callEvent(event);

                newPower = event.getNewCurrent();
            }
            */

            if (newPower > oldPower) {
                state = setWireState(worldIn, pos1, state, newPower);
            } else if (newPower >= oldPower) {
            }
            checkSurroundingWires(worldIn, pos1);
        }
        this.turnOff.clear();
        this.turnOn.clear();
    }

    private void addWireToList(World worldIn, BlockPos pos, int otherPower) {
        IBlockState state = worldIn.getBlockState(pos);
        if (state.getBlock() == this) {
            int power = state.getValue(POWER);
            if (power < otherPower - 1 && !this.turnOn.contains(pos)) {
                this.turnOn.add(pos);
            }

            if (power > otherPower && !this.turnOff.contains(pos)) {
                this.turnOff.add(pos);
            }
        }

    }

    private void checkSurroundingWires(World worldIn, BlockPos pos) {
        IBlockState state = worldIn.getBlockState(pos);
        int ownPower = 0;
        if (state.getBlock() == this) {
            ownPower = state.getValue(POWER);
        }

        for (EnumFacing facing : facingsHorizontal) {
            BlockPos offsetPos = pos.offset(facing);
            if (facing.getAxis().isHorizontal()) {
                addWireToList(worldIn, offsetPos, ownPower);
            }
        }

        for (EnumFacing facingVertical : facingsVertical) {
            BlockPos offsetPos = pos.offset(facingVertical);
            boolean solidBlock = worldIn.getBlockState(offsetPos).isBlockNormalCube();
            for (EnumFacing facingHorizontal : facingsHorizontal) {
                if (((facingVertical == EnumFacing.UP) && (!solidBlock)) || ((facingVertical == EnumFacing.DOWN) && (solidBlock) && (!worldIn.getBlockState(offsetPos.offset(facingHorizontal)).isBlockNormalCube()))) {
                    addWireToList(worldIn, offsetPos.offset(facingHorizontal), ownPower);
                }
            }
        }
    }

    private int getSurroundingWirePower(World worldIn, BlockPos pos) {
        int wirePower = 0;

        for (EnumFacing enumfacing : EnumFacing.Plane.HORIZONTAL) {
            BlockPos offsetPos = pos.offset(enumfacing);
            wirePower = getMaxCurrentStrength(worldIn, offsetPos, wirePower);
            if (worldIn.getBlockState(offsetPos).isNormalCube() && !worldIn.getBlockState(pos.up()).isNormalCube()) {
                wirePower = getMaxCurrentStrength(worldIn, offsetPos.up(), wirePower);
            } else if (!worldIn.getBlockState(offsetPos).isNormalCube()) {
                wirePower = getMaxCurrentStrength(worldIn, offsetPos.down(), wirePower);
            }
        }

        return wirePower;
    }

    private void addBlocksNeedingUpdate(World worldIn, BlockPos pos, Set<BlockPos> set) {
        List<EnumFacing> connectedSides = getSidesToPower(worldIn, pos);

        for (EnumFacing facing : facings) {
            BlockPos offsetPos = pos.offset(facing);
            if ((connectedSides.contains(facing.getOpposite()) || facing == EnumFacing.DOWN || (facing.getAxis().isHorizontal() && canConnectTo(worldIn.getBlockState(offsetPos), facing, worldIn, offsetPos))) && canBlockBePoweredFromSide(worldIn.getBlockState(offsetPos), facing, true)) {
                set.add(offsetPos);
            }
        }

        for (EnumFacing facing : facings) {
            BlockPos offsetPos = pos.offset(facing);
            if ((connectedSides.contains(facing.getOpposite()) || facing == EnumFacing.DOWN) && worldIn.getBlockState(offsetPos).isBlockNormalCube()) {
                for (EnumFacing facing1 : facings) {
                    if (canBlockBePoweredFromSide(worldIn.getBlockState(offsetPos.offset(facing1)), facing1, false)) {
                        set.add(offsetPos.offset(facing1));
                    }
                }
            }
        }
    }

    private boolean canBlockBePoweredFromSide(IBlockState state, EnumFacing side, boolean isWire) {
        if (state.getBlock() instanceof BlockPistonBase && state.getValue(BlockPistonBase.FACING) == side.getOpposite()) {
            return false;
        } else if (state.getBlock() instanceof BlockRedstoneDiode && state.getValue(BlockRedstoneDiode.FACING) != side.getOpposite()) {
            return isWire && state.getBlock() instanceof BlockRedstoneComparator && state.getValue(BlockRedstoneComparator.FACING).getAxis() != side.getAxis() && side.getAxis().isHorizontal();
        } else {
            return !(state.getBlock() instanceof BlockRedstoneTorch) || !isWire && state.getValue(BlockRedstoneTorch.FACING) == side;
        }
    }

    private List<EnumFacing> getSidesToPower(World worldIn, BlockPos pos) {
        List<EnumFacing> retval = Lists.newArrayList();

        for (EnumFacing facing : facingsHorizontal) {
            if (isPowerSourceAt(worldIn, pos, facing)) {
                retval.add(facing);
            }
        }

        if (retval.isEmpty()) {
            return Lists.newArrayList(facingsHorizontal);
        } else {
            boolean northsouth = retval.contains(EnumFacing.NORTH) || retval.contains(EnumFacing.SOUTH);
            boolean eastwest = retval.contains(EnumFacing.EAST) || retval.contains(EnumFacing.WEST);
            if (northsouth) {
                retval.remove(EnumFacing.EAST);
                retval.remove(EnumFacing.WEST);
            }

            if (eastwest) {
                retval.remove(EnumFacing.NORTH);
                retval.remove(EnumFacing.SOUTH);
            }

            return retval;
        }
    }

    private void addAllSurroundingBlocks(BlockPos pos, Set<BlockPos> set) {
        for (Vec3i vect : surroundingBlocksOffset) {
            set.add(pos.add(vect));
        }
    }

    private IBlockState setWireState(World worldIn, BlockPos pos, IBlockState state, int power) {
        state = state.withProperty(POWER, power);
        worldIn.setBlockState(pos, state, 2);
        this.updatedRedstoneWire.add(pos);
        return state;
    }

    private int getMaxCurrentStrength(World worldIn, BlockPos pos, int strength) {
        if (worldIn.getBlockState(pos).getBlock() != this) {
            return strength;
        } else {
            int i = worldIn.getBlockState(pos).getValue(POWER);
            return i > strength ? i : strength;
        }
    }

    private void notifyWireNeighborsOfStateChange(World worldIn, BlockPos pos) {
        if (worldIn.getBlockState(pos).getBlock() == this) {
            worldIn.notifyNeighborsOfStateChange(pos, this, false);

            for (EnumFacing enumfacing : EnumFacing.values()) {
                worldIn.notifyNeighborsOfStateChange(pos.offset(enumfacing), this, false);
            }
        }
    }

    @Override
    public void onBlockAdded(World worldIn, BlockPos pos, IBlockState state) {
        if (!worldIn.isRemote) {
            updateSurroundingRedstone(worldIn, pos, state);

            for (EnumFacing facing : EnumFacing.Plane.VERTICAL) {
                worldIn.notifyNeighborsOfStateChange(pos.offset(facing), this, false);
            }

            for (EnumFacing facing : EnumFacing.Plane.HORIZONTAL) {
                notifyWireNeighborsOfStateChange(worldIn, pos.offset(facing));
            }

            for (EnumFacing facing : EnumFacing.Plane.HORIZONTAL) {
                BlockPos pos1 = pos.offset(facing);
                if (worldIn.getBlockState(pos1).isNormalCube()) {
                    notifyWireNeighborsOfStateChange(worldIn, pos1.up());
                } else {
                    notifyWireNeighborsOfStateChange(worldIn, pos1.down());
                }
            }
        }
    }

    @Override
    public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
        super.breakBlock(worldIn, pos, state);
        if (!worldIn.isRemote) {
            EnumFacing[] facings = EnumFacing.values();
            for (EnumFacing facing : facings) {
                worldIn.notifyNeighborsOfStateChange(pos.offset(facing), this, false);
            }

            updateSurroundingRedstone(worldIn, pos, state);
            for (EnumFacing facing : EnumFacing.Plane.HORIZONTAL) {
                notifyWireNeighborsOfStateChange(worldIn, pos.offset(facing));
            }

            for (EnumFacing facing : EnumFacing.Plane.HORIZONTAL) {
                BlockPos pos1 = pos.offset(facing);
                if (worldIn.getBlockState(pos1).isNormalCube()) {
                    notifyWireNeighborsOfStateChange(worldIn, pos1.up());
                } else {
                    notifyWireNeighborsOfStateChange(worldIn, pos1.down());
                }
            }
        }

    }

    @Override
    public void neighborChanged(IBlockState state, World worldIn, BlockPos pos, Block block, BlockPos pos1) {
        if (!worldIn.isRemote) {
            if (canPlaceBlockAt(worldIn, pos)) {
                updateSurroundingRedstone(worldIn, pos, state);
            } else {
                dropBlockAsItem(worldIn, pos, state, 0);
                worldIn.setBlockToAir(pos);
            }
        }

    }

    @Override
    public int getWeakPower(IBlockState state, IBlockAccess blockAccess, BlockPos pos, EnumFacing side) {
        if (!this.g) {
            return 0;
        } else {
            int i = state.getValue(POWER);
            if (i == 0) {
                return 0;
            } else if (side == EnumFacing.UP) {
                return i;
            } else {
                return getSidesToPower((World) blockAccess, pos).contains(side) ? i : 0;
            }
        }
    }

    private boolean isPowerSourceAt(IBlockAccess blockAccess, BlockPos pos, EnumFacing facing) {
        BlockPos pos1 = pos.offset(facing);
        IBlockState state = blockAccess.getBlockState(pos1);
        boolean flag = state.isBlockNormalCube();
        boolean flag1 = blockAccess.getBlockState(pos.up()).isBlockNormalCube();
        return !flag1 && flag && canConnectUpwardsTo(blockAccess, pos1.up()) || (canConnectTo(state, facing, blockAccess, pos) || (state.getBlock() == Blocks.POWERED_REPEATER && state.getValue(BlockRedstoneDiode.FACING) == facing || !flag && canConnectUpwardsTo(blockAccess, pos1.down())));
    }

}
