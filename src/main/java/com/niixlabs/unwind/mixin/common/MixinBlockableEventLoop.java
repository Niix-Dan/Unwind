package com.niixlabs.unwind.mixin.common;

import com.niixlabs.unwind.core.AsyncCausalityTracker;
import net.minecraft.util.thread.BlockableEventLoop;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockableEventLoop.class)
public class MixinBlockableEventLoop<R extends Runnable> {

    @Inject(method = "tell(Ljava/lang/Runnable;)V", at = @At("HEAD"))
    private void unwind$onTellTask(R task, CallbackInfo ci) {
        AsyncCausalityTracker.tagRunnableOnSchedule(task);
    }

    @Inject(method = "doRunTask", at = @At("HEAD"))
    private void unwind$onStartRunTask(R task, CallbackInfo ci) {
        AsyncCausalityTracker.onEnterTask(task);
    }

    @Inject(method = "doRunTask", at = @At("RETURN"))
    private void unwind$onEndRunTask(R task, CallbackInfo ci) {
        AsyncCausalityTracker.onExitTask();
    }
}