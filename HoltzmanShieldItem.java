package dune.holtzman;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class HoltzmanShieldItem extends Item {
    public HoltzmanShieldItem(Properties props) {
        super(props.stacksTo(1));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatable("tooltip.holtzman.line1"));
        tip.add(Component.translatable("tooltip.holtzman.line2"));
    }
}
