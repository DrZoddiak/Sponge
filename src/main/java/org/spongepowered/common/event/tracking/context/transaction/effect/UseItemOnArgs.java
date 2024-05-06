package org.spongepowered.common.event.tracking.context.transaction.effect;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;

public record UseItemOnArgs(
    UseOnContext context,
    ItemStack itemStack,
    ItemStackSnapshot itemStackSnapshot,
    ServerPlayer player,
    boolean isCreative
) implements ProcessingSideEffect.Args {
}
