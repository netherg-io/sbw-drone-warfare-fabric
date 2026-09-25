package nl.smartstreamlabs.sbwdroneconfig.gametest.mixin;

import com.atsuishio.superbwarfare.tools.CustomExplosion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


/** Records every SBW custom explosion (the FPV fuze and SBW's own warhead blasts go through this builder). */
@Mixin(value = CustomExplosion.Builder.class, remap = false)
abstract class ExplosionCounterMixin {
    @Inject(method = "explode", at = @At("HEAD"))
    private void sbwdroneconfig$count(CallbackInfo ci) {
        nl.smartstreamlabs.sbwdroneconfig.gametest.FuzeGameTest.EXPLOSIONS.add(((CustomExplosion.Builder) (Object) this).getPosition());
    }
}
