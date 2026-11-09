package dune.holtzman;

import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.ArrayList;
import java.util.List;

public class SpiceBurnerBlockEntity extends BlockEntity implements MenuProvider {
    public static final int MELANGE_TICKS = 3200, COMPACTED_TICKS = 12000;

    public static int fuelTicks(ItemStack s) {
        ResourceLocation k = BuiltInRegistries.ITEM.getKey(s.getItem());
        if (!k.getNamespace().equals("minecraft")) return 0;
        return switch (k.getPath()) {
            case "melange" -> MELANGE_TICKS;
            case "compacted_melange" -> COMPACTED_TICKS;
            default -> 0;
        };
    }

    public static ItemStackHandler handler(Runnable onChange) {
        return new ItemStackHandler(1) {
            @Override
            public boolean isItemValid(int slot, ItemStack st) {
                return fuelTicks(st) > 0;
            }

            @Override
            protected void onContentsChanged(int slot) {
                onChange.run();
            }
        };
    }

    public final ItemStackHandler inv = handler(this::changed);
    public int burn, total, last;
    public final ContainerData data = new ContainerData() {
        @Override public int get(int i) { return i == 0 ? burn : total; }
        @Override public void set(int i, int v) { if (i == 0) burn = v; else total = v; }
        @Override public int getCount() { return 2; }
    };

    public SpiceBurnerBlockEntity(BlockPos pos, BlockState state) {
        super(CreateHeat.SPICE_BURNER_BE.get(), pos, state);
    }

    private void changed() {
        setChanged();
        int c = inv.getStackInSlot(0).getCount();
        if (level != null && !level.isClientSide && c > last) {
            level.playSound(null, worldPosition, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.9f, 0.7f);
            level.playSound(null, worldPosition, SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 1.0f, 0.6f);
        }
        last = c;
    }

    public void tick() {
        if (level == null || level.isClientSide) return;
        if (burn > 0) burn--;
        if (burn <= 0) {
            int f = fuelTicks(inv.getStackInSlot(0));
            if (f > 0) {
                inv.extractItem(0, 1, false);
                burn = total = f;
                level.playSound(null, worldPosition, SoundEvents.BLAZE_SHOOT, SoundSource.BLOCKS, 0.5f, 0.6f);
                if (f >= COMPACTED_TICKS) award();
            } else {
                total = 0;
            }
        }
        BlazeBurnerBlock.HeatLevel want = burn <= 0 ? BlazeBurnerBlock.HeatLevel.NONE : (total >= COMPACTED_TICKS ? BlazeBurnerBlock.HeatLevel.SEETHING : BlazeBurnerBlock.HeatLevel.KINDLED);   // compacted melange = superheated
        BlockState st = getBlockState();
        if (st.getValue(SpiceBurnerBlock.HEAT) != want) level.setBlock(worldPosition, st.setValue(SpiceBurnerBlock.HEAT, want), 3);
        if (burn > 0 && burn % 100 == 0) level.playSound(null, worldPosition, SoundEvents.CAMPFIRE_CRACKLE, SoundSource.BLOCKS, 0.6f, 0.8f);
        if (burn % 20 == 0) setChanged();
    }

    private void award() {
        Player p = level.getNearestPlayer(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, 8, false);
        if (!(p instanceof ServerPlayer sp)) return;
        AdvancementHolder h = sp.server.getAdvancements().get(ResourceLocation.parse("minecraft:dune/power_density"));
        if (h == null) return;
        AdvancementProgress prog = sp.getAdvancements().getOrStartProgress(h);
        List<String> rem = new ArrayList<>();
        prog.getRemainingCriteria().forEach(rem::add);
        for (String c : rem) sp.getAdvancements().award(h, c);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider reg) {
        super.saveAdditional(tag, reg);
        tag.put("Inv", inv.serializeNBT(reg));
        tag.putInt("Burn", burn);
        tag.putInt("Total", total);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider reg) {
        super.loadAdditional(tag, reg);
        inv.deserializeNBT(reg, tag.getCompound("Inv"));
        burn = tag.getInt("Burn");
        total = tag.getInt("Total");
        last = inv.getStackInSlot(0).getCount();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.createheat.spice_burner");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory pinv, Player p) {
        return new SpiceBurnerMenu(id, pinv, inv, data, ContainerLevelAccess.create(level, worldPosition));
    }
}
