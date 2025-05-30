package invalid.myask.adjachest.ducks;

import net.minecraftforge.common.util.ForgeDirection;

public interface IFacingChest {
    ForgeDirection adjachest$opensTo();
    ForgeDirection adjachest$doubledWith();
    void adjachest$setDouble(ForgeDirection dir);
    boolean adjachest$doubled();
    boolean adjachest$loaded();

    void adjachest$setLoaded();
}
