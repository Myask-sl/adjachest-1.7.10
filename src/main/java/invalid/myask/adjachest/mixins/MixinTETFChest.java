package invalid.myask.adjachest.mixins;

import invalid.myask.adjachest.Config;
import invalid.myask.adjachest.ducks.IAdjChest;
import invalid.myask.adjachest.ducks.IFacingChest;
import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraftforge.common.util.ForgeDirection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import twilightforest.tileentity.TileEntityTFChest;

import static net.minecraftforge.common.util.ForgeDirection.*;

@Mixin(TileEntityTFChest.class)
public abstract class MixinTETFChest extends MixinTileEntityChest {
    @Shadow
    private boolean func_145977_a(int x, int y, int z) {return false;}

    @Override
    protected boolean aValidDouble(int x, int y, int z) {
        return func_145977_a(x, y, z);
    }

    /**
     * @reason see called method--same reason: use new logic instead of old (meta-mutating, adjacent-double-asssuming) logic.
     * @author Myask
     */
    @Overwrite
    public void checkForAdjacentChests() {
        ((IAdjChest)this).adjachest$checkForAdjacentChests();
    }
        /*
        if (!hasWorldObj() || this.adjacentChestChecked) return; //don't keep doing it all the time
        this.adjacentChestChecked = true;
        TileEntityChest tEC = adjacentChestZPos = adjacentChestZNeg = adjacentChestXPos = adjacentChestXNeg = null;
        ForgeDirection first = null, second = null, third = null, fourth = null, result = UNKNOWN, old = adjachest$doubleDirection;
        if (adjachest$doubleDirection != UNKNOWN) {
            tEC = (TileEntityChest) worldObj.getTileEntity(xCoord + adjachest$doubleDirection.offsetX, yCoord,
                zCoord + adjachest$doubleDirection.offsetZ);
            if (tEC != null
                && ((IFacingChest) tEC).adjachest$doubledWith().getOpposite() == adjachest$doubleDirection) {
                first = adjachest$doubleDirection; second = first.getOpposite();
                third = adjachest$doubleDirection.getRotation(UP); fourth = second.getRotation(UP);
            }
        }
        if (first == null) {
            switch (getBlockMetadata()) {
                case 2, 3, 6, 7:
                    first = WEST; //firstX--;
                    second = EAST; //secondX++;
                    third = NORTH;
                    fourth = SOUTH;
                    break;
                case 4, 5, 8, 9:
                    first = NORTH; //firstZ--;
                    second = SOUTH; //secondZ++;
                    third = WEST;
                    fourth = EAST;
                    break;
                case 10, 11:
                    first = EAST; //X++;
                    second = WEST; //X--;
                    third = NORTH;
                    fourth = SOUTH;
                    break;
                case 12, 13:
                    first = SOUTH; //Z++;
                    second = NORTH; //Z++;
                    third = WEST;
                    fourth = EAST;
                    break;
                default:
                    first = NORTH;
                    second = WEST;
                    third = SOUTH;
                    fourth = EAST;
            }
        }
        int myMeta = worldObj.getBlockMetadata(xCoord, yCoord, zCoord);
        ForgeDirection directionToPair = UNKNOWN;
        ForgeDirection[] dirList = {first, second, third, fourth};
        int i, theirMeta = -1;
        for (i = 0; i < 4; i++) {
            directionToPair = dirList[i];
            int targX = xCoord + directionToPair.offsetX, targZ = zCoord + directionToPair.offsetZ;
            Block targBlock = worldObj.getBlock(targX, yCoord, targZ);
            if (targBlock != worldObj.getBlock(xCoord, yCoord, zCoord))
                continue;
            theirMeta = worldObj.getBlockMetadata(targX, yCoord, targZ);
            if (Config.require_face_same
                && ((theirMeta != myMeta) || (theirMeta & 6) == (directionToPair.ordinal() & 6))) continue;
            TileEntityChest iterChest = (TileEntityChest) worldObj.getTileEntity(targX, yCoord, targZ);
            IFacingChest iterInt = (IFacingChest) iterChest;
            if (iterInt == null) continue;
            if (iterInt.adjachest$doubledWith() == directionToPair.getOpposite()
                || (!iterInt.adjachest$doubled())) {
                if (func_145977_a(targX, yCoord, targZ)) { //same chest? TFchests use this too. But, fucking private, so no polymorphism
                    tEC = iterChest;
                    break;
                }
            }
        }
        if (tEC != null && !((IFacingChest)tEC).adjachest$loaded()) {
            adjachest$doubleDirection = old;
            adjacentChestChecked = false;
            return; //don't forget where we were pointed and/or wait for a description packet
        }
        if (i == 4 || tEC == null || directionToPair == UNKNOWN
            || directionToPair == UP || directionToPair == DOWN
            || (Config.require_face_same && theirMeta != myMeta)) { //don't grab on
            adjachest$doubleDirection = UNKNOWN;
            return;
        }
        switch (directionToPair) {
            case NORTH:
                adjacentChestZNeg = tEC;
//                tEC.adjacentChestZPos = (TileEntityChest) (TileEntity) this;
                first = WEST; //now used to pick facing for if we're allowing different facing merge
                second = EAST;
                break;
            case SOUTH:
                adjacentChestZPos = tEC;
//                tEC.adjacentChestZNeg = (TileEntityChest) (TileEntity) this;
                first = WEST;
                second = EAST;
                break;
            case WEST:
                adjacentChestXNeg = tEC;
//                tEC.adjacentChestXPos = (TileEntityChest) (TileEntity) this;
                first = NORTH;
                second = SOUTH;
                break;
            case EAST:
                adjacentChestXPos = tEC;
//                tEC.adjacentChestXNeg = (TileEntityChest) (TileEntity) this;
                first = NORTH;
                second = SOUTH;
                break;
            default:
        }
        adjachest$doubleDirection = directionToPair;
        ((IFacingChest)tEC).adjachest$setDouble(directionToPair.getOpposite());
        ((MixinTileEntityChest) (TileEntity) tEC).adjachest$considerInvalidation(tEC, (TileEntityChest) (TileEntity) this, directionToPair.getOpposite());
        if (theirMeta != myMeta || ((theirMeta & 6) == (directionToPair.ordinal() & 6))) { //gotta make match --earlier check should filter out if config doesn't want
            result = clearTo(first) ? first : second;
            myMeta = result.ordinal();
            worldObj.setBlockMetadataWithNotify(xCoord, yCoord, zCoord, myMeta,3);
            worldObj.setBlockMetadataWithNotify(xCoord + result.offsetX, yCoord,
                zCoord + result.offsetZ, myMeta,3);
        }
    }
*/
}
