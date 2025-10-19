package darkbum.saltymod.block;

import java.util.ArrayList;
import java.util.Random;

import darkbum.saltymod.init.ModAchievementList;
import net.minecraft.block.material.MapColor;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

import static darkbum.saltymod.block.BlockSaltBlock.*;
import static darkbum.saltymod.util.BlockUtils.*;

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
        iconTop = icon.registerIcon("saltymod:salt_lake_ore_top");
        this.blockIcon = iconTop;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int meta) {
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

    // --- Drop behavior: block drops itself, quantity 1, ignore fortune ---

    @Override
    public Item getItemDropped(int meta, Random random, int fortune) {
        return Item.getItemFromBlock(this);
    }

    @Override
    public int damageDropped(int meta) {
        return meta;
    }

    @Override
    public int quantityDropped(Random random) {
        return 1;
    }

    @Override
    public int quantityDroppedWithBonus(int fortune, Random random) {
        return 1;
    }

    @Override
    public ArrayList<ItemStack> getDrops(World world, int x, int y, int z, int metadata, int fortune) {
        ArrayList<ItemStack> drops = new ArrayList<ItemStack>(1);
        drops.add(new ItemStack(this, 1, damageDropped(metadata)));
        return drops;
    }

    @Override
    public int getExpDrop(IBlockAccess world, int meta, int fortune) {
        return 1; // set to 0 if you don't want XP anymore
    }

    @Override
    public void onEntityWalking(World world, int x, int y, int z, Entity entity) {
        handleEntityWalkingSaltVulnerableUpdate(world, x, y, z, entity, this);
        if (entity instanceof EntityPlayer) {
            ((EntityPlayer) entity).addStat(ModAchievementList.nav_salt_lake, 1);
        }
    }
}
