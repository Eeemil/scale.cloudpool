package com.elastisys.scale.cloudpool.vsphere.util;

import com.vmware.vim25.*;
import com.vmware.vim25.mo.ResourcePool;
import com.vmware.vim25.mo.VirtualMachine;
import org.joda.time.DateTime;

import java.rmi.RemoteException;
import java.util.Locale;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MockedVm {

    private VirtualMachine vm;
    private VirtualMachineRuntimeInfo runtimeInfo;
    private int key = 1;

    public MockedVm() {
        vm = mock(VirtualMachine.class);
        runtimeInfo = mock(VirtualMachineRuntimeInfo.class);
        when(vm.getRuntime()).thenReturn(runtimeInfo);
    }

    public MockedVm withName(String name) {
        when(vm.getName()).thenReturn(name);
        return this;
    }

    public MockedVm withLaunchTime(DateTime launchTime) {
        when(runtimeInfo.getBootTime()).thenReturn(launchTime.toCalendar(Locale.ENGLISH));
        return this;
    }

    public MockedVm withPowerState(VirtualMachinePowerState powerState) {
        when(runtimeInfo.getPowerState()).thenReturn(powerState);
        return this;
    }

    public MockedVm withResourcePool(String poolName) throws RemoteException {
        ResourcePool resourcePool = mock(ResourcePool.class);
        when(vm.getResourcePool()).thenReturn(resourcePool);
        when(resourcePool.getName()).thenReturn(poolName);
        return this;
    }

    public MockedVm withMachineSize(String machineSize) {
        VirtualMachineConfigInfo config = mock(VirtualMachineConfigInfo.class);
        VirtualHardware hardware = mock(VirtualHardware.class);
        when(vm.getConfig()).thenReturn(config);
        when(config.getHardware()).thenReturn(hardware);
        when(hardware.getNumCPU()).thenReturn(2);
        when(hardware.getMemoryMB()).thenReturn(1024);
        return this;
    }

    public MockedVm withPrivateIp(String privateIp) {
        GuestInfo guestInfo = mock(GuestInfo.class);
        when(vm.getGuest()).thenReturn(guestInfo);
        when(guestInfo.getIpAddress()).thenReturn(privateIp);
        return this;
    }

    public MockedVm withTag(com.elastisys.scale.cloudpool.vsphere.tag.Tag tag) throws RemoteException {
        CustomFieldDef cfd = new CustomFieldDef();
        cfd.setName(tag.getKey());
        cfd.setKey(key);
        CustomFieldDef[] cfdArr = { cfd };
        CustomFieldStringValue cfsv = new CustomFieldStringValue();
        cfsv.setKey(key);
        cfsv.setValue(tag.getValue());
        CustomFieldStringValue[] cfsvArr = { cfsv };
        when(vm.getAvailableField()).thenReturn(cfdArr);
        when(vm.getCustomValue()).thenReturn(cfsvArr);
        return this;
    }

    public VirtualMachine build() {
        return vm;
    }
}
