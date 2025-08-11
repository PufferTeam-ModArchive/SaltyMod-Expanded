package darkbum.saltymod.item;

import java.util.*;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Optional;
import darkbum.saltymod.potion.ProbablePotionEffect;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.*;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;
import squeek.applecore.api.food.FoodValues;
import squeek.applecore.api.food.IEdible;

/**
 * AppleCore/Spice of Life–compatible food base:
 * - Let vanilla/AppleCore apply hunger via addStats(this, stack)
 * - Provide per-variant values via getHealAmount/getSaturationModifier + IEdible
 * - Apply custom effects/containers in onFoodEaten (called by super.onEaten)
 */
@Optional.Interface(modid = "AppleCore", iface = "squeek.applecore.api.food.IEdible")
public abstract class ItemSaltFoodBase extends ItemFood implements IEdible {

    private static class Variant {
        public final String unlocalizedName, textureName;
        public final List<ItemStack> containers;
        public final int heal;
        public final float saturation;
        public final int maxStackSize;
        public final boolean isDogFood;
        public final EnumAction useAction;
        public final ProbablePotionEffect[] effects;
        public IIcon icon;

        public Variant(String unlocalizedName, String textureName, int heal,
                       float saturation, boolean isDogFood, int maxStackSize, List<ItemStack> containers,
                       EnumAction useAction, ProbablePotionEffect... effects) {
            this.unlocalizedName = unlocalizedName;
            this.textureName = textureName;
            this.heal = heal;
            this.saturation = saturation;
            this.isDogFood = isDogFood;
            this.maxStackSize = maxStackSize > 0 ? maxStackSize : 64;
            this.containers = containers;
            this.useAction = useAction != null ? useAction : EnumAction.eat;
            this.effects = effects != null ? effects : new ProbablePotionEffect[0];
        }
    }

    private final Map<Integer, Variant> variants = new HashMap<>();

    public ItemSaltFoodBase(String baseName) {
        super(0, 0.0f, false); // per-variant values supplied via overrides
        setUnlocalizedName(baseName);
        setHasSubtypes(true);
        setMaxDamage(0);
    }

    /* ---------------- AppleCore / SoL integration ---------------- */

    @Override
    public FoodValues getFoodValues(ItemStack stack) {
        Variant v = variants.get(stack.getItemDamage());
        return (Loader.isModLoaded("AppleCore") && v != null)
            ? new FoodValues(v.heal, v.saturation)
            : null; // AppleCore absent -> null is fine
    }

    // === ItemFood actually calls these in 1.7.10 (SRG names) ===
    @Override
    public int func_150905_g(ItemStack stack) {
        Variant v = variants.get(stack.getItemDamage());
        return v != null ? v.heal : 0;
    }

    @Override
    public float func_150906_h(ItemStack stack) {
        Variant v = variants.get(stack.getItemDamage());
        return v != null ? v.saturation : 0F;
    }

    // === Keep your old helpers for compatibility (NO @Override) ===
    public int getHealAmount(ItemStack stack) {
        return func_150905_g(stack);
    }

    public float getSaturationModifier(ItemStack stack) {
        return func_150906_h(stack);
    }

    @Override
    public EnumAction getItemUseAction(ItemStack stack) {
        Variant v = variants.get(stack.getItemDamage());
        return v != null ? v.useAction : EnumAction.none;
    }

    /**
     * IMPORTANT: don’t add hunger here manually.
     * Call super so vanilla path runs: it does addStats(this, stack) which AppleCore hooks,
     * then calls onFoodEaten where we apply effects/containers.
     */
    @Override
    public ItemStack onEaten(ItemStack stack, World world, EntityPlayer player) {
        // super handles: stackSize--, FoodStats.addStats(this, stack) (AppleCore/SoL see this), then calls onFoodEaten
        ItemStack result = super.onEaten(stack, world, player);

        // Handle containers AFTER super so we don’t interfere with AppleCore/SoL
        if (!world.isRemote) {
            Variant v = variants.get(stack.getItemDamage());
            if (v != null && v.containers != null) {
                for (ItemStack container : v.containers) {
                    ItemStack copy = container.copy();
                    if (!player.inventory.addItemStackToInventory(copy)) {
                        world.spawnEntityInWorld(new EntityItem(world, player.posX, player.posY, player.posZ, copy));
                    }
                }
            }
        }
        return result;
    }

    /**
     * Apply custom potion effects here; this is invoked by super.onEaten() after hunger is applied.
     */
    @Override
    protected void onFoodEaten(ItemStack stack, World world, EntityPlayer player) {
        super.onFoodEaten(stack, world, player);
        if (!world.isRemote) {
            Variant v = variants.get(stack.getItemDamage());
            if (v != null) {
                for (ProbablePotionEffect effect : v.effects) {
                    effect.procEffect(player, itemRand);
                }
            }
        }
    }

    /* ---------------- Variants & visuals ---------------- */

    public ItemSaltFoodBase addVariant(int meta, String unlocalizedName, String textureName,
                                       int heal, float saturation, boolean isDogFood, int maxStackSize, List<ItemStack> containers,
                                       EnumAction useAction, ProbablePotionEffect... effects) {
        variants.put(meta, new Variant(unlocalizedName, textureName, heal, saturation,
            isDogFood, maxStackSize, containers, useAction, effects));
        return this;
    }

    public ItemSaltFoodBase addVariant(int meta, String unlocalizedName, String textureName,
                                       int heal, float saturation, boolean isDogFood, int maxStackSize, List<ItemStack> containers,
                                       EnumAction useAction) {
        return addVariant(meta, unlocalizedName, textureName, heal, saturation, isDogFood, maxStackSize, containers, useAction, (ProbablePotionEffect[]) null);
    }

    public ItemSaltFoodBase addVariant(int meta, String unlocalizedName, String textureName,
                                       int heal, float saturation, boolean isDogFood) {
        return addVariant(meta, unlocalizedName, textureName, heal, saturation, isDogFood, 64, null, EnumAction.eat);
    }

    public ItemSaltFoodBase addVariant(int meta, String unlocalizedName, String textureName,
                                       int heal, float saturation, boolean isDogFood,
                                       ProbablePotionEffect... effects) {
        return addVariant(meta, unlocalizedName, textureName, heal, saturation, isDogFood, 64, null, EnumAction.eat, effects);
    }

    public ItemSaltFoodBase addVariant(int meta, String unlocalizedName, String textureName,
                                       int heal, float saturation, boolean isDogFood, int maxStackSize, List<ItemStack> containers,
                                       ProbablePotionEffect... effects) {
        return addVariant(meta, unlocalizedName, textureName, heal, saturation, isDogFood, maxStackSize, containers, EnumAction.eat, effects);
    }

    @Override
    public String getUnlocalizedName(ItemStack stack) {
        Variant v = variants.get(stack.getItemDamage());
        return v != null ? "item." + v.unlocalizedName : "item.unknown_saltfood";
    }

    @Override
    public String getUnlocalizedName() {
        return getUnlocalizedName(new ItemStack(this, 1, 0));
    }

    @Override
    public ItemSaltFoodBase setCreativeTab(CreativeTabs tab) {
        super.setCreativeTab(tab);
        return this;
    }

    @Override
    public void registerIcons(IIconRegister iconRegister) {
        for (Map.Entry<Integer, Variant> entry : variants.entrySet()) {
            Variant v = entry.getValue();
            if (v != null) {
                v.icon = iconRegister.registerIcon("saltymod:" + v.textureName);
            }
        }
    }

    @Override
    public IIcon getIconFromDamage(int meta) {
        Variant v = variants.get(meta);
        return v != null ? v.icon : super.getIconFromDamage(meta);
    }

    @Override
    public int getMetadata(int damage) {
        return damage;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void getSubItems(Item item, CreativeTabs tab, List list) {
        for (Integer meta : variants.keySet()) {
            list.add(new ItemStack(this, 1, meta));
        }
    }

    @Override
    public int getItemStackLimit(ItemStack stack) {
        Variant v = variants.get(stack.getItemDamage());
        return v != null ? v.maxStackSize : super.getItemStackLimit(stack);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean advanced) {
        Variant v = variants.get(stack.getItemDamage());
        if (v != null) {
            for (ProbablePotionEffect effect : v.effects) {
                String tooltip = effect.addTooltip();
                if (tooltip != null) list.add(tooltip);
            }
        }
    }
}
