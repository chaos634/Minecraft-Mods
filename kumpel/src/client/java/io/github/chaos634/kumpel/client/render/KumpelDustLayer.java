package io.github.chaos634.kumpel.client.render;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.Identifier;

import io.github.chaos634.kumpel.Kumpel;
import io.github.chaos634.kumpel.entity.behaviour.CoalDust;

/** Kohlenstaub: the dust a Kumpel collects while digging, drawn over its texture in three stages. */
public class KumpelDustLayer extends RenderLayer<KumpelRenderState, KumpelModel> {
	private static final Identifier[] DUST = new Identifier[CoalDust.STAGES];

	static {
		for (int stage = 1; stage <= CoalDust.STAGES; stage++) {
			DUST[stage - 1] = Kumpel.id("textures/entity/kumpel/dust_" + stage + ".png");
		}
	}

	public KumpelDustLayer(RenderLayerParent<KumpelRenderState, KumpelModel> parent) {
		super(parent);
	}

	@Override
	public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, KumpelRenderState state, float yRot, float xRot) {
		if (state.dust > 0) {
			coloredCutoutModelCopyLayerRender(getParentModel(), DUST[Math.min(state.dust, CoalDust.STAGES) - 1], poseStack, collector, light, state, -1, 1);
		}
	}
}
