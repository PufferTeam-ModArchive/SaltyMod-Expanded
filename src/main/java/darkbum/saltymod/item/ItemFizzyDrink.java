package darkbum.saltymod.item;

import java.util.List;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import darkbum.saltymod.common.config.ModConfigurationItems;
import darkbum.saltymod.init.ModAchievementList;

import static darkbum.saltymod.util.ItemUtils.addItemTooltip;
import static darkbum.saltymod.util.ItemUtils.variantsFizzyDrink;

public class ItemFizzyDrink extends ItemSaltFood {

    public ItemFizzyDrink(String baseName, CreativeTabs tab) {
        super(baseName);
        setCreativeTab(tab);
        setAlwaysEdible();
        variantsFizzyDrink(this);
    }

    @Override
    @SuppressWarnings("rawtypes")
    public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean advanced) {
        addItemTooltip(stack, list);
    }

    /** Call super first so AppleCore/Spice-of-Life see the meal. Then do our extras. */
    @Override
    public ItemStack onEaten(ItemStack stack, World world, EntityPlayer player) {
        // vanilla path: applies FoodStats.addStats(this, stack) and decrements stack
        ItemStack result = super.onEaten(stack, world, player);

        if (!world.isRemote) {
            if (ModConfigurationItems.fizzyDrinkEffect) {
                player.clearActivePotions();
            } else {
                player.curePotionEffects(new ItemStack(Items.milk_bucket));
            }
            if (player.isBurning()) {
                player.addStat(ModAchievementList.consume_fizzy_drink, 1);
                player.extinguish();
            }
        }

        // Return a glass bottle like vanilla potions do
        if (!player.capabilities.isCreativeMode) {
            if (result.stackSize <= 0) {
                return new ItemStack(Items.glass_bottle);
            } else {
                ItemStack bottle = new ItemStack(Items.glass_bottle);
                if (!player.inventory.addItemStackToInventory(bottle)) {
                    player.dropPlayerItemWithRandomChoice(bottle, false);
                }
            }
        }
        return result;
    }
}
