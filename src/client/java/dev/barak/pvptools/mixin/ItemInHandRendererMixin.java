package dev.barak.pvptools.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.barak.pvptools.BarakConfig;
import net.minecraft.client.renderer.ItemInHandRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Skaliert das Item in der Hand (First-Person). Alles ist mit require = 0 abgesichert:
 * Passt etwas nicht, wird nur die Skalierung übersprungen, das Spiel stürzt nicht ab.
 */
@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin {

    @Unique
    private PoseStack barakpvptools$pushed;

    @ModifyVariable(method = "renderArmWithItem", at = @At("HEAD"), argsOnly = true, require = 0)
    private PoseStack barakpvptools$scaleStart(PoseStack poseStack) {
        float s = BarakConfig.itemScale;
        if (s != 1.0f) {
            poseStack.pushPose();
            // Skalierung um die Handposition, damit das Item nicht aus dem Bild wandert
            poseStack.translate(0.56f, -0.52f, -0.72f);
            poseStack.scale(s, s, s);
            poseStack.translate(-0.56f, 0.52f, 0.72f);
            this.barakpvptools$pushed = poseStack;
        }
        return poseStack;
    }

    @Inject(method = "renderArmWithItem", at = @At("RETURN"), require = 0)
    private void barakpvptools$scaleEnd(CallbackInfo ci) {
        if (this.barakpvptools$pushed != null) {
            this.barakpvptools$pushed.popPose();
            this.barakpvptools$pushed = null;
        }
    }
}
