package org.spongepowered.common.event.tracking.context.transaction.effect;

import net.minecraft.world.InteractionResult;
import org.spongepowered.common.event.tracking.context.transaction.EffectTransactor;
import org.spongepowered.common.event.tracking.context.transaction.inventory.PlayerInventoryTransaction;
import org.spongepowered.common.event.tracking.context.transaction.pipeline.InteractItemPipeline;

public class UseItemEffect implements ProcessingSideEffect.Simple<InteractItemPipeline<UseItemArgs>, UseItemArgs, InteractionResult> {

    private static final class Holder {
        static final UseItemEffect INSTANCE = new UseItemEffect();
    }

    private UseItemEffect() {
    }

    public static UseItemEffect getInstance() {
        return Holder.INSTANCE;
    }

    @Override
    public EffectResult<InteractionResult> processSideEffect(
        final InteractItemPipeline<UseItemArgs> pipeline, final InteractionResult result, final UseItemArgs args
    ) {
        final var res = args.gameMode().useItem(args.player(), args.world(), args.stack(), args.hand());
        pipeline.transactor().logPlayerInventoryChange(args.player(), PlayerInventoryTransaction.EventCreator.STANDARD);
        try (EffectTransactor ignored = BroadcastInventoryChangesEffect.transact(pipeline.transactor())) {
            args.player().containerMenu.broadcastChanges();
        }
        return new EffectResult<>(res, true);
    }
}
