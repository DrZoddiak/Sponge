/*
 * This file is part of Sponge, licensed under the MIT License (MIT).
 *
 * Copyright (c) SpongePowered <https://spongepowered.org>
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
 *
 */
package org.spongepowered.common.event.tracking.context.transaction.inventory;

import com.google.common.collect.ImmutableList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.event.Cancellable;
import org.spongepowered.api.event.Cause;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.api.event.CompositeEvent;
import org.spongepowered.api.event.Event;
import org.spongepowered.api.event.SpongeEventFactory;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.impl.AbstractCompositeEvent;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.util.Tristate;
import org.spongepowered.common.event.tracking.PhaseContext;
import org.spongepowered.common.event.tracking.context.transaction.GameTransaction;
import org.spongepowered.common.event.tracking.context.transaction.type.TransactionTypes;
import org.spongepowered.common.item.util.ItemStackUtil;
import org.spongepowered.common.util.PrettyPrinter;
import org.spongepowered.math.vector.Vector3d;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

public class InteractItemWithBlockTransaction extends GameTransaction<CompositeEvent<InteractBlockEvent.Secondary>> {

    private final Vector3d hitVec;
    private final BlockSnapshot snapshot;
    private final Direction direction;
    private final InteractionHand hand;
    private final ItemStackSnapshot stack;
    private final Tristate originalBlockResult, blockResult, originalItemResult, itemResult;

    public InteractItemWithBlockTransaction(
        final ServerPlayer playerIn, final ItemStack stackIn, final Vector3d hitVec, final BlockSnapshot snapshot,
        final Direction direction, final InteractionHand handIn,
        final Tristate originalBlockResult, final Tristate useBlockResult,
        final Tristate originalUseItemResult, final Tristate useItemResult) {
        super(TransactionTypes.INTERACT_BLOCK_SECONDARY.get());
        this.stack = ItemStackUtil.snapshotOf(stackIn);
        this.hitVec = hitVec;
        this.snapshot = snapshot;
        this.direction = direction;
        this.hand = handIn;
        this.originalBlockResult = originalBlockResult;
        this.blockResult = useBlockResult;
        this.originalItemResult = originalUseItemResult;
        this.itemResult = useItemResult;

    }


    @Override
    public Optional<BiConsumer<PhaseContext<@NonNull ?>, CauseStackManager.StackFrame>> getFrameMutator(
        @Nullable GameTransaction<@NonNull ?> parent
    ) {
        return Optional.empty();
    }

    @Override
    public void addToPrinter(PrettyPrinter printer) {

    }

    @Override
    public Optional<CompositeEvent<InteractBlockEvent.Secondary>> generateEvent(
        final PhaseContext<@NonNull ?> context,
        final @Nullable GameTransaction<@NonNull ?> parent,
        final ImmutableList<GameTransaction<CompositeEvent<InteractBlockEvent.Secondary>>> gameTransactions,
        final Cause currentCause
    ) {
        final var root = SpongeEventFactory.createInteractBlockEventSecondary(currentCause,
            this.originalBlockResult, this.blockResult,
            this.originalItemResult, this.itemResult,
            this.snapshot, this.hitVec,
            this.direction
        );
        final List<Event> list = new ArrayList<>();
        final var composite = SpongeEventFactory.createCompositeEvent(currentCause, root, list);
        return Optional.of(composite);
    }

    @Override
    public void associateSideEffectEvents(CompositeEvent<InteractBlockEvent.Secondary> event, Stream<Event> elements) {
        elements.forEach(event.children()::add);
        // This finalizes the list to be immutable
        ((AbstractCompositeEvent<InteractBlockEvent.Secondary>) event).postInit();
    }

    public void pushCause(CauseStackManager.StackFrame frame, CompositeEvent<InteractBlockEvent.Secondary> e) {
        frame.pushCause(e.baseEvent());
    }

    @Override
    public void restore(PhaseContext<@NonNull ?> context, CompositeEvent<InteractBlockEvent.Secondary> event) {

    }

    @Override
    public boolean markCancelledTransactions(
        final CompositeEvent<InteractBlockEvent.Secondary> event,
        final ImmutableList<? extends GameTransaction<CompositeEvent<InteractBlockEvent.Secondary>>> gameTransactions) {
        event.children().stream().filter(e -> e instanceof Cancellable)
            .map(e -> (Cancellable) e)
            .forEach(e -> e.setCancelled(event.isCancelled()));
        return false;
    }
}
