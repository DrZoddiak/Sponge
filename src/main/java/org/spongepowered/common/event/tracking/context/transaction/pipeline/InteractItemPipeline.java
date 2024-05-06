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
package org.spongepowered.common.event.tracking.context.transaction.pipeline;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.common.event.tracking.PhaseContext;
import org.spongepowered.common.event.tracking.context.transaction.EffectTransactor;
import org.spongepowered.common.event.tracking.context.transaction.ResultingTransactionBySideEffect;
import org.spongepowered.common.event.tracking.context.transaction.TransactionalCaptureSupplier;
import org.spongepowered.common.event.tracking.context.transaction.effect.EffectResult;
import org.spongepowered.common.event.tracking.context.transaction.effect.InteractionAtArgs;
import org.spongepowered.common.event.tracking.context.transaction.effect.InteractionUseItemEffect;
import org.spongepowered.common.event.tracking.context.transaction.effect.InteractionUseItemOnEffect;
import org.spongepowered.common.event.tracking.context.transaction.effect.ProcessingSideEffect;
import org.spongepowered.common.event.tracking.context.transaction.effect.UseItemArgs;
import org.spongepowered.common.event.tracking.context.transaction.effect.UseItemEffect;
import org.spongepowered.common.event.tracking.context.transaction.effect.UseItemOnArgs;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Supplier;

public final class InteractItemPipeline<A extends ProcessingSideEffect.Args> {

    public static InteractItemPipeline.Builder<InteractionAtArgs> kickOff(
        ServerLevel worldIn, ServerPlayer playerIn, InteractionHand handIn, BlockHitResult blockRaytraceResultIn,
        BlockState blockstate, ItemStack copiedStack, TransactionalCaptureSupplier transactor) {
        final var builder = new Builder<InteractionAtArgs>();
        builder.addEffect(InteractionUseItemEffect.getInstance());
        final var playerRef = new WeakReference<>(playerIn);
        builder.player = () -> Objects.requireNonNull(playerRef.get(), "Player de-referenced");
        final WeakReference<ServerLevel> worldRef = new WeakReference<>(worldIn);
        builder.serverWorld = () -> Objects.requireNonNull(worldRef.get(), "ServerWorld de-referenced");
        builder.hand = handIn;
        builder.blockRaytraceResult = blockRaytraceResultIn;
        builder.blockstate = blockstate;
        builder.copiedStack = copiedStack;
        builder.transactor = transactor;
        builder.argsBuilder = (world, player) -> new InteractionAtArgs(
            world,
            player,
            handIn,
            blockRaytraceResultIn,
            blockstate,
            copiedStack
        );
        return builder;
    }

    public static InteractItemPipeline.Builder<UseItemOnArgs> interactItem(
        ServerLevel serverLevel, UseOnContext context, ItemStack itemStack, ItemStackSnapshot itemStackSnapshot,
        ServerPlayer player, TransactionalCaptureSupplier transactor, final boolean isCreative) {
        final var builder = new Builder<UseItemOnArgs>();
        builder.addEffect(InteractionUseItemOnEffect.getInstance());
        final var playerRef = new WeakReference<>(player);
        builder.player = () -> Objects.requireNonNull(playerRef.get(), "Player de-referenced");
        builder.copiedStack = itemStack;
        builder.transactor = transactor;
        builder.argsBuilder = (world, p) -> new UseItemOnArgs(
            context,
            itemStack,
            itemStackSnapshot,
            p,
            isCreative
        );
        final var worldRef = new WeakReference<>(serverLevel);
        builder.serverWorld = () -> Objects.requireNonNull(worldRef.get(), "ServerWorld de-referenced");

        return builder;
    }

    public static InteractItemPipeline.Builder<UseItemArgs> useItem(
        final ServerLevel serverLevel, final ServerPlayerGameMode serverPlayerGameMode, final ItemStack itemStack,
        final ServerPlayer player, InteractionHand hand, final TransactionalCaptureSupplier transactor) {
        final var builder = new Builder<UseItemArgs>();
        builder.addEffect(UseItemEffect.getInstance());
        builder.copiedStack = itemStack;
        final var worldRef = new WeakReference<>(serverLevel);
        builder.serverWorld = () -> Objects.requireNonNull(worldRef.get(), "ServerWorld de-referenced");
        builder.transactor = transactor;
        final var playerRef = new WeakReference<>(player);
        builder.player = () -> Objects.requireNonNull(playerRef.get(), "Player de-referenced");
        builder.argsBuilder = (world, p) -> new UseItemArgs(world, serverPlayerGameMode, p,  hand, itemStack);
        return builder;
    }

    private final @Nullable Supplier<ServerLevel> worldIn;
    private final Supplier<ServerPlayer> player;
    private final List<ResultingTransactionBySideEffect<InteractItemPipeline<A>, InteractionResult, A, InteractionResult>> effects;
    private final TransactionalCaptureSupplier transactor;
    private final BiFunction<ServerLevel, ServerPlayer, A> argsProvider;


    private InteractItemPipeline(final Builder<A> builder) {
        this.worldIn = builder.serverWorld;
        this.player = Objects.requireNonNull(builder.player, "player cannot be null");
        this.effects = builder.effects;
        this.transactor = Objects.requireNonNull(builder.transactor);
        this.argsProvider = Objects.requireNonNull(builder.argsBuilder, "args provider cannot be null");
    }

    public InteractionResult processInteraction(PhaseContext<?> context) {
        var interaction = InteractionResult.PASS;
        for (final var effect : this.effects) {
            final var level = this.worldIn.get();
            final var player = this.player.get();
            try (final EffectTransactor ignored = context.getTransactor().pushEffect(effect)) {
                final var args = this.argsProvider.apply(level, player);
                final EffectResult<InteractionResult> result = effect.effect.processSideEffect(
                    this,
                    interaction,
                    args
                );
                if (result.hasResult) {
                    final @Nullable InteractionResult resultingState = result.resultingState;
                    interaction = Objects.requireNonNullElse(resultingState, interaction);
                }
            }
        }
        return interaction;
    }

    public TransactionalCaptureSupplier transactor() {
        return this.transactor;
    }

    public static final class Builder<A extends ProcessingSideEffect.Args> {

        @MonotonicNonNull
        BiFunction<ServerLevel, ServerPlayer, A> argsBuilder;
        @Nullable
        Supplier<ServerLevel> serverWorld;
        @Nullable
        Supplier<ServerPlayer> player;
        @Nullable
        InteractionHand hand;
        @Nullable
        BlockHitResult blockRaytraceResult;
        @Nullable
        BlockState blockstate;
        @Nullable
        ItemStack copiedStack;
        @Nullable
        TransactionalCaptureSupplier transactor;
        @MonotonicNonNull
        List<ResultingTransactionBySideEffect<InteractItemPipeline<A>, InteractionResult, A, InteractionResult>> effects;

        public Builder<A> addEffect(final ProcessingSideEffect<InteractItemPipeline<A>, InteractionResult, A, InteractionResult> effect) {
            if (this.effects == null) {
                this.effects = new LinkedList<>();
            }
            this.effects.add(new ResultingTransactionBySideEffect<>(Objects.requireNonNull(effect, "Effect is null")));
            return this;
        }

        public InteractItemPipeline<A> build() {
            if (this.effects == null) {
                this.effects = Collections.emptyList();
            }
            Objects.requireNonNull(this.serverWorld, "ServerWorld cannot be null!");
            Objects.requireNonNull(this.player, "Player cannot be null!");
            return new InteractItemPipeline<>(this);
        }

    }
}
