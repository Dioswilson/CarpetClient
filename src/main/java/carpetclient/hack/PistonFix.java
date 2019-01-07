package carpetclient.hack;

import carpetclient.mixins.IMixinTileEntityPiston;
import carpetclient.mixins.MixinWorldClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityPiston;
import net.minecraft.util.ITickable;
import net.minecraft.util.ReportedException;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Iterator;

public class PistonFix {
    private static PistonFix instance;
    public static boolean pushPlayersNow;
    private static boolean pistonFix;
    private static boolean firstPistonPush;

    static {
        instance = new PistonFix();
    }

    public static void processPacket(PacketBuffer data) {
        System.out.println("-------------------------------------------------------------------- " + pistonFix);
        if (pistonFix) {
            System.out.println("packet clogging");
            instance.fixTileEntitys();
        }
        pistonFix = true;
    }

    public static void movePlayer() {
        if (pushPlayersNow) {
            instance.move();
            firstPistonPush = false;
        }
    }

    public static void resetBools() {
        firstPistonPush = true;
        if(pistonFix) System.out.println("pistonFix " + pistonFix);
        pistonFix = false;
    }

    public static void clearBlockLocation(BlockPos pos) {
//        TileEntity tileentity = Minecraft.getMinecraft().world.getTileEntity(pos);
//        if (tileentity != null && tileentity instanceof TileEntityPiston && ((IMixinTileEntityPiston) tileentity).getProgress() < 1.0) {
//            int iter = 1;
//
//            if (((IMixinTileEntityPiston) tileentity).getProgress() == 0.0) iter = 2;
//
//            for (int i = 0; i < iter; i++) {
//                ((TileEntityPiston) tileentity).update();
//            }
//        }
    }

    private void move() {
        System.out.println("move player");
        Minecraft.getMinecraft().player.onUpdate();
    }

    private void fixTileEntitys() {
        World world = Minecraft.getMinecraft().world;
        Iterator<TileEntity> iterator = world.tickableTileEntities.iterator();
        pushPlayersNow = true;

        while (iterator.hasNext()) {
            TileEntity tileentity = iterator.next();

            if (!(tileentity instanceof TileEntityPiston)) continue;

            if (!tileentity.isInvalid() && tileentity.hasWorld()) {
                BlockPos blockpos = tileentity.getPos();

//                if (world.isBlockLoaded(blockpos) && world.worldBorder.contains(blockpos))
                if (world.isBlockLoaded(blockpos)) {
                    try {
                        ((ITickable) tileentity).update();
                    } catch (Throwable throwable) {
                        CrashReport crashreport2 = CrashReport.makeCrashReport(throwable, "Ticking block entity");
                        CrashReportCategory crashreportcategory2 = crashreport2.makeCategory("Block entity being ticked");
                        tileentity.addInfoToCrashReport(crashreportcategory2);
                        throw new ReportedException(crashreport2);
                    }
                }
            }

            if (tileentity.isInvalid()) {
                iterator.remove();
                world.loadedTileEntityList.remove(tileentity);

                if (world.isBlockLoaded(tileentity.getPos())) {
                    world.getChunkFromBlockCoords(tileentity.getPos()).removeTileEntity(tileentity.getPos());
                }
            }
        }

        pushPlayersNow = false;
    }
}
