package darkbum.saltymod.block;

import java.util.Random;

import darkbum.saltymod.util.BlockUtils;
import net.minecraft.block.material.MapColor;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import darkbum.saltymod.init.ModAchievementList;

import static darkbum.saltymod.block.BlockSaltBlock.*;
import static darkbum.saltymod.util.BlockUtils.*;

/**
 * Block class for the salt lake block.
 * Now uses the TOP texture on all faces.
 */
public class BlockSaltLake extends BlockSaltOre {

    @SideOnly(Side.CLIENT)
    private IIcon iconTop;

    public BlockSaltLake(String name, CreativeTabs tab) {
        super(name, tab);
        setTickRandomly(true);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister icon) {
        // Only register & use the top texture, and assign it as the block's default icon
        iconTop = icon.registerIcon("saltymod:salt_lake_ore_top");
        this.blockIcon = iconTop;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int meta) {
        // Always use the top texture on every side
        return iconTop != null ? iconTop : this.blockIcon;
    }

    @Override
    public MapColor getMapColor(int meta) {
        return MapColor.quartzColor;
    }

    @Override
    public void updateTick(World world, int x, int y, int z, Random rand) {
        if (world.isRemote) return;

        checkAndDamageNearbyEntities(world, x, y, z, this);
        tryMeltIceAndSnow(world, x, y, z, rand, this);
        crystal = false;
    }

    @Override
    public void onEntityWalking(World world, int x, int y, int z, Entity entity) {
        handleEntityWalkingSaltVulnerableUpdate(world, x, y, z, entity, this);
        if (entity instanceof EntityPlayer) {
            ((EntityPlayer) entity).addStat(ModAchievementList.nav_salt_lake, 1);
        }
    }
}
