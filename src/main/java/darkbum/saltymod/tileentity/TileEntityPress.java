package darkbum.saltymod.tileentity;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import darkbum.saltymod.util.PressingRecipe;
import darkbum.saltymod.util.MachineUtilRegistry;
import darkbum.saltymod.init.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import darkbum.saltymod.util.PressingRecipe;
import net.minecraftforge.common.util.ForgeDirection;

public class TileEntityPress extends TileEntity implements ISidedInventory {

    @SuppressWarnings("unused")
    private String inventoryName;

    private ItemStack[] inventory = new ItemStack[4];

    public int pressingTime = 0;

    public boolean isHeaterNearby = false;

    public boolean isMillNearby = false;

    private static final int[] slotsTop = new int[]{0};
    private static final int[] slotsBottom = new int[]{1, 2};
    private static final int[] slotVessel = new int[]{3};

    public String getInventoryName() {
        return hasCustomInventoryName() ? this.inventoryName : "container.press";
    }

    public boolean hasCustomInventoryName() {
        return (this.inventoryName != null && !this.inventoryName.isEmpty());
    }

    @Override
    public int getSizeInventory() {
        return inventory.length;
    }

    @Override
    public ItemStack getStackInSlot(int index) {
        return inventory[index];
    }

    @Override
    public ItemStack decrStackSize(int index, int count) {
        if (inventory[index] != null) {
            ItemStack itemstack;
            if (inventory[index].stackSize <= count) {
                itemstack = inventory[index];
                inventory[index] = null;
            } else {
                itemstack = inventory[index].splitStack(count);
                if (inventory[index].stackSize == 0)
                    inventory[index] = null;
            }
            return itemstack;
        }
        return null;
    }

    @Override
    public void setInventorySlotContents(int index, ItemStack stack) {
        inventory[index] = stack;
        if (stack != null && stack.stackSize > getInventoryStackLimit()) {
            stack.stackSize = getInventoryStackLimit();
        }
    }

    @Override
    public int getInventoryStackLimit() {
        return 64;
    }

    @Override
    public boolean isUseableByPlayer(EntityPlayer player) {
        return worldObj.getTileEntity(xCoord, yCoord, zCoord) == this &&
            player.getDistanceSq(xCoord + 0.5D, yCoord + 0.5D, zCoord + 0.5D) <= 64.0D;
    }

    @Override
    public void openInventory() {
    }

    @Override
    public void closeInventory() {
    }

    @Override
    public boolean isItemValidForSlot(int index, ItemStack stack) {
        if (index == 0) {
            return true;
        } else if (index == 3) {
            return MachineUtilRegistry.isValidVessel(stack);
        }
        return false;
    }

    @Override
    public int[] getAccessibleSlotsFromSide(int side) {
        if (side == 0) {
            return slotsBottom;
        } else if (side == 1) {
            return slotsTop;
        } else return slotVessel;
    }

    public boolean canInsertItem(int index, ItemStack itemstack, int side) {
        return this.isItemValidForSlot(index, itemstack);
    }

    @Override
    public boolean canExtractItem(int index, ItemStack itemstack, int side) {
        return index != 0;
    }

    @Override
    public ItemStack getStackInSlotOnClosing(int index) {
        if (inventory[index] != null) {
            ItemStack stack = inventory[index];
            inventory[index] = null;
            return stack;
        }
        return null;
    }

    @SideOnly(Side.CLIENT)
    public int getPressProgressScale(int scale) {
        return pressingTime * scale / 100;
    }

    public boolean isRunning() {
        return pressingTime > 0;
    }

    @Override
    public void updateEntity() {
        boolean updated = false;
        checkForHeater();
        checkForMill();

        if (!worldObj.isRemote) {
            if (canRun()) {
                pressingTime++;
                if (pressingTime >= 100) {
                    pressingTime = 0;
                    pressItems();
                    updated = true;
                }
            } else {
                if (pressingTime != 0) {
                    pressingTime = 0;
                    updated = true;
                }
            }

            if (updated || pressingTime > 0) {
                markDirty();
                worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
            }
        }
    }

    private boolean canRun() {
        ItemStack input = inventory[0];
        if (input == null) return false;

        ItemStack vessel = inventory[3];
        PressingRecipe.PressRecipe recipe = PressingRecipe.pressing().getRecipeFor(input, vessel);
        if (recipe == null) return false;

        if (!isHeaterRequirementMet(recipe)) return false;
        if (!isMillRequirementMet(recipe)) return false;
        if (!isVesselRequirementMet(recipe, vessel)) return false;
        if (!canAcceptOutput(inventory[1], recipe.output1())) return false;
        return canAcceptOutput(inventory[2], recipe.output2());
    }

    private boolean isHeaterRequirementMet(PressingRecipe.PressRecipe recipe) {
        return recipe.requiresHeater() == isHeaterNearby;
    }

    private boolean isMillRequirementMet(PressingRecipe.PressRecipe recipe) {
        return recipe.requiresMill() == isMillNearby;
    }

    private boolean isVesselRequirementMet(PressingRecipe.PressRecipe recipe, ItemStack vessel) {
        if (recipe.vesselItem() == null) return true;
        if (vessel == null) return false;
        return MachineUtilRegistry.isValidVessel(vessel) && vessel.getItem() == recipe.vesselItem().getItem();
    }

    private boolean canAcceptOutput(ItemStack currentStack, ItemStack output) {
        if (output == null) return true;
        if (currentStack == null) return true;

        if (!currentStack.isItemEqual(output)) return false;
        return currentStack.stackSize + output.stackSize <= currentStack.getMaxStackSize();
    }

    public void pressItems() {
        ItemStack input = inventory[0];
        if (input == null) return;

        ItemStack vessel = inventory[3];
        PressingRecipe.PressRecipe recipe = PressingRecipe.pressing().getRecipeFor(input, vessel);
        if (recipe == null) return;

        if (recipe.output1() != null) {
            if (inventory[1] == null) {
                inventory[1] = recipe.output1().copy();
            } else {
                inventory[1].stackSize += recipe.output1().stackSize;
            }
        }

        if (recipe.output2() != null) {
            if (inventory[2] == null) {
                inventory[2] = recipe.output2().copy();
            } else {
                inventory[2].stackSize += recipe.output2().stackSize;
            }
        }

        inventory[0].stackSize--;
        if (inventory[0].stackSize <= 0) inventory[0] = null;

        if (recipe.vesselItem() != null && inventory[3] != null && MachineUtilRegistry.isValidVessel(inventory[3])) {
            if (inventory[3].getItem() == recipe.vesselItem().getItem()) {
                inventory[3].stackSize--;
                if (inventory[3].stackSize <= 0) {
                    inventory[3] = null;
                }
            }
        }
    }

    private void checkForHeater() {
        isHeaterNearby = false;

        int[][] directions = {
            {0, 1, 0},
            {0, -1, 0},
            {1, 0, 0},
            {-1, 0, 0},
            {0, 0, 1},
            {0, 0, -1}
        };

        for (int[] dir : directions) {
            int dx = dir[0];
            int dy = dir[1];
            int dz = dir[2];

            Block neighborBlock = worldObj.getBlock(xCoord + dx, yCoord + dy, zCoord + dz);
            if (MachineUtilRegistry.isValidHeater(neighborBlock)) {
                isHeaterNearby = true;
                return;
            }
        }
    }

    private void checkForMill() {
        isMillNearby = false;

        int blockMeta = worldObj.getBlockMetadata(xCoord, yCoord, zCoord);

        int targetSide = -1;

        targetSide = switch (blockMeta) {
            case 0 -> 2;
            case 1 -> 5;
            case 2 -> 3;
            case 3 -> 4;
            default -> targetSide;
        };

        if (targetSide != -1) {
            Block neighborBlock = worldObj.getBlock(xCoord + ForgeDirection.VALID_DIRECTIONS[targetSide].offsetX,
                yCoord + ForgeDirection.VALID_DIRECTIONS[targetSide].offsetY,
                zCoord + ForgeDirection.VALID_DIRECTIONS[targetSide].offsetZ);

            if (neighborBlock != null && neighborBlock == ModBlocks.mill) {
                int neighborMeta = worldObj.getBlockMetadata(xCoord + ForgeDirection.VALID_DIRECTIONS[targetSide].offsetX,
                    yCoord + ForgeDirection.VALID_DIRECTIONS[targetSide].offsetY,
                    zCoord + ForgeDirection.VALID_DIRECTIONS[targetSide].offsetZ);

                if ((blockMeta == 0 && neighborMeta == 6) ||
                    (blockMeta == 1 && neighborMeta == 7) ||
                    (blockMeta == 2 && neighborMeta == 4) ||
                    (blockMeta == 3 && neighborMeta == 5)) {
                    isMillNearby = true;
                }
            }
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        NBTTagList itemList = tag.getTagList("Items", 10);
        inventory = new ItemStack[getSizeInventory()];

        for (int i = 0; i < itemList.tagCount(); i++) {
            NBTTagCompound itemTag = itemList.getCompoundTagAt(i);
            int slot = itemTag.getByte("Slot") & 255;
            if (slot < inventory.length) {
                inventory[slot] = ItemStack.loadItemStackFromNBT(itemTag);
            }
        }

        pressingTime = tag.getShort("CookTime");
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setShort("CookTime", (short) pressingTime);
        NBTTagList itemList = new NBTTagList();

        for (int i = 0; i < inventory.length; i++) {
            if (inventory[i] != null) {
                NBTTagCompound itemTag = new NBTTagCompound();
                itemTag.setByte("Slot", (byte) i);
                inventory[i].writeToNBT(itemTag);
                itemList.appendTag(itemTag);
            }
        }
        tag.setTag("Items", itemList);
    }

    @Override
    public Packet getDescriptionPacket() {
        NBTTagCompound tag = new NBTTagCompound();
        writeToNBT(tag);
        return new S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, 1, tag);
    }

    @Override
    public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity packet) {
        readFromNBT(packet.func_148857_g());
    }
}
