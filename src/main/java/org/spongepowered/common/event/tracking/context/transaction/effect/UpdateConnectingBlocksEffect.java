/*
 * This file is part of Sponge, licensed under the MIT License (MIT).
 *
 * Copyright (c) SpongePowered <https://www.spongepowered.org>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.spongepowered.common.event.tracking.context.transaction.effect;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.common.event.tracking.context.transaction.pipeline.BlockPipeline;
import org.spongepowered.common.event.tracking.context.transaction.pipeline.PipelineCursor;

public final class UpdateConnectingBlocksEffect implements ProcessingSideEffect<BlockPipeline, PipelineCursor, BlockChangeArgs, BlockState> {

    private static final class Holder {
        static final UpdateConnectingBlocksEffect INSTANCE = new UpdateConnectingBlocksEffect();
    }

    public static UpdateConnectingBlocksEffect getInstance() {
        return UpdateConnectingBlocksEffect.Holder.INSTANCE;
    }

    UpdateConnectingBlocksEffect() {}

    @Override
    public EffectResult<@Nullable BlockState> processSideEffect(
        final BlockPipeline pipeline, final PipelineCursor oldState, final BlockChangeArgs args
    ) {
        final ServerLevel world = pipeline.getServerWorld();
        final BlockPos pos = oldState.pos;
        final var flag = args.flag();
        final var newState = args.newState();
        final var limit = args.limit();
        if (flag.updateNeighboringShapes() && limit > 0) {
            // int i = p_241211_3_ & -34; // Vanilla negates 34 to flip neighbor notification and and "state drops"
            final int withoutNeighborDropsAndNestedNeighborUpdates = flag.asNestedNeighborUpdates().getRawFlag();
            // blockstate.updateIndirectNeighbourShapes(this, p_241211_1_, i, p_241211_4_ - 1);
            oldState.state.updateIndirectNeighbourShapes(world, pos, withoutNeighborDropsAndNestedNeighborUpdates, limit - 1);
            // p_241211_2_.updateNeighbourShapes(this, p_241211_1_, i, p_241211_4_ - 1);
            newState.updateNeighbourShapes(world, pos, withoutNeighborDropsAndNestedNeighborUpdates, limit - 1);
            // p_241211_2_.updateIndirectNeighbourShapes(this, p_241211_1_, i, p_241211_4_ - 1);
            newState.updateIndirectNeighbourShapes(world, pos, withoutNeighborDropsAndNestedNeighborUpdates, limit - 1);
        }

        return EffectResult.nullPass();
    }

}
