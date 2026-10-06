package io.github.chaos634.kumpel.client.render;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartNames;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

import io.github.chaos634.kumpel.Kumpel;

/**
 * A stocky little golem (about one block tall) with a miner's helmet and lamp.
 * Texture layout (64x32): head 0,0 · lamp 24,0 · arm 32,0 · leg 44,0 · body 0,12.
 */
public class KumpelModel extends EntityModel<KumpelRenderState> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Kumpel.id("kumpel"), "main");

	private static final float BODY_Y = 12.0F;
	private static final float HEAD_Y = 12.0F;
	private static final float ARM_Y = 13.0F;
	private static final float LEG_Y = 19.0F;
	/** How far the upper body drops when sitting, so it rests on the ground. */
	private static final float SIT_DROP = 5.0F;

	private final ModelPart head;
	private final ModelPart body;
	private final ModelPart rightArm;
	private final ModelPart leftArm;
	private final ModelPart rightLeg;
	private final ModelPart leftLeg;

	public KumpelModel(ModelPart root) {
		super(root);
		this.head = root.getChild(PartNames.HEAD);
		this.body = root.getChild(PartNames.BODY);
		this.rightArm = root.getChild(PartNames.RIGHT_ARM);
		this.leftArm = root.getChild(PartNames.LEFT_ARM);
		this.rightLeg = root.getChild(PartNames.RIGHT_LEG);
		this.leftLeg = root.getChild(PartNames.LEFT_LEG);
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();

		root.addOrReplaceChild(PartNames.HEAD,
				CubeListBuilder.create()
						.texOffs(0, 0).addBox(-3.0F, -6.0F, -3.0F, 6.0F, 6.0F, 6.0F)
						// helmet lamp
						.texOffs(24, 0).addBox(-1.0F, -6.0F, -4.0F, 2.0F, 2.0F, 1.0F),
				PartPose.offset(0.0F, HEAD_Y, 0.0F));
		root.addOrReplaceChild(PartNames.BODY,
				CubeListBuilder.create().texOffs(0, 12).addBox(-4.0F, 0.0F, -2.5F, 8.0F, 7.0F, 5.0F),
				PartPose.offset(0.0F, BODY_Y, 0.0F));
		root.addOrReplaceChild(PartNames.RIGHT_ARM,
				CubeListBuilder.create().texOffs(32, 0).addBox(-2.0F, -1.0F, -1.5F, 2.0F, 8.0F, 3.0F),
				PartPose.offset(-4.0F, ARM_Y, 0.0F));
		root.addOrReplaceChild(PartNames.LEFT_ARM,
				CubeListBuilder.create().texOffs(32, 0).mirror().addBox(0.0F, -1.0F, -1.5F, 2.0F, 8.0F, 3.0F),
				PartPose.offset(4.0F, ARM_Y, 0.0F));
		root.addOrReplaceChild(PartNames.RIGHT_LEG,
				CubeListBuilder.create().texOffs(44, 0).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 5.0F, 3.0F),
				PartPose.offset(-2.0F, LEG_Y, 0.0F));
		root.addOrReplaceChild(PartNames.LEFT_LEG,
				CubeListBuilder.create().texOffs(44, 0).mirror().addBox(-1.5F, 0.0F, -1.5F, 3.0F, 5.0F, 3.0F),
				PartPose.offset(2.0F, LEG_Y, 0.0F));

		return LayerDefinition.create(mesh, 64, 32);
	}

	@Override
	public void setupAnim(KumpelRenderState state) {
		super.setupAnim(state);

		float drop = state.sitting ? SIT_DROP : 0.0F;
		head.y = HEAD_Y + drop;
		body.y = BODY_Y + drop;
		rightArm.y = ARM_Y + drop;
		leftArm.y = ARM_Y + drop;

		head.yRot = state.yRot * Mth.DEG_TO_RAD;
		head.xRot = state.xRot * Mth.DEG_TO_RAD;

		float walkPos = state.walkAnimationPos;
		float walkSpeed = state.walkAnimationSpeed;
		float idle = state.ageInTicks * 0.067F;

		rightArm.xRot = Mth.cos(walkPos * 0.6662F + Mth.PI) * walkSpeed;
		leftArm.xRot = Mth.cos(walkPos * 0.6662F) * walkSpeed;
		rightArm.yRot = 0.0F;
		rightArm.zRot = Mth.cos(idle) * 0.05F + 0.05F;
		leftArm.zRot = -(Mth.cos(idle) * 0.05F + 0.05F);

		if (state.pointing) {
			// Raise the right arm towards whatever the head is looking at (a freshly sensed ore).
			rightArm.xRot = -Mth.HALF_PI + head.xRot;
			rightArm.yRot = head.yRot;
			rightArm.zRot = 0.0F;
		}

		if (state.sitting) {
			rightLeg.y = LEG_Y + 3.5F;
			leftLeg.y = LEG_Y + 3.5F;
			rightLeg.xRot = -Mth.HALF_PI;
			leftLeg.xRot = -Mth.HALF_PI;
			rightLeg.yRot = 0.2F;
			leftLeg.yRot = -0.2F;
		} else {
			rightLeg.y = LEG_Y;
			leftLeg.y = LEG_Y;
			rightLeg.xRot = Mth.cos(walkPos * 0.6662F) * 1.4F * walkSpeed;
			leftLeg.xRot = Mth.cos(walkPos * 0.6662F + Mth.PI) * 1.4F * walkSpeed;
			rightLeg.yRot = 0.0F;
			leftLeg.yRot = 0.0F;
		}
	}
}
