package nl.smartstreamlabs.sbwdroneconfig;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FiberOpticCableRenderMathTest {
    @Test
    void prefersCapturedLaunchAnchorOverMovedControllerPosition() {
        Vec3 movedControllerPosition = new Vec3(24.0D, 91.0D, -7.0D);
        Vec3 capturedLaunchAnchor = new Vec3(10.5D, 64.0D, 10.5D);

        Vec3 storedAnchor = FiberOpticCableRenderMath.resolveStoredAnchor(movedControllerPosition, capturedLaunchAnchor);

        assertEquals(capturedLaunchAnchor.x, storedAnchor.x, 1.0E-6D);
        assertEquals(capturedLaunchAnchor.y, storedAnchor.y, 1.0E-6D);
        assertEquals(capturedLaunchAnchor.z, storedAnchor.z, 1.0E-6D);
    }

    @Test
    void fallsBackToControllerFeetWhenNoCapturedLaunchAnchorExists() {
        Vec3 controllerFeet = new Vec3(5.25D, 66.0D, -2.75D);

        Vec3 storedAnchor = FiberOpticCableRenderMath.resolveStoredAnchor(controllerFeet, null);

        assertEquals(controllerFeet.x, storedAnchor.x, 1.0E-6D);
        assertEquals(controllerFeet.y, storedAnchor.y, 1.0E-6D);
        assertEquals(controllerFeet.z, storedAnchor.z, 1.0E-6D);
    }

    @Test
    void renderEndpointsAttachToGroundAndDroneRearBottomInsteadOfCenter() {
        Vec3 storedAnchor = new Vec3(10.5D, 64.0D, 10.5D);
        Vec3 dronePosition = new Vec3(22.0D, 70.0D, 30.0D);

        Vec3 renderStart = FiberOpticCableRenderMath.toRenderAnchor(storedAnchor);
        Vec3 renderEnd = FiberOpticCableRenderMath.toRenderEnd(dronePosition, 0.0F, 0.9D, 1.4D);

        assertTrue(renderStart.y > storedAnchor.y, "The cable should rise only slightly above the stored ground anchor.");
        assertTrue(renderStart.y < storedAnchor.y + 0.3D, "The ground offset should stay subtle.");
        assertTrue(renderEnd.z < dronePosition.z, "Yaw 0 should place the cable at the drone's rear, not the nose.");
        assertTrue(renderEnd.y < dronePosition.y + (1.4D * 0.5D), "The cable should attach low on the drone body, not at the camera center.");
    }

    @Test
    void segmentedCurveSagsSlightlyWithoutDippingTooFarBelowGround() {
        Vec3 renderStart = new Vec3(0.0D, 64.12D, 0.0D);
        Vec3 renderEnd = new Vec3(18.0D, 67.5D, 2.0D);

        Vec3[] points = FiberOpticCableRenderMath.buildCableCurvePoints(renderStart, renderEnd, 10);

        assertEquals(11, points.length, "Ten segments should generate eleven bezier points.");
        assertEquals(renderStart.x, points[0].x, 1.0E-6D);
        assertEquals(renderStart.y, points[0].y, 1.0E-6D);
        assertEquals(renderStart.z, points[0].z, 1.0E-6D);
        assertEquals(renderEnd.x, points[10].x, 1.0E-6D);
        assertEquals(renderEnd.y, points[10].y, 1.0E-6D);
        assertEquals(renderEnd.z, points[10].z, 1.0E-6D);

        double straightMidY = (renderStart.y + renderEnd.y) * 0.5D;
        double minimumCurveY = Double.POSITIVE_INFINITY;
        for (Vec3 point : points) {
            minimumCurveY = Math.min(minimumCurveY, point.y);
        }

        assertTrue(minimumCurveY < straightMidY, "The cable should sag below a perfectly straight midpoint.");
        assertTrue(minimumCurveY >= Math.min(renderStart.y, renderEnd.y) - 0.35D, "The sag should stay subtle and not sink far below the ground anchor.");
    }
}
