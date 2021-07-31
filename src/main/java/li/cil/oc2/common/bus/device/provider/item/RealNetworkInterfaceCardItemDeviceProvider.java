package li.cil.oc2.common.bus.device.provider.item;

import li.cil.oc2.api.bus.device.ItemDevice;
import li.cil.oc2.api.bus.device.provider.ItemDeviceQuery;
import li.cil.oc2.common.Config;
import li.cil.oc2.common.bus.device.item.NetworkInterfaceCardItemDevice;
import li.cil.oc2.common.bus.device.item.RealNetworkInterfaceCardItemDevice;
import li.cil.oc2.common.bus.device.provider.util.AbstractItemDeviceProvider;
import li.cil.oc2.common.item.Items;

import java.util.Optional;

public class RealNetworkInterfaceCardItemDeviceProvider extends AbstractItemDeviceProvider {
    public RealNetworkInterfaceCardItemDeviceProvider() {
        super(Items.REAL_NETWORK_INTERFACE_CARD);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected Optional<ItemDevice> getItemDevice(final ItemDeviceQuery query) {
        return query.getContainerTileEntity().map(tileEntity -> {
            try {
                return new RealNetworkInterfaceCardItemDevice(query.getItemStack());
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        });
    }

    @Override
    protected int getItemDeviceEnergyConsumption(final ItemDeviceQuery query) {
        return Config.networkInterfaceEnergyPerTick;
    }
}
