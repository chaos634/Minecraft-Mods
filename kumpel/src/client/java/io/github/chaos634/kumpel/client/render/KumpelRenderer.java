package io.github.chaos634.kumpel.client.render;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.resources.Identifier;

import io.github.chaos634.kumpel.entity.KumpelEntity;
import io.github.chaos634.kumpel.entity.behaviour.BarbaraDay;

public class KumpelRenderer extends MobRenderer<KumpelEntity, KumpelRenderState, KumpelModel> {
	public KumpelRenderer(EntityRendererProvider.Context context) {
		super(context, new KumpelModel(context.bakeLayer(KumpelModel.LAYER)), 0.35F);
		// Shows the pickaxe of a Kumpel in Hauer mode.
		addLayer(new ItemInHandLayer<>(this));
	}

	@Override
	public KumpelRenderState createRenderState() {
		return new KumpelRenderState();
	}

	@Override
	public void extractRenderState(KumpelEntity entity, KumpelRenderState state, float partialTick) {
		super.extractRenderState(entity, state, partialTick);
		ArmedEntityRenderState.extractArmedEntityRenderState(entity, state, itemModelResolver, partialTick);
		state.texture = entity.getTexture();
		state.sitting = entity.isInSittingPose();
		state.pointing = entity.isPointing();
		state.mining = entity.isMining();
		state.dancing = entity.isDancing();
		state.barbaraDay = BarbaraDay.isToday();
		state.canary = entity.hasCanary();
		state.forge = entity.hasForge();
	}

	@Override
	public Identifier getTextureLocation(KumpelRenderState state) {
		return state.texture;
	}
}
