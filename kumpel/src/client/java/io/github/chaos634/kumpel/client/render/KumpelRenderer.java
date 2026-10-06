package io.github.chaos634.kumpel.client.render;

import java.util.EnumMap;
import java.util.Map;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

import io.github.chaos634.kumpel.Kumpel;
import io.github.chaos634.kumpel.entity.KumpelEntity;
import io.github.chaos634.kumpel.entity.KumpelTier;

public class KumpelRenderer extends MobRenderer<KumpelEntity, KumpelRenderState, KumpelModel> {
	private static final Map<KumpelTier, Identifier> TEXTURES = new EnumMap<>(KumpelTier.class);

	static {
		for (KumpelTier tier : KumpelTier.values()) {
			TEXTURES.put(tier, Kumpel.id("textures/entity/kumpel/" + tier.textureName() + ".png"));
		}
	}

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
		state.tier = entity.getTier();
		state.sitting = entity.isInSittingPose();
		state.pointing = entity.isPointing();
	}

	@Override
	public Identifier getTextureLocation(KumpelRenderState state) {
		return TEXTURES.get(state.tier);
	}
}
