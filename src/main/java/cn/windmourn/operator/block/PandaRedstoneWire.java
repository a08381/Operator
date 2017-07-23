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

public class PandaRedstoneWire extends BlockRedstoneWire {

    private List<BlockPos> turnOff = Lists.newArrayList();
    private List<BlockPos> turnOn = Lists.newArrayList();
    private final Set<BlockPos> updatedRedstoneWire = Sets.newLinkedHashSet();
    private static final EnumFacing[] facingsHorizontal;
    private static final EnumFacing[] facingsVertical;
    private static final EnumFacing[] facings;
    private static final Vec3i[] surroundingBlocksOffset;
    private boolean g = true;

    public PandaRedstoneWire() {
        this.setHardness(0.0F);
        this.setSoundType(SoundType.STONE);
        this.setUnlocalizedName("redstoneDust");
        this.disableStats();
    }

    private void updateSurroundingRedstone(World world, BlockPos blockposition, IBlockState iblockdata) {
        this.calculateCurrentChanges(world, blockposition);
        Set<BlockPos> blocksNeedingUpdate = Sets.newLinkedHashSet();
        Iterator<BlockPos> it = this.updatedRedstoneWire.iterator();

        while (it.hasNext()) {
            BlockPos posi = it.next();
            this.addBlocksNeedingUpdate(world, posi, blocksNeedingUpdate);
        }

        it = Lists.newLinkedList(this.updatedRedstoneWire).descendingIterator();

        while (it.hasNext()) {
            this.addAllSurroundingBlocks(it.next(), blocksNeedingUpdate);
        }

        blocksNeedingUpdate.removeAll(this.updatedRedstoneWire);
        this.updatedRedstoneWire.clear();

        for (BlockPos posi : blocksNeedingUpdate) {
            world.neighborChanged(posi, this, blockposition);
        }

    }

    private void calculateCurrentChanges(World world, BlockPos blockposition) {
        if (world.getBlockState(blockposition).getBlock() == this) {
            this.turnOff.add(blockposition);
        } else {
            this.checkSurroundingWires(world, blockposition);
        }

        BlockPos pos;
        IBlockState state;
        int oldPower;
        int blockPower;
        int wirePower;
        int newPower;
        // BlockRedstoneEvent event;
        for (; !this.turnOff.isEmpty(); this.checkSurroundingWires(world, pos)) {
            pos = this.turnOff.remove(0);
            state = world.getBlockState(pos);
            oldPower = state.getValue(POWER);
            this.g = false;
            blockPower = world.isBlockIndirectlyGettingPowered(pos);
            this.g = true;
            wirePower = this.getSurroundingWirePower(world, pos);
            --wirePower;
            newPower = Math.max(blockPower, wirePower);
            /*
            if (oldPower != newPower) {
                event = new BlockRedstoneEvent(world.getWorld().getBlockAt(pos.getX(), pos.getY(), pos.getZ()), oldPower, newPower);
                world.getServer().getPluginManager().callEvent(event);
                newPower = event.getNewCurrent();
            }
            */

            if (newPower < oldPower) {
                if (blockPower > 0 && !this.turnOn.contains(pos)) {
                    this.turnOn.add(pos);
                }

                this.setWireState(world, pos, state, 0);
            } else if (newPower > oldPower) {
                this.setWireState(world, pos, state, newPower);
            }
        }

        for (; !this.turnOn.isEmpty(); this.checkSurroundingWires(world, pos)) {
            pos = this.turnOn.remove(0);
            state = world.getBlockState(pos);
            oldPower = state.getValue(POWER);
            this.g = false;
            blockPower = world.isBlockIndirectlyGettingPowered(pos);
            this.g = true;
            wirePower = this.getSurroundingWirePower(world, pos);
            --wirePower;
            newPower = Math.max(blockPower, wirePower);
            /*
            if (oldPower != newPower) {
                event = new BlockRedstoneEvent(world.getWorld().getBlockAt(pos.getX(), pos.getY(), pos.getZ()), oldPower, newPower);
                world.getServer().getPluginManager().callEvent(event);
                newPower = event.getNewCurrent();
            }
            */

            if (newPower > oldPower) {
                this.setWireState(world, pos, state, newPower);
            } else if (newPower < oldPower) {
            }
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

        EnumFacing[] var5 = facingsHorizontal;
        int var6 = var5.length;

        int var7;
        EnumFacing facingVertical;
        BlockPos offsetPos;
        for (var7 = 0; var7 < var6; ++var7) {
            facingVertical = var5[var7];
            offsetPos = pos.offset(facingVertical);
            if (facingVertical.getAxis().isHorizontal()) {
                this.addWireToList(worldIn, offsetPos, ownPower);
            }
        }

        var5 = facingsVertical;
        var6 = var5.length;

        for (var7 = 0; var7 < var6; ++var7) {
            facingVertical = var5[var7];
            offsetPos = pos.offset(facingVertical);
            boolean solidBlock = worldIn.getBlockState(offsetPos).isBlockNormalCube();

            for (EnumFacing facingHorizontal : facingsHorizontal) {
                if (facingVertical == EnumFacing.UP && !solidBlock || facingVertical == EnumFacing.DOWN && solidBlock && !worldIn.getBlockState(offsetPos.offset(facingHorizontal)).isBlockNormalCube()) {
                    this.addWireToList(worldIn, offsetPos.offset(facingHorizontal), ownPower);
                }
            }
        }

    }

    private int getSurroundingWirePower(World worldIn, BlockPos pos) {
        int wirePower = 0;

        for (EnumFacing enumfacing : EnumFacing.Plane.HORIZONTAL) {
            BlockPos offsetPos = pos.offset(enumfacing);
            wirePower = this.getMaxCurrentStrength(worldIn, offsetPos, wirePower);
            if (worldIn.getBlockState(offsetPos).isNormalCube() && !worldIn.getBlockState(pos.up()).isNormalCube()) {
                wirePower = this.getMaxCurrentStrength(worldIn, offsetPos.up(), wirePower);
            } else if (!worldIn.getBlockState(offsetPos).isNormalCube()) {
                wirePower = this.getMaxCurrentStrength(worldIn, offsetPos.down(), wirePower);
            }
        }

        return wirePower;
    }

    private void addBlocksNeedingUpdate(World worldIn, BlockPos pos, Set<BlockPos> set) {
        List<EnumFacing> connectedSides = this.getSidesToPower(worldIn, pos);
        EnumFacing[] var5 = facings;
        int var6 = var5.length;

        int var7;
        EnumFacing facing;
        BlockPos offsetPos;
        for (var7 = 0; var7 < var6; ++var7) {
            facing = var5[var7];
            offsetPos = pos.offset(facing);
            if ((connectedSides.contains(facing.getOpposite()) || facing == EnumFacing.DOWN || facing.getAxis().isHorizontal() && canConnectTo(worldIn.getBlockState(offsetPos), facing, worldIn, offsetPos)) && this.canBlockBePoweredFromSide(worldIn.getBlockState(offsetPos), facing, true)) {
                set.add(offsetPos);
            }
        }

        var5 = facings;
        var6 = var5.length;

        for (var7 = 0; var7 < var6; ++var7) {
            facing = var5[var7];
            offsetPos = pos.offset(facing);
            if ((connectedSides.contains(facing.getOpposite()) || facing == EnumFacing.DOWN) && worldIn.getBlockState(offsetPos).isBlockNormalCube()) {

                for (EnumFacing facing1 : facings) {
                    if (this.canBlockBePoweredFromSide(worldIn.getBlockState(offsetPos.offset(facing1)), facing1, false)) {
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
            if (this.isPowerSourceAt(worldIn, pos, facing)) {
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

    public void onBlockAdded(World world, BlockPos blockposition, IBlockState iblockdata) {
        if (!world.isRemote) {
            this.updateSurroundingRedstone(world, blockposition, iblockdata);
            Iterator iterator = EnumFacing.Plane.VERTICAL.iterator();

            EnumFacing enumdirection;
            while (iterator.hasNext()) {
                enumdirection = (EnumFacing) iterator.next();
                world.notifyNeighborsOfStateChange(blockposition.offset(enumdirection), this, false);
            }

            iterator = EnumFacing.Plane.HORIZONTAL.iterator();

            while (iterator.hasNext()) {
                enumdirection = (EnumFacing) iterator.next();
                this.notifyWireNeighborsOfStateChange(world, blockposition.offset(enumdirection));
            }

            iterator = EnumFacing.Plane.HORIZONTAL.iterator();

            while (iterator.hasNext()) {
                enumdirection = (EnumFacing) iterator.next();
                BlockPos blockposition1 = blockposition.offset(enumdirection);
                if (world.getBlockState(blockposition1).isNormalCube()) {
                    this.notifyWireNeighborsOfStateChange(world, blockposition1.up());
                } else {
                    this.notifyWireNeighborsOfStateChange(world, blockposition1.down());
                }
            }
        }

    }

    public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
        super.breakBlock(worldIn, pos, state);
        if (!worldIn.isRemote) {
            EnumFacing[] aenumdirection = EnumFacing.values();

            EnumFacing enumdirection1;
            for (EnumFacing anAenumdirection : aenumdirection) {
                worldIn.notifyNeighborsOfStateChange(pos.offset(anAenumdirection), this, false);
            }

            this.updateSurroundingRedstone(worldIn, pos, state);
            Iterator iterator = EnumFacing.Plane.HORIZONTAL.iterator();

            while (iterator.hasNext()) {
                enumdirection1 = (EnumFacing) iterator.next();
                this.notifyWireNeighborsOfStateChange(worldIn, pos.offset(enumdirection1));
            }

            iterator = EnumFacing.Plane.HORIZONTAL.iterator();

            while (iterator.hasNext()) {
                enumdirection1 = (EnumFacing) iterator.next();
                BlockPos pos1 = pos.offset(enumdirection1);
                if (worldIn.getBlockState(pos1).isNormalCube()) {
                    this.notifyWireNeighborsOfStateChange(worldIn, pos1.up());
                } else {
                    this.notifyWireNeighborsOfStateChange(worldIn, pos1.down());
                }
            }
        }

    }

    public void neighborChanged(IBlockState iblockdata, World world, BlockPos blockposition, Block block, BlockPos blockposition1) {
        if (!world.isRemote) {
            if (this.canPlaceBlockAt(world, blockposition)) {
                this.updateSurroundingRedstone(world, blockposition, iblockdata);
            } else {
                this.dropBlockAsItem(world, blockposition, iblockdata, 0);
                world.setBlockToAir(blockposition);
            }
        }

    }

    public int getWeakPower(IBlockState iblockdata, IBlockAccess iblockaccess, BlockPos blockposition, EnumFacing enumdirection) {
        if (!this.g) {
            return 0;
        } else {
            int i = iblockdata.getValue(BlockRedstoneWire.POWER);
            if (i == 0) {
                return 0;
            } else if (enumdirection == EnumFacing.UP) {
                return i;
            } else {
                return this.getSidesToPower((World) iblockaccess, blockposition).contains(enumdirection) ? i : 0;
            }
        }
    }

    private boolean isPowerSourceAt(IBlockAccess iblockaccess, BlockPos blockposition, EnumFacing enumdirection) {
        BlockPos blockposition1 = blockposition.offset(enumdirection);
        IBlockState iblockdata = iblockaccess.getBlockState(blockposition1);
        boolean flag = iblockdata.isBlockNormalCube();
        boolean flag1 = iblockaccess.getBlockState(blockposition.up()).isBlockNormalCube();
        return !flag1 && flag && canConnectUpwardsTo(iblockaccess, blockposition1.up()) || (canConnectTo(iblockdata, enumdirection, iblockaccess, blockposition) || (iblockdata.getBlock() == Blocks.POWERED_REPEATER && iblockdata.getValue(BlockRedstoneDiode.FACING) == enumdirection || !flag && canConnectUpwardsTo(iblockaccess, blockposition1.down())));
    }

    static {
        facingsHorizontal = new EnumFacing[]{EnumFacing.WEST, EnumFacing.EAST, EnumFacing.NORTH, EnumFacing.SOUTH};
        facingsVertical = new EnumFacing[]{EnumFacing.DOWN, EnumFacing.UP};
        facings = ArrayUtils.addAll(facingsVertical, facingsHorizontal);
        Set<Vec3i> set = Sets.newLinkedHashSet();
        EnumFacing[] var1 = facings;
        int var2 = var1.length;

        int var3;
        EnumFacing facing1;
        for (var3 = 0; var3 < var2; ++var3) {
            facing1 = var1[var3];
            set.add(ReflectUtil.getOfT(facing1, Vec3i.class));
        }

        var1 = facings;
        var2 = var1.length;

        for (var3 = 0; var3 < var2; ++var3) {
            facing1 = var1[var3];
            Vec3i v1 = ReflectUtil.getOfT(facing1, Vec3i.class);

            for (EnumFacing facing2 : facings) {
                Vec3i v2 = ReflectUtil.getOfT(facing2, Vec3i.class);
                set.add(new BlockPos(v1.getX() + v2.getX(), v1.getY() + v2.getY(), v1.getZ() + v2.getZ()));
            }
        }

        set.remove(BlockPos.ORIGIN);
        surroundingBlocksOffset = set.toArray(new Vec3i[set.size()]);
    }


}
