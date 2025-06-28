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

    /**
     * Set block bounds based on mixed-in field instead of old calculations. Can possibly be de-Overwrite'd. TODO
     * @reason Original assumes any adjacency is connecting; this just takes cached results.
     * @author Myask
     * @param worldIn world of block to set bounding box for
     * @param x coordinate of that block
     * @param y coordinate of that block
     * @param z coordinate of that block
     */
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

    /**
     * No longer need to prevent placement by other doubles. Can probably be de-@Overwrite'd. TODO
     * @reason Double chests no longer exclude each other--that's the whole point of this mod.
     * @author Myask
     * @param worldIn world of prospective block position
     * @param x coordinate of that block
     * @param y coordinate of that block
     * @param z coordinate of that block
     * @return true, now: the block has no special case to not place it, so normal logic is fine.
     */
    @Overwrite
    public boolean canPlaceBlockAt(World worldIn, int x, int y, int z)
    { return true; }

    /**
     * Returns whether chest is a double, as before.
     * @reason Logic changed. Much cheaper to use saved value.
     * @author Myask
     * @param worldIn world of block to check if it's a double
     * @param x coordinate of that block
     * @param y coordinate of that block
     * @param z coordinate of that block
     * @return whether it's a double chest
     */
    @Overwrite
    private boolean func_149952_n(World worldIn, int x, int y, int z) {
        if (worldIn.getTileEntity(x, y, z) instanceof TileEntityChest tEC)
            return ((IFacingChest) tEC).adjachest$doubled();
        else return false;
    }

    /**
     * @reason All the old logic assumes any adjacent chest is part of a block's double. No point checking all directions.
     * @author Myask
     * @param w World
     * @param x coordinate
     * @param y coordinate
     * @param z coordinate
     * @return inventory of [double] chest, as before...or null if blocked by block or ocelot.
     */
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
     * Pick a new facing for the target.
     * @reason Original changes metas of this block and adjacents based on the old logic--undesired behavior.
     * @author Myask
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

    /**
     * @reason Remove all the extraneous calls to update adjacent metas for being doubled with, as that logic no longer applies.
     * @author Myask
     * @param w world of new chest
     * @param x coordinate of new chest
     * @param y coordinate of new chest
     * @param z coordinate of new chest
     */
    @Overwrite
    public void onBlockAdded(World w, int x, int y, int z)
    {
        super.onBlockAdded(w, x, y, z);
    }

    /**
     * Select meta for the newly-placed block.
     * @reason to base it on facing rather than neighbors. Could possibly head-cancel? TODO?
     * @author Myask
     * @param worldIn world of newly-placed block
     * @param x coordinate of that block
     * @param y coordinate of that block
     * @param z coordinate of that block
     * @param placer the player who placed that block
     * @param itemIn the item[stack] used to placed that block
     */
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

    /**
     * reason: Now that a chest can be adjacent to a double, it may be desired to latch it onto one that becomes single.
     * @author Myask
     * @param world The world
     * @param x The x position of this block instance
     * @param y The y position of this block instance
     * @param z The z position of this block instance
     * @param tileX The x position of the tile that changed
     * @param tileY The y position of the tile that changed
     * @param tileZ The z position of the tile that changed
     */
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
