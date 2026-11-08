package dune.holtzman;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

public class SpiceBurnerMenu extends AbstractContainerMenu {
    private final ContainerLevelAccess access;
    private final ContainerData data;

    public SpiceBurnerMenu(int id, Inventory inv) {
        this(id, inv, SpiceBurnerBlockEntity.handler(() -> {}), new SimpleContainerData(2), ContainerLevelAccess.NULL);
    }

    public SpiceBurnerMenu(int id, Inventory pinv, ItemStackHandler h, ContainerData data, ContainerLevelAccess access) {
        super(CreateHeat.SPICE_BURNER_MENU.get(), id);
        this.access = access;
        this.data = data;
        addSlot(new SlotItemHandler(h, 0, 80, 20));
        for (int r = 0; r < 3; r++) for (int c = 0; c < 9; c++) addSlot(new Slot(pinv, 9 + r * 9 + c, 8 + c * 18, 51 + r * 18));
        for (int c = 0; c < 9; c++) addSlot(new Slot(pinv, c, 8 + c * 18, 109));
        addDataSlots(data);
    }

    public int burn() { return data.get(0); }
    public int total() { return data.get(1); }

    @Override
    public boolean stillValid(Player p) {
        return stillValid(access, p, CreateHeat.SPICE_BURNER.get());
    }

    @Override
    public ItemStack quickMoveStack(Player p, int idx) {
        ItemStack ret = ItemStack.EMPTY;
        Slot s = slots.get(idx);
        if (s != null && s.hasItem()) {
            ItemStack st = s.getItem();
            ret = st.copy();
            if (idx == 0) {
                if (!moveItemStackTo(st, 1, slots.size(), true)) return ItemStack.EMPTY;
            } else if (SpiceBurnerBlockEntity.fuelTicks(st) > 0) {
                if (!moveItemStackTo(st, 0, 1, false)) return ItemStack.EMPTY;
            } else if (idx < 28) {
                if (!moveItemStackTo(st, 28, 37, false)) return ItemStack.EMPTY;
            } else if (!moveItemStackTo(st, 1, 28, false)) return ItemStack.EMPTY;
            if (st.isEmpty()) s.setByPlayer(ItemStack.EMPTY); else s.setChanged();
            if (st.getCount() == ret.getCount()) return ItemStack.EMPTY;
            s.onTake(p, st);
        }
        return ret;
    }
}
