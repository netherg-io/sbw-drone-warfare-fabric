package nl.smartstreamlabs.sbwdroneconfig;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

final class FiberOpticCableRenderMath {
    private static final double GROUND_OFFSET = 0.12D;
    private static final double MIN_REAR_OFFSET = 0.18D;
    private static final double MAX_REAR_OFFSET = 0.34D;
    private static final double MIN_BODY_ATTACHMENT_HEIGHT = 0.06D;
    private static final double MAX_BODY_ATTACHMENT_HEIGHT = 0.16D;
    private static final double MIN_SAG = 0.08D;
    private static final double MAX_SAG = 1.05D;
    private static final double MAX_GROUND_DIP = 0.35D;

    private FiberOpticCableRenderMath() {
    }

    static Vec3 resolveStoredAnchor(Vec3 controllerFeetPosition, Vec3 capturedLaunchAnchor) {
        return capturedLaunchAnchor != null ? capturedLaunchAnchor : controllerFeetPosition;
    }

    static Vec3 toRenderAnchor(Vec3 storedAnchor) {
        return storedAnchor.add(0.0D, GROUND_OFFSET, 0.0D);
    }

    static Vec3 toRenderEnd(Vec3 dronePosition, float droneYawDegrees, double droneBbWidth, double droneBbHeight) {
        Vec3 forward = horizontalForward(droneYawDegrees);
        double rearOffset = Mth.clamp(droneBbWidth * 0.55D, MIN_REAR_OFFSET, MAX_REAR_OFFSET);
        double attachmentHeight = Mth.clamp(droneBbHeight * 0.14D, MIN_BODY_ATTACHMENT_HEIGHT, MAX_BODY_ATTACHMENT_HEIGHT);

        return new Vec3(
                dronePosition.x - (forward.x * rearOffset),
                dronePosition.y + attachmentHeight,
                dronePosition.z - (forward.z * rearOffset)
        );
    }

    static Vec3[] buildCableCurvePoints(Vec3 renderStart, Vec3 renderEnd, int segmentCount) {
        int segments = Mth.clamp(segmentCount, 1, 256);
        Vec3 control = buildControlPoint(renderStart, renderEnd);
        Vec3[] points = new Vec3[segments + 1];

        for (int i = 0; i <= segments; i++) {
            double t = i / (double) segments;
            points[i] = sampleQuadratic(renderStart, control, renderEnd, t);
        }

        return points;
    }

    private static Vec3 buildControlPoint(Vec3 renderStart, Vec3 renderEnd) {
        double cableLength = renderStart.distanceTo(renderEnd);
        double sag = Mth.clamp((float) (MIN_SAG + (cableLength * 0.0065D)), (float) MIN_SAG, (float) MAX_SAG);
        Vec3 midpoint = renderStart.lerp(renderEnd, 0.5D);
        double floorClamp = Math.min(renderStart.y, renderEnd.y) - Math.min(MAX_GROUND_DIP, sag * 0.45D);
        double controlY = Math.max(midpoint.y - sag, floorClamp);
        return new Vec3(midpoint.x, controlY, midpoint.z);
    }

    private static Vec3 sampleQuadratic(Vec3 p0, Vec3 p1, Vec3 p2, double t) {
        double inverseT = 1.0D - t;
        double x = (inverseT * inverseT * p0.x) + (2.0D * inverseT * t * p1.x) + (t * t * p2.x);
        double y = (inverseT * inverseT * p0.y) + (2.0D * inverseT * t * p1.y) + (t * t * p2.y);
        double z = (inverseT * inverseT * p0.z) + (2.0D * inverseT * t * p1.z) + (t * t * p2.z);
        return new Vec3(x, y, z);
    }

    private static Vec3 horizontalForward(float droneYawDegrees) {
        double yawRadians = Math.toRadians(droneYawDegrees);
        double x = -Math.sin(yawRadians);
        double z = Math.cos(yawRadians);
        Vec3 forward = new Vec3(x, 0.0D, z);
        if (forward.lengthSqr() < 1.0E-6D) {
            return new Vec3(0.0D, 0.0D, 1.0D);
        }
        return forward.normalize();
    }
}
