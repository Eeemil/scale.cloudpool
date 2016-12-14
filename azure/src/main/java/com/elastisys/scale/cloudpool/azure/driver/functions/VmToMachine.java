package com.elastisys.scale.cloudpool.azure.driver.functions;

import static com.elastisys.scale.cloudpool.azure.driver.Constants.MEMBERSHIP_STATUS_TAG;
import static com.elastisys.scale.cloudpool.azure.driver.Constants.SERVICE_STATE_TAG;

import java.util.List;
import java.util.function.Function;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.cloudpool.api.types.CloudProviders;
import com.elastisys.scale.cloudpool.api.types.Machine;
import com.elastisys.scale.cloudpool.api.types.Machine.Builder;
import com.elastisys.scale.cloudpool.api.types.MachineState;
import com.elastisys.scale.cloudpool.api.types.MembershipStatus;
import com.elastisys.scale.cloudpool.api.types.ServiceState;
import com.elastisys.scale.commons.json.JsonUtils;
import com.microsoft.azure.management.batch.ProvisioningState;
import com.microsoft.azure.management.compute.DiskInstanceView;
import com.microsoft.azure.management.compute.InstanceViewStatus;
import com.microsoft.azure.management.compute.PowerState;
import com.microsoft.azure.management.compute.VirtualMachine;
import com.microsoft.azure.management.network.NetworkInterface;
import com.microsoft.azure.management.network.NicIpConfiguration;

public class VmToMachine implements Function<VirtualMachine, Machine> {
    static Logger LOG = LoggerFactory.getLogger(VmToMachine.class);

    @Override
    public Machine apply(VirtualMachine vm) {
        Builder builder = Machine.builder();

        builder.id(vm.id());
        builder.cloudProvider(CloudProviders.AZURE);
        builder.region(vm.regionName());
        builder.machineSize(vm.size().toString());
        NetworkInterface nic = vm.getPrimaryNetworkInterface();
        builder.privateIp(nic.primaryPrivateIp());
        NicIpConfiguration primaryIpConfig = nic.primaryIpConfiguration();
        if (primaryIpConfig.publicIpAddressId() != null) {
            String publicIp = primaryIpConfig.getPublicIpAddress().ipAddress();
            if (publicIp != null) {
                builder.publicIp(publicIp);
            }
        }
        builder.launchTime(extractLaunchTime(vm));
        builder.machineState(extractMachineState(vm));

        // extract membership status if tag has been set on server
        MembershipStatus membershipStatus = MembershipStatus.defaultStatus();
        if (vm.tags().containsKey(MEMBERSHIP_STATUS_TAG)) {
            membershipStatus = JsonUtils.toObject(JsonUtils.parseJsonString(vm.tags().get(MEMBERSHIP_STATUS_TAG)),
                    MembershipStatus.class);
        }
        builder.membershipStatus(membershipStatus);

        // extract service state if tag has been set on server
        ServiceState serviceState = ServiceState.UNKNOWN;
        if (vm.tags().containsKey(SERVICE_STATE_TAG)) {
            serviceState = ServiceState.valueOf(vm.tags().get(SERVICE_STATE_TAG));
        }
        builder.serviceState(serviceState);

        return builder.build();
    }

    /**
     * Translates an Azure VM's provisioning state and power state to a
     * {@link MachineState}.
     *
     * @param vm
     * @return
     */
    private MachineState extractMachineState(VirtualMachine vm) {
        if (LOG.isTraceEnabled()) {
            LOG.trace("vm states: provisioningState: {}, powerState: {}", vm.provisioningState(), vm.powerState());
        }
        // It _appears_ to be the case that a VM goes through a sequence of
        // statuses, starting with a provisioningState, and then (possibly)
        // entering a power state

        // TODO: re-iterate the logic of determining Azure machine state

        // powerState may be null (probably if provisioning is incomplete or
        // failed)
        PowerState powerState = vm.powerState();
        if (powerState != null) {
            switch (powerState) {
            case STARTING:
                return MachineState.PENDING;
            case RUNNING:
                return MachineState.RUNNING;
            case DEALLOCATED:
                return MachineState.TERMINATED;
            case DEALLOCATING:
                return MachineState.TERMINATING;
            default:
                LOG.warn("VM found to be in unrecognized powerState: {}", powerState.name());
            }
        }

        // no powerState. fall back to looking at provisioningState.
        ProvisioningState provisioningState = ProvisioningState.fromString(vm.provisioningState());
        switch (provisioningState) {
        case INVALID:
            // set to pending to prevent cloud pool from launching additional
            // servers which might fail?
            return MachineState.PENDING;
        case CREATING:
            return MachineState.PENDING;
        case DELETING:
            return MachineState.TERMINATING;
        case SUCCEEDED:
            // this should never happen (if provisioning was successful, a power
            // state should be set)
            return MachineState.RUNNING;
        case FAILED:
            // set to pending to prevent cloud pool from launching additional
            // servers which might fail
            return MachineState.PENDING;
        case CANCELLED:
            return MachineState.TERMINATED;
        default:
            LOG.warn("VM found to be in unrecognized provisioningState: {}", powerState.name());
        }

        throw new AzureVmMetadataException(String.format(
                "failed to determine machineState for VM %s with powerState '%s' and provisioningState '%s'", vm.id(),
                powerState, provisioningState.toString()));
    }

    /**
     * Tries to determine the start time of an Azure VM.
     *
     * @param vm
     * @return
     */
    private DateTime extractLaunchTime(VirtualMachine vm) {
        // TODO: re-iterate the logic of determining Azure vm start time

        // since there is no obvious way of determining this, look at the
        // provisioning time stamp on the OS disk
        String osDiskName = vm.storageProfile().osDisk().name();
        List<DiskInstanceView> vmDisks = vm.instanceView().disks();
        for (DiskInstanceView vmDisk : vmDisks) {
            if (vmDisk.name().equals(osDiskName)) {
                for (InstanceViewStatus status : vmDisk.statuses()) {
                    if (status.code().toLowerCase().contains("provisioningstate")) {
                        return status.time();
                    }
                }
            }
        }
        throw new AzureVmMetadataException(
                String.format("failed to determine launchTime for VM %s by looking at the OS disk timestamp", vm.id()));
    }

}