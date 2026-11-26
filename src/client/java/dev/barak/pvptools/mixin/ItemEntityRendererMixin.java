package dev.barak.pvptools.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.barak.pvptools.BarakConfig;
import dev.barak.pvptools.ItemScaleTracker;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Macht fallengelassene Items größer/kleiner (pro Item-Typ einstellbar).
 * Alle Injektionen sind mit require = 0 abgesichert: Passt etwas nicht, wird nur die Skalierung
 * übersprungen, das Spiel stürzt nicht ab.
 */
@Mixin(ItemEntityRenderer.class)
public abstract class ItemEntityRendererMixin {

    @Unique
    private PoseStack barakpvptools$pushed;

    @Inject(
            method = "extractRenderState(Lnet/minecraft/world/entity/item/ItemEntity;Lnet/minecraft/client/renderer/entity/state/ItemEntityRenderState;F)V",
            at = @At("RETURN"),
            require = 0)
    private void barakpvptools$extract(ItemEntity entity, ItemEntityRenderState state, float partialTick, CallbackInfo ci) {
        ItemScaleTracker.set(state, BarakConfig.scaleFor(entity.getItem()));
    }

    @Inject(
            method = "submit(Lnet/minecraft/client/renderer/entity/state/ItemEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V",
            at = @At("HEAD"),
            require = 0)
    private void barakpvptools$submitStart(CallbackInfo ci,
                                           @Local(argsOnly = true) ItemEntityRenderState state,
                                           @Local(argsOnly = true) PoseStack poseStack) {
        float s = ItemScaleTracker.get(state);
        if (s != 1.0f) {
            poseStack.pushPose();
            poseStack.scale(s, s, s);
            this.barakpvptools$pushed = poseStack;
        }
    }

    @Inject(
            method = "submit(Lnet/minecraft/client/renderer/entity/state/ItemEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V",
            at = @At("RETURN"),
            require = 0)
    private void barakpvptools$submitEnd(CallbackInfo ci) {
        if (this.barakpvptools$pushed != null) {
            this.barakpvptools$pushed.popPose();
            this.barakpvptools$pushed = null;
        }
    }
}
