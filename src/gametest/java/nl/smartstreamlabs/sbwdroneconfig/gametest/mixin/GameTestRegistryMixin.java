package nl.smartstreamlabs.sbwdroneconfig.gametest.mixin;

import net.minecraft.gametest.framework.GameTestRegistry;
import net.minecraft.gametest.framework.TestFunction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;

/**
 * Runs only this addon's game tests. Porting Lib Core (a Superb Warfare dependency) pulls in
 * porting_lib_gametest, whose own self-test needs a structure that is not shipped.
 */
@Mixin(GameTestRegistry.class)
abstract class GameTestRegistryMixin {
    private static final java.util.Set<String> OURS = java.util.Set.of(
            "pilotviewgametest", "fpvstrikegametest", "pilotresyncgametest", "sessionlifecyclegametest", "fuzegametest");

    @Inject(method = "getAllTestFunctions", at = @At("RETURN"), cancellable = true)
    private static void sbwdroneconfig$onlyOurTests(CallbackInfoReturnable<Collection<TestFunction>> cir) {
        cir.setReturnValue(cir.getReturnValue().stream()
                .filter(t -> OURS.contains(t.testName().substring(0, Math.max(0, t.testName().indexOf('.')))))
                .toList());
    }
}
