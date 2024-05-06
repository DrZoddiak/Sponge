package org.spongepowered.common.event.tracking.context.transaction.effect;

import net.minecraft.world.InteractionResult;
import org.spongepowered.common.event.tracking.context.transaction.EffectTransactor;
import org.spongepowered.common.event.tracking.context.transaction.inventory.PlayerInventoryTransaction;
import org.spongepowered.common.event.tracking.context.transaction.pipeline.InteractItemPipeline;

public final class InteractionUseItemOnEffect implements ProcessingSideEffect.Simple<InteractItemPipeline<UseItemOnArgs>, UseItemOnArgs, InteractionResult> {

    private static final class Holder {
        static final InteractionUseItemOnEffect INSTANCE = new InteractionUseItemOnEffect();
    }

    public static InteractionUseItemOnEffect getInstance() {
        return InteractionUseItemOnEffect.Holder.INSTANCE;
    }

    @Override
    public EffectResult<InteractionResult> processSideEffect(
        InteractItemPipeline<UseItemOnArgs> pipeline, InteractionResult oldState, UseItemOnArgs args
    ) {
        // Run vanilla change
        InteractionResult result;
        final var stack = args.itemStack();
        if (args.isCreative()) {
            var i = stack.getCount();
            result = stack.useOn(args.context());
            stack.setCount(i);
        } else {
            result = stack.useOn(args.context());
        }

        final var player = args.player();
        pipeline.transactor().logPlayerInventoryChange(player, PlayerInventoryTransaction.EventCreator.STANDARD);
        try (EffectTransactor ignored = BroadcastInventoryChangesEffect.transact(pipeline.transactor())) {
            player.containerMenu.broadcastChanges();
        }
        return new EffectResult<>(result, true);
    }
}
