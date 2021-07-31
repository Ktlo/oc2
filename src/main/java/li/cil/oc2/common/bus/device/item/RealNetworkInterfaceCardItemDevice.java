package li.cil.oc2.common.bus.device.item;

import com.google.common.eventbus.Subscribe;
import li.cil.oc2.api.bus.device.ItemDevice;
import li.cil.oc2.api.bus.device.vm.VMDevice;
import li.cil.oc2.api.bus.device.vm.VMDeviceLoadResult;
import li.cil.oc2.api.bus.device.vm.context.VMContext;
import li.cil.oc2.api.bus.device.vm.event.VMPausingEvent;
import li.cil.oc2.api.bus.device.vm.event.VMResumingRunningEvent;
import li.cil.oc2.common.bus.device.util.IdentityProxy;
import li.cil.oc2.common.bus.device.util.OptionalAddress;
import li.cil.oc2.common.bus.device.util.OptionalInterrupt;
import li.cil.oc2.common.serialization.NBTSerialization;
import li.cil.oc2.common.util.NBTTagIds;
import li.cil.sedna.device.virtio.VirtIONetworkDevice;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.Direction;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.Arrays;

@SuppressWarnings("UnstableApiUsage")
public class RealNetworkInterfaceCardItemDevice extends IdentityProxy<ItemStack> implements VMDevice, ItemDevice, ICapabilityProvider {
    private static final String DEVICE_TAG_NAME = "device";
    private static final String ADDRESS_TAG_NAME = "address";
    private static final String INTERRUPT_TAG_NAME = "interrupt";

    ///////////////////////////////////////////////////////////////

    private VirtIONetworkDevice device;
    private volatile boolean isRunning;

    private final OptionalAddress address = new OptionalAddress();
    private final OptionalInterrupt interrupt = new OptionalInterrupt();
    private CompoundNBT deviceTag;

    ///////////////////////////////////////////////////////////////

    public RealNetworkInterfaceCardItemDevice(final ItemStack identity) throws SocketException {
        super(identity);
    }

    ///////////////////////////////////////////////////////////////

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(final Capability<T> cap, @Nullable final Direction side) {
        return LazyOptional.empty();
    }

    @Override
    public VMDeviceLoadResult mount(final VMContext context) {
        device = new VirtIONetworkDevice(context.getMemoryMap());

        if (!address.claim(context, device)) {
            return VMDeviceLoadResult.fail();
        }

        if (interrupt.claim(context)) {
            device.getInterrupt().set(interrupt.getAsInt(), context.getInterruptController());
        } else {
            return VMDeviceLoadResult.fail();
        }

        if (deviceTag != null) {
            NBTSerialization.deserialize(deviceTag, device);
        }

        context.getEventBus().register(this);

        try {
            setup();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return VMDeviceLoadResult.success();
    }

    @Override
    public void unmount() {
        suspend();
        isRunning = false;
        address.clear();
        interrupt.clear();
        MinecraftForge.EVENT_BUS.unregister(this);
    }

    @Override
    public void suspend() {
        device = null;
    }

    @Subscribe
    public void handlePausingEvent(final VMPausingEvent event) {
        isRunning = false;
        MinecraftForge.EVENT_BUS.unregister(this);
    }

    @Subscribe
    public void handleResumingRunningEvent(final VMResumingRunningEvent event) {
        isRunning = true;
        MinecraftForge.EVENT_BUS.register(this);
    }

    @Override
    public CompoundNBT serializeNBT() {
        final CompoundNBT tag = new CompoundNBT();

        if (device != null) {
            deviceTag = NBTSerialization.serialize(device);
        }
        if (deviceTag != null) {
            tag.put(DEVICE_TAG_NAME, deviceTag);
        }
        if (address.isPresent()) {
            tag.putLong(ADDRESS_TAG_NAME, address.getAsLong());
        }
        if (interrupt.isPresent()) {
            tag.putInt(INTERRUPT_TAG_NAME, interrupt.getAsInt());
        }

        return tag;
    }

    @Override
    public void deserializeNBT(final CompoundNBT tag) {
        if (tag.contains(DEVICE_TAG_NAME, NBTTagIds.TAG_COMPOUND)) {
            deviceTag = tag.getCompound(DEVICE_TAG_NAME);
        }
        if (tag.contains(ADDRESS_TAG_NAME, NBTTagIds.TAG_LONG)) {
            address.set(tag.getLong(ADDRESS_TAG_NAME));
        }
        if (tag.contains(INTERRUPT_TAG_NAME, NBTTagIds.TAG_INT)) {
            interrupt.set(tag.getInt(INTERRUPT_TAG_NAME));
        }
    }

    private final static SocketAddress server = new InetSocketAddress("127.0.0.1", 1234);

    private DatagramChannel channel = null;

    private void setup() throws IOException {
        channel = DatagramChannel.open(StandardProtocolFamily.INET);
        channel.configureBlocking(false);
        byte[] b = new byte[0];
        channel.send(ByteBuffer.wrap(b), server);
    }

    private static final int FRAME_SIZE = 1500;

    private final byte[] recvBytes = new byte[FRAME_SIZE];
    private final ByteBuffer recvBuffer = ByteBuffer.wrap(recvBytes);

    @SubscribeEvent
    public void onTick(TickEvent.ServerTickEvent event) {
        try {
            Object result = channel.receive(recvBuffer);
            if (result != null) {
                device.writeEthernetFrame(Arrays.copyOf(recvBytes, recvBuffer.position()));
                recvBuffer.rewind();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            byte[] frame = device.readEthernetFrame();
            if (frame != null) {
                ByteBuffer sendBuffer = ByteBuffer.wrap(frame);
                channel.send(sendBuffer, server);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
