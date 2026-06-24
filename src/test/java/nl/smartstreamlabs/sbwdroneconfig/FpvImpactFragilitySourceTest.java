package nl.smartstreamlabs.sbwdroneconfig;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class FpvImpactFragilitySourceTest {
    @Test
    void cubedFpvHasDedicatedImpactFragilitySystem() throws Exception {
        Path systemSource = Path.of(
                "src",
                "main",
                "java",
                "nl",
                "smartstreamlabs",
                "sbwdroneconfig",
                "FpvImpactDamageSystem.java"
        );
        String source = Files.readString(systemSource, StandardCharsets.UTF_8);

        assertTrue(source.contains("entity instanceof CubedFpvDroneEntity"),
                "Impact fragility must target only the Cubed FPV drone, not LUCAS or the base SBW drone.");
        assertTrue(source.contains("horizontalCollision") || source.contains("verticalCollision"),
                "Impact fragility should only trigger after real block collision flags.");
        assertTrue(source.contains("getDeltaMovement().length()"),
                "The system should capture real pre-impact speed before SBW travel can clamp movement.");
        assertTrue(source.contains("drone.hurt("),
                "A hard FPV collision should apply actual entity damage so it can break normally.");
        assertTrue(source.contains("DESTROY_IMPACT_SPEED"),
                "Very hard FPV collisions should have a direct destroy threshold so the drone no longer feels tanky.");
    }

    @Test
    void droneTravelMixinInvokesImpactFragilityBeforeAndAfterTravel() throws Exception {
        Path mixinSource = Path.of(
                "src",
                "main",
                "java",
                "nl",
                "smartstreamlabs",
                "sbwdroneconfig",
                "mixin",
                "DroneEnergyMixin.java"
        );
        String source = Files.readString(mixinSource, StandardCharsets.UTF_8);

        assertTrue(source.contains("FpvImpactDamageSystem.beforeDroneTravel(drone);"),
                "The mixin should capture FPV speed before the base travel collision logic runs.");
        assertTrue(source.contains("FpvImpactDamageSystem.afterDroneTravel(drone);"),
                "The mixin should convert post-travel collision flags into FPV impact damage.");
    }
}
