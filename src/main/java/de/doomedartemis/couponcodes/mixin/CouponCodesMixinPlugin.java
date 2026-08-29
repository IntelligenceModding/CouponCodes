package de.doomedartemis.couponcodes.mixin;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public final class CouponCodesMixinPlugin implements IMixinConfigPlugin {
    private static final String JEI_BATCH_RENDERER = "mezz/jei/library/render/batch/SimpleItemStackBatchRenderer.class";

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.endsWith(".jei.SimpleItemStackBatchRendererMixin")) {
            return isClassPresent(JEI_BATCH_RENDERER);
        }
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    private static boolean isClassPresent(String className) {
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        ClassLoader ownClassLoader = CouponCodesMixinPlugin.class.getClassLoader();
        return hasResource(contextClassLoader, className) || hasResource(ownClassLoader, className);
    }

    private static boolean hasResource(ClassLoader classLoader, String resourceName) {
        return classLoader != null && classLoader.getResource(resourceName) != null;
    }
}
