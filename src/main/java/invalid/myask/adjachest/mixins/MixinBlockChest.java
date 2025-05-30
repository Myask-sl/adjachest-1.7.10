package invalid.myask.adjachest.mixins;

import invalid.myask.adjachest.ducks.IFacingChest;
import net.minecraft.block.BlockChest;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryLargeChest;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.MathHelper;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(BlockChest.class)
public abstract class MixinBlockChest extends BlockContainer {
    protected MixinBlockChest(Material mat) { super(mat); }

    @Shadow
    private static boolean func_149953_o(World w, int x, int y, int z) {return true;}
    //ocelot check

    @Overwrite
    public void setBlockBoundsBasedOnState(IBlockAccess worldIn, int x, int y, int z)
    {
        ForgeDirection dir = ForgeDirection.UNKNOWN;
        if (worldIn.getTileEntity(x,y,z) instanceof IFacingChest ifc) dir = ifc.adjachest$doubledWith();
        switch (dir) {
            case NORTH:
                this.setBlockBounds(0.0625F, 0.0F, 0.0F, 0.9375F, 0.875F, 0.9375F);
                break;
            case SOUTH:
                this.setBlockBounds(0.0625F, 0.0F, 0.0625F, 0.9375F, 0.875F, 1.0F);
                break;
            case WEST:
                this.setBlockBounds(0.0F, 0.0F, 0.0625F, 0.9375F, 0.875F, 0.9375F);
                break;
            case EAST:
                this.setBlockBounds(0.0625F, 0.0F, 0.0625F, 1.0F, 0.875F, 0.9375F);
                break;
            default:
                this.setBlockBounds(0.0625F, 0.0F, 0.0625F, 0.9375F, 0.875F, 0.9375F);
        }
    }

    @Overwrite
    public boolean canPlaceBlockAt(World worldIn, int x, int y, int z)
    { return true; }

    @Overwrite
    private boolean func_149952_n(World worldIn, int x, int y, int z) {
        if (worldIn.getTileEntity(x, y, z) instanceof TileEntityChest tEC)
            return ((IFacingChest) tEC).adjachest$doubled();
        else return false;
    }

    @Overwrite
    public IInventory func_149951_m(World w, int x, int y, int z) {
        if (w.getTileEntity(x, y, z) instanceof TileEntityChest thisOne) {
            IInventory result = null;

            if (!w.isSideSolid(x, y + 1, z, ForgeDirection.DOWN) && !func_149953_o(w, x, y, z)) { //this chest unblocked by block and ocelot
                if (thisOne instanceof IFacingChest iFC) {
                    if (iFC.adjachest$doubled()) {
                        TileEntity sideA = w.getTileEntity(x + iFC.adjachest$doubledWith().offsetX, y, z + iFC.adjachest$doubledWith().offsetZ),
                            sideB = thisOne;
                        if (sideA == null) {
                            iFC.adjachest$setDouble(ForgeDirection.UNKNOWN);
                            return null;
                        }
                        if (w.isSideSolid(sideA.xCoord, sideA.yCoord + 1, sideA.zCoord, ForgeDirection.DOWN)
                            || func_149953_o(w, sideA.xCoord, sideA.yCoord, sideA.zCoord))
                            return null; //doubled chest blocked
                        if (sideA.xCoord < sideB.xCoord || sideA.zCoord < sideB.zCoord)
                            result = new InventoryLargeChest("container.chestDouble", (IInventory) sideA, (IInventory) sideB);
                        else
                            result = new InventoryLargeChest("container.chestDouble", (IInventory) sideB, (IInventory) sideA);
                    } else result = thisOne;
                }
            }
            return result;
        }
        return null;
    }

    /**
     * Pick a new facing for the target. Original is rather...likely to change.
     * @param world of target
     * @param x of target
     * @param y of target
     * @param z of target
     */
    @Overwrite
    public void func_149954_e(World world, int x, int y, int z) {
        int meta = world.getBlockMetadata(x, y, z);
        if (meta < 2 || meta > 5)
            world.setBlockMetadataWithNotify(x, y, z, 2, 3);
    }

    @Overwrite
    public void onBlockAdded(World worldIn, int x, int y, int z)
    {
        super.onBlockAdded(worldIn, x, y, z);
    }

    @Overwrite
    public void onBlockPlacedBy(World worldIn, int x, int y, int z, EntityLivingBase placer, ItemStack itemIn) {
        int newMeta = MathHelper.floor_double((double)(placer.rotationYaw * 4.0F / 360.0F) + 0.5D) & 3;
        newMeta = switch(newMeta) {
            case 0 -> 2;
            case 1 -> 5;
            case 2 -> 3;
            case 3 -> 4;
            default -> 3;
        };

        worldIn.setBlockMetadataWithNotify(x, y, z, newMeta, 3);

        if (worldIn.getTileEntity(x, y, z) instanceof TileEntityChest tEC) {
            if (itemIn.hasDisplayName()) {
                tEC.func_145976_a(itemIn.getDisplayName());
            }
            ((IFacingChest) tEC).adjachest$setLoaded();
            tEC.checkForAdjacentChests();
        }
    }

    @Override
    public void onNeighborChange(IBlockAccess world, int x, int y, int z, int tileX, int tileY, int tileZ) {
        super.onNeighborChange(world, x, y, z, tileX, tileY, tileZ);
        if (world.getTileEntity(x, y, z) instanceof TileEntityChest chest) {
            IFacingChest iFC = (IFacingChest) chest;
            if (tileX == x + iFC.adjachest$doubledWith().offsetX && tileY == y &&
                tileZ == z + iFC.adjachest$doubledWith().offsetZ) {
                chest.adjacentChestChecked = false;
                //((IFacingChest) chest).adjachest$setDouble(ForgeDirection.UNKNOWN);
            }
        }
    }
}
