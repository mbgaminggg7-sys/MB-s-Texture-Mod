package mb.texturemenu;

/** Mixin target strings (annotation values must be constants). 1.21.11 signature of the "draw a model" call. */
public final class Targets {
    private Targets() {}

    public static final String SUBMIT_MODEL =
            "submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;"
                    + "Lnet/minecraft/client/renderer/rendertype/RenderType;IIILnet/minecraft/client/renderer/texture/TextureAtlasSprite;"
                    + "ILnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V";

    public static final String ORDERED = "Lnet/minecraft/client/renderer/OrderedSubmitNodeCollector;" + SUBMIT_MODEL;
    public static final String DIRECT = "Lnet/minecraft/client/renderer/SubmitNodeCollector;" + SUBMIT_MODEL;
}
