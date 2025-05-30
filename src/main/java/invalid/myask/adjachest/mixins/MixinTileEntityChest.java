package invalid.myask.adjachest.mixins;

import com.llamalad7.mixinextras.sugar.Local;
import invalid.myask.adjachest.Config;
import invalid.myask.adjachest.ducks.IAdjChest;
import invalid.myask.adjachest.ducks.IFacingChest;
import net.minecraft.block.Block;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;

import net.minecraftforge.common.util.ForgeDirection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


import static net.minecraftforge.common.util.ForgeDirection.*;


@Mixin(TileEntityChest.class)
public abstract class MixinTileEntityChest extends TileEntity implements IFacingChest, IAdjChest {

    @Shadow
    private boolean func_145977_a(int x, int y, int z) {
        return false;
    };

    protected boolean aValidDouble(int x, int y, int z) {
        return func_145977_a(x, y, z);
    }

    @Shadow
    public boolean adjacentChestChecked;
    @Shadow
    public TileEntityChest adjacentChestZNeg;
    @Shadow
    public TileEntityChest adjacentChestXPos;
    @Shadow
    public TileEntityChest adjacentChestXNeg;
    @Shadow
    public TileEntityChest adjacentChestZPos;

    @Shadow
    private void func_145978_a(TileEntityChest chest, int dirIndex) {}; //make adjacent recheck

    @Shadow
    public abstract int func_145980_j() ;

    @Unique
    public ForgeDirection adjachest$doubleDirection = UNKNOWN;
    @Unique
    boolean adjachest$finishedLoading = false;

    @Overwrite
    public void checkForAdjacentChests() {
        adjachest$checkForAdjacentChests();
    }

    public void adjachest$checkForAdjacentChests () {
        if (!hasWorldObj() || this.adjacentChestChecked) return; //don't keep doing it all the time
        this.adjacentChestChecked = true;
        TileEntityChest tEC = adjacentChestZPos = adjacentChestZNeg = adjacentChestXPos = adjacentChestXNeg = null;
        ForgeDirection first = null, second = null, third = null, fourth = null, result = UNKNOWN, old = adjachest$doubleDirection;
        if (adjachest$doubleDirection != UNKNOWN) {
            if (worldObj.getTileEntity(xCoord + adjachest$doubleDirection.offsetX, yCoord,
                zCoord + adjachest$doubleDirection.offsetZ) instanceof TileEntityChest that) {
                tEC = that;
                if (tEC != null
                    && ((IFacingChest) tEC).adjachest$doubledWith().getOpposite() == adjachest$doubleDirection) {
                    first = adjachest$doubleDirection;
                    second = first.getOpposite();
                    third = adjachest$doubleDirection.getRotation(UP);
                    fourth = second.getRotation(UP);
                }
            }
        }
        if (first == null) {
/*        int firstX, secondX, firstZ, secondZ, resultX = 0, resultZ = 0;
        firstX = secondX = xCoord;
        firstZ = secondZ = zCoord;
*/
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
            if (worldObj.getTileEntity(targX, yCoord, targZ) instanceof TileEntityChest iterChest) {
                IFacingChest iterInt = (IFacingChest) iterChest;
//                if (iterInt == null) continue;
                if (iterInt.adjachest$doubledWith() == directionToPair.getOpposite()
                    || (!iterInt.adjachest$doubled() /* && iterInt.adjachest$loaded() */)) {
                    if (aValidDouble(targX, yCoord, targZ)) { //wrapped so I can fucking use polymorphism
                        tEC = iterChest;
                        break;
                    }
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
        /*
        if (func_145977_a(firstX, yCoord, firstZ)) {
            tEC = (TileEntityChest) worldObj.getTileEntity(firstX, yCoord, firstZ);
            resultX = firstX - xCoord;
            resultZ = firstZ - zCoord;
        }
        if ((tEC == null || !isPartner(tEC, this)) && func_145977_a(secondX, yCoord, secondZ)) {
            tEC = (TileEntityChest) worldObj.getTileEntity(secondX, yCoord, secondZ);
            resultX = secondX - xCoord;
            resultZ = secondZ - zCoord;
        }
        if (tEC == null || !isPartner(tEC, this)) { // Nondouble.
            adjachest$doubleDirection = -1;
            // worldObj.setBlockMetadataWithNotify(xCoord, yCoord, zCoord, getBlockMetadata(), 3);
            return;
        }
        // else
        if (resultX < 0) {
            adjacentChestXNeg = tEC;
            adjachest$doubleDirection = 0;
        } else if (resultX > 0) {
            adjacentChestXPos = tEC;
            adjachest$doubleDirection = 1;
        } else if (resultZ < 0) {
            adjacentChestZNeg = tEC;
            adjachest$doubleDirection = 2;
        } else if (resultZ > 0) {
            adjacentChestZPos = tEC;
            adjachest$doubleDirection = 3;
        } */
    }

    protected boolean clearTo(ForgeDirection dir) {
        int targX = xCoord + dir.offsetX, targZ = yCoord + dir.offsetZ;
        return !worldObj.getBlock(targX, yCoord, targZ).func_149730_j() &&
            !worldObj.getBlock(targX, yCoord + 1, targZ).func_149730_j() &&
            !worldObj.getBlock(targX + adjachest$doubleDirection.offsetX, yCoord,
                targZ + adjachest$doubleDirection.offsetZ).func_149730_j() &&
            !worldObj.getBlock(targX + adjachest$doubleDirection.offsetX, yCoord + 1,
                targZ + adjachest$doubleDirection.offsetZ).func_149730_j();

    }

    @Inject(method = "writeToNBT(Lnet/minecraft/nbt/NBTTagCompound;)V", at = @At("TAIL"))
    public void adjaChest$writeToNBT(CallbackInfo nameAndNumber, @Local(argsOnly = true, ordinal = 0) NBTTagCompound compound) {
        compound.setInteger("adjachest$doubledir", adjachest$doubleDirection.ordinal());
    }

    @Inject(method = "readFromNBT(Lnet/minecraft/nbt/NBTTagCompound;)V", at = @At("TAIL"))
    public void adjaChest$readFromNBT(CallbackInfo nameAndNumber, @Local(argsOnly = true, ordinal = 0) NBTTagCompound compound) {
        int i = compound.getInteger("adjachest$doubledir");
        adjachest$doubleDirection = (i == 0) ? UNKNOWN : ForgeDirection.getOrientation(i); //don't want Down
        adjachest$finishedLoading = true;
    }

    @Override
    public Packet getDescriptionPacket() {
        NBTTagCompound justOne = new NBTTagCompound();
        justOne.setInteger("adjachest$doubledir", adjachest$doubleDirection.ordinal());
        return new S35PacketUpdateTileEntity(xCoord, yCoord, zCoord,
            worldObj.getBlockMetadata(xCoord, yCoord, zCoord),
            justOne);
    }

    @Override
    public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity pkt) {
        NBTTagCompound justOne = pkt.func_148857_g();
        int i = justOne.getInteger("adjachest$doubledir");
        adjachest$doubleDirection = (i == 0) ? UNKNOWN : ForgeDirection.getOrientation(i); //don't want Down
        adjachest$finishedLoading = true;
    }

    /*
    public boolean isPartner(TileEntityChest tEC, TileEntity tE) {
        if (tEC.adjacentChestXNeg != null) return tEC.adjacentChestXNeg == tE;
        if (tEC.adjacentChestZNeg != null) return tEC.adjacentChestZNeg == tE;
        if (tEC.adjacentChestZPos != null) return tEC.adjacentChestZPos == tE;
        return tEC.adjacentChestXPos == tE;
    }
    */


    /*
    public int adjaChest$getBlockMetadata() { // only really used for render
        int i = super.getBlockMetadata();
        return switch (i) {
            case 6, 7, 8, 9 -> i - 4;
            case 10, 11, 12, 13 -> i - 8;
            default -> i;
        };
    }

    public int adjaChest$getTrueBlockMetadata() {
        return super.getBlockMetadata();
    } */

    @Override
    public ForgeDirection adjachest$opensTo() { return getOrientation(getBlockMetadata()); }

    @Override
    public ForgeDirection adjachest$doubledWith() {
        return adjachest$doubleDirection;
    }

    @Override
    public boolean adjachest$doubled() {
        return adjachest$doubleDirection != UNKNOWN;
    }

    @Override
    public void adjachest$setDouble(ForgeDirection dir) {
        adjachest$doubleDirection = dir;
    }

    @Override
    public boolean adjachest$loaded() {
        return adjachest$finishedLoading;
    }

    @Override //gotta do this to keep a line of three from switching visual doubling
    public void adjachest$setLoaded() {
        adjachest$finishedLoading = true;
    }

    public void adjachest$considerInvalidation(TileEntityChest calleeChest, TileEntityChest th, ForgeDirection dir) {
        if (th.isInvalid()) calleeChest.adjacentChestChecked = false;
        else if (calleeChest.adjacentChestChecked) {
            switch (dir)
            {
                case SOUTH:
                    if (calleeChest.adjacentChestZPos != th)
                        calleeChest.adjacentChestChecked = false;
                    break;
                case WEST:
                    if (calleeChest.adjacentChestXNeg != th)
                        calleeChest.adjacentChestChecked = false;
                    break;
                case NORTH:
                    if (calleeChest.adjacentChestZNeg != th)
                        calleeChest.adjacentChestChecked = false;
                    break;
                case EAST:
                    if (calleeChest.adjacentChestXPos != th)
                        calleeChest.adjacentChestChecked = false;
                default:
            }
        }
    }
}
