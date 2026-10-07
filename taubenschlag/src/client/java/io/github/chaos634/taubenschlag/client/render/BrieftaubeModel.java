package io.github.chaos634.taubenschlag.client.render;

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

import io.github.chaos634.taubenschlag.Taubenschlag;

/**
 * A pigeon: plump body, small round head with a beak, folded wings, a fan of a tail and two red legs. It nods its head
 * while it walks. Texture layout (32x32): body 0,0 · wing 20,0 · head 0,10 · beak 12,10 · leg 16,10 · tail 0,16.
 */
public class BrieftaubeModel extends EntityModel<BrieftaubeRenderState> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Taubenschlag.id("brieftaube"), "main");
	private static final String BEAK = "beak";
	private static final String LEFT_WING = "left_wing";
	private static final String RIGHT_WING = "right_wing";
	private static final String TAIL = "tail";

	private static final float BODY_Y = 17.0F;
	private static final float HEAD_Y = 17.5F;
	private static final float HEAD_Z = -2.5F;
	private static final float WING_Y = 17.5F;
	private static final float TAIL_Y = 18.5F;
	private static final float LEG_Y = 21.0F;
	/** How far a sitting pigeon sinks down onto its legs. */
	private static final float SIT_DROP = 2.0F;
	/** Wings rest slightly away from the body. */
	private static final float WING_REST = 0.0873F;

	private final ModelPart body;
	private final ModelPart head;
	private final ModelPart leftWing;
	private final ModelPart rightWing;
	private final ModelPart tail;
	private final ModelPart leftLeg;
	private final ModelPart rightLeg;

	public BrieftaubeModel(ModelPart root) {
		super(root);
		this.body = root.getChild(PartNames.BODY);
		this.head = root.getChild(PartNames.HEAD);
		this.leftWing = root.getChild(LEFT_WING);
		this.rightWing = root.getChild(RIGHT_WING);
		this.tail = root.getChild(TAIL);
		this.leftLeg = root.getChild(PartNames.LEFT_LEG);
		this.rightLeg = root.getChild(PartNames.RIGHT_LEG);
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();

		root.addOrReplaceChild(PartNames.BODY,
				CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, 0.0F, -3.0F, 4.0F, 4.0F, 6.0F),
				PartPose.offset(0.0F, BODY_Y, 0.0F));
		PartDefinition head = root.addOrReplaceChild(PartNames.HEAD,
				CubeListBuilder.create().texOffs(0, 10).addBox(-1.5F, -3.0F, -1.5F, 3.0F, 3.0F, 3.0F),
				PartPose.offset(0.0F, HEAD_Y, HEAD_Z));
		head.addOrReplaceChild(BEAK,
				CubeListBuilder.create().texOffs(12, 10).addBox(-0.5F, -1.5F, -2.5F, 1.0F, 1.0F, 1.0F),
				PartPose.ZERO);
		root.addOrReplaceChild(LEFT_WING,
				CubeListBuilder.create().texOffs(20, 0).addBox(0.0F, 0.0F, 0.0F, 1.0F, 3.0F, 5.0F),
				PartPose.offset(2.0F, WING_Y, -2.0F));
		root.addOrReplaceChild(RIGHT_WING,
				CubeListBuilder.create().texOffs(20, 0).mirror().addBox(-1.0F, 0.0F, 0.0F, 1.0F, 3.0F, 5.0F),
				PartPose.offset(-2.0F, WING_Y, -2.0F));
		root.addOrReplaceChild(TAIL,
				CubeListBuilder.create().texOffs(0, 16).addBox(-1.5F, 0.0F, 0.0F, 3.0F, 1.0F, 4.0F),
				PartPose.offsetAndRotation(0.0F, TAIL_Y, 2.5F, -0.3F, 0.0F, 0.0F));
		root.addOrReplaceChild(PartNames.LEFT_LEG,
				CubeListBuilder.create().texOffs(16, 10).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 3.0F, 1.0F),
				PartPose.offset(1.0F, LEG_Y, 0.5F));
		root.addOrReplaceChild(PartNames.RIGHT_LEG,
				CubeListBuilder.create().texOffs(16, 10).mirror().addBox(-0.5F, 0.0F, -0.5F, 1.0F, 3.0F, 1.0F),
				PartPose.offset(-1.0F, LEG_Y, 0.5F));

		return LayerDefinition.create(mesh, 32, 32);
	}

	@Override
	public void setupAnim(BrieftaubeRenderState state) {
		super.setupAnim(state);

		float drop = state.sitting ? SIT_DROP : 0.0F;
		body.y = BODY_Y + drop;
		head.y = HEAD_Y + drop;
		leftWing.y = WING_Y + drop;
		rightWing.y = WING_Y + drop;
		tail.y = TAIL_Y + drop;

		head.yRot = state.yRot * Mth.DEG_TO_RAD;
		head.xRot = state.xRot * Mth.DEG_TO_RAD;
		// Pigeons nod as they walk.
		head.z = HEAD_Z - Mth.abs(Mth.sin(state.walkAnimationPos * 0.9F)) * Mth.clamp(state.walkAnimationSpeed * 2.0F, 0.0F, 1.0F);

		float wings = WING_REST + (state.flying ? 0.3F + state.flapAngle : state.flapAngle * 0.3F);
		leftWing.zRot = -wings;
		rightWing.zRot = wings;

		boolean legsVisible = !state.sitting;
		leftLeg.visible = legsVisible;
		rightLeg.visible = legsVisible;
		if (state.flying) {
			// Legs tucked back in flight, tail spread out behind.
			leftLeg.xRot = 1.2F;
			rightLeg.xRot = 1.2F;
			tail.xRot = 0.0F;
		} else {
			float walkPos = state.walkAnimationPos;
			float walkSpeed = state.walkAnimationSpeed;
			leftLeg.xRot = Mth.cos(walkPos * 0.6662F) * 1.4F * walkSpeed;
			rightLeg.xRot = Mth.cos(walkPos * 0.6662F + Mth.PI) * 1.4F * walkSpeed;
			tail.xRot = -0.3F;
		}
	}
}
