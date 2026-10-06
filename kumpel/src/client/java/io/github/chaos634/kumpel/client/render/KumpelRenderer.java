package io.github.chaos634.kumpel.client.render;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

import io.github.chaos634.kumpel.entity.KumpelEntity;

public class KumpelRenderer extends MobRenderer<KumpelEntity, KumpelRenderState, KumpelModel> {
	public KumpelRenderer(EntityRendererProvider.Context context) {
		super(context, new KumpelModel(context.bakeLayer(KumpelModel.LAYER)), 0.35F);
	}

	@Override
	public KumpelRenderState createRenderState() {
		return new KumpelRenderState();
	}

	@Override
	public void extractRenderState(KumpelEntity entity, KumpelRenderState state, float partialTick) {
		super.extractRenderState(entity, state, partialTick);
		state.texture = entity.getTexture();
		state.sitting = entity.isInSittingPose();
		state.pointing = entity.isPointing();
	}

	@Override
	public Identifier getTextureLocation(KumpelRenderState state) {
		return state.texture;
	}
}
