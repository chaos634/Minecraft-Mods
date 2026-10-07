package io.github.chaos634.taubenschlag.client.render;

import java.util.EnumMap;
import java.util.Map;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

import io.github.chaos634.taubenschlag.Taubenschlag;
import io.github.chaos634.taubenschlag.entity.BrieftaubeEntity;
import io.github.chaos634.taubenschlag.entity.BrieftaubeVariant;

public class BrieftaubeRenderer extends MobRenderer<BrieftaubeEntity, BrieftaubeRenderState, BrieftaubeModel> {
	private static final Map<BrieftaubeVariant, Identifier> TEXTURES = new EnumMap<>(BrieftaubeVariant.class);
	private static final float YOUNG_SCALE = 0.6F;

	static {
		for (BrieftaubeVariant variant : BrieftaubeVariant.values()) {
			TEXTURES.put(variant, Taubenschlag.id("textures/entity/brieftaube/" + variant.id() + ".png"));
		}
	}

	public BrieftaubeRenderer(EntityRendererProvider.Context context) {
		super(context, new BrieftaubeModel(context.bakeLayer(BrieftaubeModel.LAYER)), 0.25F);
	}

	@Override
	public BrieftaubeRenderState createRenderState() {
		return new BrieftaubeRenderState();
	}

	@Override
	public void extractRenderState(BrieftaubeEntity entity, BrieftaubeRenderState state, float partialTick) {
		super.extractRenderState(entity, state, partialTick);
		state.variant = entity.getVariant();
		state.flying = entity.isAirborne();
		state.sitting = entity.isInSittingPose();
		float flap = Mth.lerp(partialTick, entity.oFlap, entity.flap);
		float flapSpeed = Mth.lerp(partialTick, entity.oFlapSpeed, entity.flapSpeed);
		state.flapAngle = (Mth.sin(flap) + 1.0F) * flapSpeed;
	}

	@Override
	public Identifier getTextureLocation(BrieftaubeRenderState state) {
		return TEXTURES.get(state.variant);
	}

	@Override
	protected void scale(BrieftaubeRenderState state, PoseStack poseStack) {
		if (state.isBaby) {
			poseStack.scale(YOUNG_SCALE, YOUNG_SCALE, YOUNG_SCALE);
		}
	}
}
