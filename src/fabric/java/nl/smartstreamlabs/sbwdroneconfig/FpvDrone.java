package nl.smartstreamlabs.sbwdroneconfig;

import com.atsuishio.superbwarfare.entity.vehicle.DroneEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

public final class FpvDrone extends DroneEntity {
    public FpvDrone(EntityType<? extends DroneEntity> type, Level level) {
        super(type, level);
    }

    @Override public Item droneItem() { return DroneWarfare.FPV_ITEM; }
}
