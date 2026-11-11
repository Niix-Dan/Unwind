package com.niixlabs.unwind.mixin.common;

import com.niixlabs.unwind.core.FrameCaptureContext;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Mixin(RecipeManager.class)
public class MixinRecipeManager {

    @Inject(method = "getRecipeFor(Lnet/minecraft/world/item/crafting/RecipeType;Lnet/minecraft/world/item/crafting/RecipeInput;Lnet/minecraft/world/level/Level;)Ljava/util/Optional;", at = @At("HEAD"))
    private <I extends RecipeInput, T extends Recipe<I>> void unwind$onGetRecipeHead(RecipeType<T> recipeType, I input, Level level, CallbackInfoReturnable<Optional<RecipeHolder<T>>> cir) {
        String typeId = BuiltInRegistries.RECIPE_TYPE.getKey(recipeType) != null
                ? BuiltInRegistries.RECIPE_TYPE.getKey(recipeType).toString()
                : recipeType.toString();

        StringBuilder slotsDump = new StringBuilder("[");
        for (int i = 0; i < input.size(); i++) {
            if (i > 0) slotsDump.append(", ");
            slotsDump.append(i).append("=").append(input.getItem(i));
        }
        slotsDump.append("]");

        Map<String, String> args = new LinkedHashMap<>();
        args.put("RecipeType", typeId);
        args.put("RecipeInputClass", input.getClass().getName());
        args.put("InputSize", String.valueOf(input.size()));
        args.put("InputItems", slotsDump.toString());

        FrameCaptureContext.pushFrame(
                level != null ? level.getGameTime() : -1L,
                "RECIPE_LOOKUP",
                "RecipeManager.getRecipeFor(" + typeId + ")",
                level != null && level.isClientSide() ? "CLIENT" : "SERVER",
                recipeType.getClass(),
                args
        );
    }

    @Inject(method = "getRecipeFor(Lnet/minecraft/world/item/crafting/RecipeType;Lnet/minecraft/world/item/crafting/RecipeInput;Lnet/minecraft/world/level/Level;)Ljava/util/Optional;", at = @At("RETURN"))
    private <I extends RecipeInput, T extends Recipe<I>> void unwind$onGetRecipeReturn(RecipeType<T> recipeType, I input, Level level, CallbackInfoReturnable<Optional<RecipeHolder<T>>> cir) {
        FrameCaptureContext.popFrame();
    }
}