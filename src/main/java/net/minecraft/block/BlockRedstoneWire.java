package net.minecraft.block;

import cn.windmourn.operator.utils.ReflectUtil;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.*;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.commons.lang3.ArrayUtils;

import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class BlockRedstoneWire extends Block {
    public static final PropertyEnum<BlockRedstoneWire.EnumAttachPosition> NORTH = PropertyEnum.create("north", BlockRedstoneWire.EnumAttachPosition.class);
    public static final PropertyEnum<BlockRedstoneWire.EnumAttachPosition> EAST = PropertyEnum.create("east", BlockRedstoneWire.EnumAttachPosition.class);
    public static final PropertyEnum<BlockRedstoneWire.EnumAttachPosition> SOUTH = PropertyEnum.create("south", BlockRedstoneWire.EnumAttachPosition.class);
    public static final PropertyEnum<BlockRedstoneWire.EnumAttachPosition> WEST = PropertyEnum.create("west", BlockRedstoneWire.EnumAttachPosition.class);
    public static final PropertyInteger POWER = PropertyInteger.create("power", 0, 15);
    protected static final AxisAlignedBB[] REDSTONE_WIRE_AABB = new AxisAlignedBB[]{new AxisAlignedBB(0.1875D, 0.0D, 0.1875D, 0.8125D, 0.0625D, 0.8125D), new AxisAlignedBB(0.1875D, 0.0D, 0.1875D, 0.8125D, 0.0625D, 1.0D), new AxisAlignedBB(0.0D, 0.0D, 0.1875D, 0.8125D, 0.0625D, 0.8125D), new AxisAlignedBB(0.0D, 0.0D, 0.1875D, 0.8125D, 0.0625D, 1.0D), new AxisAlignedBB(0.1875D, 0.0D, 0.0D, 0.8125D, 0.0625D, 0.8125D), new AxisAlignedBB(0.1875D, 0.0D, 0.0D, 0.8125D, 0.0625D, 1.0D), new AxisAlignedBB(0.0D, 0.0D, 0.0D, 0.8125D, 0.0625D, 0.8125D), new AxisAlignedBB(0.0D, 0.0D, 0.0D, 0.8125D, 0.0625D, 1.0D), new AxisAlignedBB(0.1875D, 0.0D, 0.1875D, 1.0D, 0.0625D, 0.8125D), new AxisAlignedBB(0.1875D, 0.0D, 0.1875D, 1.0D, 0.0625D, 1.0D), new AxisAlignedBB(0.0D, 0.0D, 0.1875D, 1.0D, 0.0625D, 0.8125D), new AxisAlignedBB(0.0D, 0.0D, 0.1875D, 1.0D, 0.0625D, 1.0D), new AxisAlignedBB(0.1875D, 0.0D, 0.0D, 1.0D, 0.0625D, 0.8125D), new AxisAlignedBB(0.1875D, 0.0D, 0.0D, 1.0D, 0.0625D, 1.0D), new AxisAlignedBB(0.0D, 0.0D, 0.0D, 1.0D, 0.0625D, 0.8125D), new AxisAlignedBB(0.0D, 0.0D, 0.0D, 1.0D, 0.0625D, 1.0D)};
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

    /**
     * List of blocks to update with redstone.
     */
    private final Set<BlockPos> blocksNeedingUpdate = Sets.newHashSet();
    private final Set<BlockPos> updatedRedstoneWire = Sets.newLinkedHashSet();
    private boolean canProvidePower = true;
    /*
     * 作者说用LinkedHashSet替代arraylist没有明显的性能提示，红石设备不多的时候的确如此
     * 但是实测在红石设备数量很大的时候，有2~5%的性能提升（基于PaperSpigot1.10.2测试），所以还是改用LinkedHashSet来实现
     */
    // 需要被断路的红石位置
    private Set<BlockPos> turnOff = Sets.newLinkedHashSet();
    // 需要被激活的红石位置
    private Set<BlockPos> turnOn = Sets.newLinkedHashSet();

    public BlockRedstoneWire() {
        super(Material.CIRCUITS);
        this.setDefaultState(this.blockState.getBaseState().withProperty(NORTH, BlockRedstoneWire.EnumAttachPosition.NONE).withProperty(EAST, BlockRedstoneWire.EnumAttachPosition.NONE).withProperty(SOUTH, BlockRedstoneWire.EnumAttachPosition.NONE).withProperty(WEST, BlockRedstoneWire.EnumAttachPosition.NONE).withProperty(POWER, 0));

        this.setHardness(0.0F);
        this.setSoundType(SoundType.STONE);
        this.setUnlocalizedName("redstoneDust");
        this.disableStats();
    }

    private static int getAABBIndex(IBlockState state) {
        int i = 0;
        boolean flag = state.getValue(NORTH) != BlockRedstoneWire.EnumAttachPosition.NONE;
        boolean flag1 = state.getValue(EAST) != BlockRedstoneWire.EnumAttachPosition.NONE;
        boolean flag2 = state.getValue(SOUTH) != BlockRedstoneWire.EnumAttachPosition.NONE;
        boolean flag3 = state.getValue(WEST) != BlockRedstoneWire.EnumAttachPosition.NONE;

        if (flag || flag2 && !flag && !flag1 && !flag3) {
            i |= 1 << EnumFacing.NORTH.getHorizontalIndex();
        }

        if (flag1 || flag3 && !flag && !flag1 && !flag2) {
            i |= 1 << EnumFacing.EAST.getHorizontalIndex();
        }

        if (flag2 || flag && !flag1 && !flag2 && !flag3) {
            i |= 1 << EnumFacing.SOUTH.getHorizontalIndex();
        }

        if (flag3 || flag1 && !flag && !flag2 && !flag3) {
            i |= 1 << EnumFacing.WEST.getHorizontalIndex();
        }

        return i;
    }

    protected static boolean canConnectUpwardsTo(IBlockAccess worldIn, BlockPos pos) {
        return canConnectTo(worldIn.getBlockState(pos), null, worldIn, pos);
    }

    protected static boolean canConnectTo(IBlockState blockState, @Nullable EnumFacing side, IBlockAccess world, BlockPos pos) {
        Block block = blockState.getBlock();

        if (block == Blocks.REDSTONE_WIRE) {
            return true;
        } else if (Blocks.UNPOWERED_REPEATER.isSameDiode(blockState)) {
            EnumFacing enumfacing = (EnumFacing) blockState.getValue(BlockRedstoneRepeater.FACING);
            return enumfacing == side || enumfacing.getOpposite() == side;
        } else {
            return Blocks.OBSERVER == blockState.getBlock() ? side == blockState.getValue(BlockObserver.FACING) : blockState.getBlock().canConnectRedstone(blockState, world, pos, side);
        }
    }

    @SideOnly(Side.CLIENT)
    public static int colorMultiplier(int p_176337_0_) {
        float f = (float) p_176337_0_ / 15.0F;
        float f1 = f * 0.6F + 0.4F;

        if (p_176337_0_ == 0) {
            f1 = 0.3F;
        }

        float f2 = f * f * 0.7F - 0.5F;
        float f3 = f * f * 0.6F - 0.7F;

        if (f2 < 0.0F) {
            f2 = 0.0F;
        }

        if (f3 < 0.0F) {
            f3 = 0.0F;
        }

        int i = MathHelper.clamp((int) (f1 * 255.0F), 0, 255);
        int j = MathHelper.clamp((int) (f2 * 255.0F), 0, 255);
        int k = MathHelper.clamp((int) (f3 * 255.0F), 0, 255);
        return -16777216 | i << 16 | j << 8 | k;
    }

    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return REDSTONE_WIRE_AABB[getAABBIndex(state.getActualState(source, pos))];
    }

    /**
     * Get the actual Block state of this Block at the given position. This applies properties not visible in the
     * metadata, such as fence connections.
     */
    public IBlockState getActualState(IBlockState state, IBlockAccess worldIn, BlockPos pos) {
        state = state.withProperty(WEST, this.getAttachPosition(worldIn, pos, EnumFacing.WEST));
        state = state.withProperty(EAST, this.getAttachPosition(worldIn, pos, EnumFacing.EAST));
        state = state.withProperty(NORTH, this.getAttachPosition(worldIn, pos, EnumFacing.NORTH));
        state = state.withProperty(SOUTH, this.getAttachPosition(worldIn, pos, EnumFacing.SOUTH));
        return state;
    }

    private BlockRedstoneWire.EnumAttachPosition getAttachPosition(IBlockAccess worldIn, BlockPos pos, EnumFacing direction) {
        BlockPos blockpos = pos.offset(direction);
        IBlockState iblockstate = worldIn.getBlockState(pos.offset(direction));

        if (!canConnectTo(worldIn.getBlockState(blockpos), direction, worldIn, blockpos) && (iblockstate.isNormalCube() || !canConnectUpwardsTo(worldIn, blockpos.down()))) {
            IBlockState iblockstate1 = worldIn.getBlockState(pos.up());

            if (!iblockstate1.isNormalCube()) {
                boolean flag = worldIn.getBlockState(blockpos).isSideSolid(worldIn, blockpos, EnumFacing.UP) || worldIn.getBlockState(blockpos).getBlock() == Blocks.GLOWSTONE;

                if (flag && canConnectUpwardsTo(worldIn, blockpos.up())) {
                    if (iblockstate.isBlockNormalCube()) {
                        return BlockRedstoneWire.EnumAttachPosition.UP;
                    }

                    return BlockRedstoneWire.EnumAttachPosition.SIDE;
                }
            }

            return BlockRedstoneWire.EnumAttachPosition.NONE;
        } else {
            return BlockRedstoneWire.EnumAttachPosition.SIDE;
        }
    }

    @Nullable
    public AxisAlignedBB getCollisionBoundingBox(IBlockState blockState, IBlockAccess worldIn, BlockPos pos) {
        return NULL_AABB;
    }

    /**
     * Used to determine ambient occlusion and culling when rebuilding chunks for render
     */
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    public boolean isFullCube(IBlockState state) {
        return false;
    }

    public boolean canPlaceBlockAt(World worldIn, BlockPos pos) {
        return worldIn.getBlockState(pos.down()).isFullyOpaque() || worldIn.getBlockState(pos.down()).getBlock() == Blocks.GLOWSTONE;
    }

    private void updateSurroundingRedstone(World worldIn, BlockPos pos, IBlockState state) {
        calculateCurrentChanges(worldIn, pos);
        Set<BlockPos> blocksNeedingUpdate = Sets.newLinkedHashSet();

        for (BlockPos anUpdatedRedstoneWire : this.updatedRedstoneWire) {
            addBlocksNeedingUpdate(worldIn, anUpdatedRedstoneWire, blocksNeedingUpdate);
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
            this.canProvidePower = false;
            int blockPower = worldIn.isBlockIndirectlyGettingPowered(pos1);
            this.canProvidePower = true;
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
            this.canProvidePower = false;
            int blockPower = worldIn.isBlockIndirectlyGettingPowered(pos1);
            this.canProvidePower = true;
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

            if ((connectedSides.contains(facing.getOpposite()) || facing == EnumFacing.DOWN || (facing.getAxis().isHorizontal() && canConnectTo(worldIn.getBlockState(offsetPos), facing, worldIn, pos))) && canBlockBePoweredFromSide(worldIn.getBlockState(offsetPos), facing, true)) {
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

    /**
     * Calls World.notifyNeighborsOfStateChange() for all neighboring blocks, but only if the given block is a redstone
     * wire.
     */
    private void notifyWireNeighborsOfStateChange(World worldIn, BlockPos pos) {
        if (worldIn.getBlockState(pos).getBlock() == this) {
            worldIn.notifyNeighborsOfStateChange(pos, this, false);

            for (EnumFacing enumfacing : EnumFacing.values()) {
                worldIn.notifyNeighborsOfStateChange(pos.offset(enumfacing), this, false);
            }
        }
    }

    /**
     * Called after the block is set in the Chunk data, but before the Tile Entity is set
     */
    public void onBlockAdded(World worldIn, BlockPos pos, IBlockState state) {
        if (!worldIn.isRemote) {
            this.updateSurroundingRedstone(worldIn, pos, state);

            for (EnumFacing enumfacing : EnumFacing.Plane.VERTICAL) {
                worldIn.notifyNeighborsOfStateChange(pos.offset(enumfacing), this, false);
            }

            for (EnumFacing enumfacing1 : EnumFacing.Plane.HORIZONTAL) {
                this.notifyWireNeighborsOfStateChange(worldIn, pos.offset(enumfacing1));
            }

            for (EnumFacing enumfacing2 : EnumFacing.Plane.HORIZONTAL) {
                BlockPos blockpos = pos.offset(enumfacing2);

                if (worldIn.getBlockState(blockpos).isNormalCube()) {
                    this.notifyWireNeighborsOfStateChange(worldIn, blockpos.up());
                } else {
                    this.notifyWireNeighborsOfStateChange(worldIn, blockpos.down());
                }
            }
        }
    }

    /**
     * Called serverside after this block is replaced with another in Chunk, but before the Tile Entity is updated
     */
    public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
        super.breakBlock(worldIn, pos, state);

        if (!worldIn.isRemote) {
            for (EnumFacing enumfacing : EnumFacing.values()) {
                worldIn.notifyNeighborsOfStateChange(pos.offset(enumfacing), this, false);
            }

            this.updateSurroundingRedstone(worldIn, pos, state);

            for (EnumFacing enumfacing1 : EnumFacing.Plane.HORIZONTAL) {
                this.notifyWireNeighborsOfStateChange(worldIn, pos.offset(enumfacing1));
            }

            for (EnumFacing enumfacing2 : EnumFacing.Plane.HORIZONTAL) {
                BlockPos blockpos = pos.offset(enumfacing2);

                if (worldIn.getBlockState(blockpos).isNormalCube()) {
                    this.notifyWireNeighborsOfStateChange(worldIn, blockpos.up());
                } else {
                    this.notifyWireNeighborsOfStateChange(worldIn, blockpos.down());
                }
            }
        }
    }

    private int getMaxCurrentStrength(World worldIn, BlockPos pos, int strength) {
        if (worldIn.getBlockState(pos).getBlock() != this) {
            return strength;
        } else {
            int i = worldIn.getBlockState(pos).getValue(POWER);
            return i > strength ? i : strength;
        }
    }

    /**
     * Called when a neighboring block was changed and marks that this state should perform any checks during a neighbor
     * change. Cases may include when redstone power is updated, cactus blocks popping off due to a neighboring solid
     * block, etc.
     */
    public void neighborChanged(IBlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos fromPos) {
        if (!worldIn.isRemote) {
            if (this.canPlaceBlockAt(worldIn, pos)) {
                this.updateSurroundingRedstone(worldIn, pos, state);
            } else {
                this.dropBlockAsItem(worldIn, pos, state, 0);
                worldIn.setBlockToAir(pos);
            }
        }
    }

    /**
     * Get the Item that this Block should drop when harvested.
     */
    public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        return Items.REDSTONE;
    }

    public int getStrongPower(IBlockState blockState, IBlockAccess blockAccess, BlockPos pos, EnumFacing side) {
        return !this.canProvidePower ? 0 : blockState.getWeakPower(blockAccess, pos, side);
    }

    public int getWeakPower(IBlockState blockState, IBlockAccess blockAccess, BlockPos pos, EnumFacing side) {
        if (!this.canProvidePower) {
            return 0;
        } else {
            int i = blockState.getValue(POWER);

            if (i == 0) {
                return 0;
            } else if (side == EnumFacing.UP) {
                return i;
            } else if (getSidesToPower((World) blockAccess, pos).contains(side)) {
                return i;
            } else {
                return 0;
            }
        }
    }

    private boolean isPowerSourceAt(IBlockAccess worldIn, BlockPos pos, EnumFacing side) {
        BlockPos blockpos = pos.offset(side);
        IBlockState iblockstate = worldIn.getBlockState(blockpos);
        boolean flag = iblockstate.isNormalCube();
        boolean flag1 = worldIn.getBlockState(pos.up()).isNormalCube();
        return !flag1 && flag && canConnectUpwardsTo(worldIn, blockpos.up()) || (canConnectTo(iblockstate, side, worldIn, pos) || (iblockstate.getBlock() == Blocks.POWERED_REPEATER && iblockstate.getValue(BlockRedstoneDiode.FACING) == side ? true : !flag && canConnectUpwardsTo(worldIn, blockpos.down())));
    }

    /**
     * Can this block provide power. Only wire currently seems to have this change based on its state.
     */
    public boolean canProvidePower(IBlockState state) {
        return this.canProvidePower;
    }

    @SideOnly(Side.CLIENT)
    public void randomDisplayTick(IBlockState stateIn, World worldIn, BlockPos pos, Random rand) {
        int i = stateIn.getValue(POWER);

        if (i != 0) {
            double d0 = (double) pos.getX() + 0.5D + ((double) rand.nextFloat() - 0.5D) * 0.2D;
            double d1 = (double) ((float) pos.getY() + 0.0625F);
            double d2 = (double) pos.getZ() + 0.5D + ((double) rand.nextFloat() - 0.5D) * 0.2D;
            float f = (float) i / 15.0F;
            float f1 = f * 0.6F + 0.4F;
            float f2 = Math.max(0.0F, f * f * 0.7F - 0.5F);
            float f3 = Math.max(0.0F, f * f * 0.6F - 0.7F);
            worldIn.spawnParticle(EnumParticleTypes.REDSTONE, d0, d1, d2, (double) f1, (double) f2, (double) f3, new int[0]);
        }
    }

    public ItemStack getItem(World worldIn, BlockPos pos, IBlockState state) {
        return new ItemStack(Items.REDSTONE);
    }

    /**
     * Convert the given metadata into a BlockState for this Block
     */
    public IBlockState getStateFromMeta(int meta) {
        return this.getDefaultState().withProperty(POWER, Integer.valueOf(meta));
    }

    @SideOnly(Side.CLIENT)
    public BlockRenderLayer getBlockLayer() {
        return BlockRenderLayer.CUTOUT;
    }

    /**
     * Convert the BlockState into the correct metadata value
     */
    public int getMetaFromState(IBlockState state) {
        return state.getValue(POWER);
    }

    /**
     * Returns the blockstate with the given rotation from the passed blockstate. If inapplicable, returns the passed
     * blockstate.
     */
    public IBlockState withRotation(IBlockState state, Rotation rot) {
        switch (rot) {
            case CLOCKWISE_180:
                return state.withProperty(NORTH, state.getValue(SOUTH)).withProperty(EAST, state.getValue(WEST)).withProperty(SOUTH, state.getValue(NORTH)).withProperty(WEST, state.getValue(EAST));
            case COUNTERCLOCKWISE_90:
                return state.withProperty(NORTH, state.getValue(EAST)).withProperty(EAST, state.getValue(SOUTH)).withProperty(SOUTH, state.getValue(WEST)).withProperty(WEST, state.getValue(NORTH));
            case CLOCKWISE_90:
                return state.withProperty(NORTH, state.getValue(WEST)).withProperty(EAST, state.getValue(NORTH)).withProperty(SOUTH, state.getValue(EAST)).withProperty(WEST, state.getValue(SOUTH));
            default:
                return state;
        }
    }

    /**
     * Returns the blockstate with the given mirror of the passed blockstate. If inapplicable, returns the passed
     * blockstate.
     */
    public IBlockState withMirror(IBlockState state, Mirror mirrorIn) {
        switch (mirrorIn) {
            case LEFT_RIGHT:
                return state.withProperty(NORTH, state.getValue(SOUTH)).withProperty(SOUTH, state.getValue(NORTH));
            case FRONT_BACK:
                return state.withProperty(EAST, state.getValue(WEST)).withProperty(WEST, state.getValue(EAST));
            default:
                return super.withMirror(state, mirrorIn);
        }
    }

    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, NORTH, EAST, SOUTH, WEST, POWER);
    }

    static enum EnumAttachPosition implements IStringSerializable {
        UP("up"),
        SIDE("side"),
        NONE("none");

        private final String name;

        private EnumAttachPosition(String name) {
            this.name = name;
        }

        public String toString() {
            return this.getName();
        }

        public String getName() {
            return this.name;
        }
    }
}