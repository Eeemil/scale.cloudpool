package com.elastisys.scale.cloudpool.vsphere.driver;

import com.elastisys.scale.cloudpool.api.NotFoundException;
import com.elastisys.scale.cloudpool.api.types.Machine;
import com.elastisys.scale.cloudpool.api.types.MachineState;
import com.elastisys.scale.cloudpool.api.types.MembershipStatus;
import com.elastisys.scale.cloudpool.api.types.ServiceState;
import com.elastisys.scale.cloudpool.commons.basepool.driver.CloudPoolDriverException;
import com.elastisys.scale.cloudpool.commons.basepool.driver.DriverConfig;
import com.elastisys.scale.cloudpool.commons.basepool.driver.StartMachinesException;
import com.elastisys.scale.cloudpool.vsphere.client.VsphereClient;
import com.elastisys.scale.cloudpool.vsphere.util.MockedVm;
import com.elastisys.scale.cloudpool.vsphere.util.TestUtils;
import com.elastisys.scale.commons.util.time.UtcTime;
import com.vmware.vim25.VirtualMachinePowerState;
import com.vmware.vim25.mo.VirtualMachine;
import jersey.repackaged.com.google.common.collect.Lists;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class TestVspherePoolDriverOperations {

    private VspherePoolDriver driver;
    private VsphereClient mockedClient = mock(VsphereClient.class);

    @Before
    public void setup() {
        String specificConfigPath = "config/valid-vsphere-config.json";
        DriverConfig configuration = TestUtils.loadDriverConfig(specificConfigPath);
        driver = new VspherePoolDriver(mockedClient);
        driver.configure(configuration);
    }

    @Test
    public void emptyListOfMachines() {
        assertTrue(driver.listMachines().isEmpty());
    }

    @Test
    public void listSingleMachine() throws RemoteException {
        String name = "vmName";
        List<VirtualMachine> vms = Lists.newArrayList(getMockedVM(name));
        when(mockedClient.getVirtualMachines(any())).thenReturn(vms);

        List<Machine> result = driver.listMachines();
        verify(mockedClient).getVirtualMachines(any());
        assertEquals(1, result.size());
        assertThat(result, is(MachinesMatcher.machines(name)));
    }

    @Test
    public void listMoreMachines() throws RemoteException {
        List<String> runningNames = Lists.newArrayList("vm1", "vm2", "vm3");
        List<String> pendingNames = Lists.newArrayList("vm4", "vm5");
        List<String> names = runningNames;
        names.addAll(pendingNames);

        List<VirtualMachine> runningVms = Lists.newArrayList();
        List<VirtualMachine> pendingVms = Lists.newArrayList();

        runningVms.addAll(getMockedVMs(runningNames));
        pendingVms.addAll(getMockedVMs(pendingNames));
        when(mockedClient.getVirtualMachines(any())).thenReturn(runningVms);
        when(mockedClient.pendingVirtualMachines()).thenReturn(pendingNames);

        List<Machine> result = driver.listMachines();
        verify(mockedClient).getVirtualMachines(any());
        verify(mockedClient).pendingVirtualMachines();
        assertEquals(names.size(), result.size());
        assertThat(result, is(new MachinesMatcher(names)));
    }

    @Test
    public void listMachinesWithDifferentStates() throws RemoteException {
        List<String> names = Lists.newArrayList("vmOn", "vmOff");
        List<VirtualMachine> vms = Lists.newArrayList();
        VirtualMachinePowerState poweredOn = VirtualMachinePowerState.poweredOn;
        VirtualMachinePowerState poweredOff = VirtualMachinePowerState.poweredOff;

        VirtualMachine vmOn = getMockedVM(names.get(0));
        VirtualMachine vmOff = getMockedVM(names.get(1));

        when(vmOn.getRuntime().getPowerState()).thenReturn(poweredOn);
        when(vmOff.getRuntime().getPowerState()).thenReturn(poweredOff);
        vms.add(vmOn);
        vms.add(vmOff);

        when(mockedClient.getVirtualMachines(any())).thenReturn(vms);

        List<Machine> result = driver.listMachines();
        verify(mockedClient).getVirtualMachines(any());
        assertEquals(names.size(), result.size());
        assertThat(result, is(new MachinesMatcher(names)));
    }

    @Test
    public void listPendingMachine() throws RemoteException {
        String name = "vmName";
        when(mockedClient.pendingVirtualMachines()).thenReturn(Lists.newArrayList(name));

        List<Machine> result = driver.listMachines();
        verify(mockedClient).pendingVirtualMachines();
        assertEquals(1, result.size());
        assertThat(result, is(MachinesMatcher.machines(name)));
        assertThat(result.get(0).getMachineState(), is(MachineState.PENDING));
    }

    /**
     * There is a race condition when listing machines so that a pending machine
     * may also be listed as running. We need to make sure that these machines
     * are not listed twice.
     */
    @Test
    public void machineBothPendingAndRunning() throws RemoteException {
        String name = "vm1";
        List<String> nameList = Lists.newArrayList(name);

        List<VirtualMachine> runningVms = Lists.newArrayList(getMockedVM(name));

        when(mockedClient.getVirtualMachines(any())).thenReturn(runningVms);
        when(mockedClient.pendingVirtualMachines()).thenReturn(nameList);

        List<Machine> result = driver.listMachines();
        assertThat(result.size(), is(1));
        assertThat(result, is(MachinesMatcher.machines(name)));
        assertThat(result.get(0).getMachineState(), is(MachineState.RUNNING));
    }

    @Test(expected = CloudPoolDriverException.class)
    public void failToGetMachines() throws CloudPoolDriverException, RemoteException {
        when(this.mockedClient.getVirtualMachines(any())).thenThrow(new RemoteException("API unreachable"));
        driver.listMachines();
    }

    @Test
    public void startSingleMachine() throws RemoteException {
        int count = 1;
        List<String> names = Lists.newArrayList("vm1");
        when(mockedClient.launchVirtualMachines(anyInt(), any())).thenReturn(names);

        List<Machine> result = driver.startMachines(count);
        assertThat(result, is(new MachinesMatcher(names)));
    }

    @Test
    public void startTwoMachines() throws RemoteException {
        int count = 2;
        List<String> names = Lists.newArrayList("vm1", "vm2");
        when(mockedClient.launchVirtualMachines(anyInt(), any())).thenReturn(names);

        List<Machine> result = driver.startMachines(count);
        assertThat(result, is(new MachinesMatcher(names)));
    }

    @Test(expected = StartMachinesException.class)
    public void failToStartMachines() throws RemoteException {
        int count = 2;
        when(mockedClient.launchVirtualMachines(anyInt(), any())).thenThrow(RemoteException.class);

        driver.startMachines(count);
    }

    @Test
    public void terminateMachine() throws RemoteException {
        String name = "vm1";
        doReturn(getMockedVMs(name)).when(mockedClient).getVirtualMachines(any());
        driver.terminateMachine(name);
        verify(mockedClient, times(1)).terminateVirtualMachines(Lists.newArrayList(name));
    }

    @Test(expected = CloudPoolDriverException.class)
    public void failToTerminateMachine() throws RemoteException {
        doReturn(getMockedVMs("vm1")).when(mockedClient).getVirtualMachines(any());
        doThrow(RemoteException.class).when(mockedClient).terminateVirtualMachines(any());
        driver.terminateMachine("vm1");
    }

    @Test(expected = NotFoundException.class)
    public void terminateNotFoundMachine() throws RemoteException {
        doReturn(getMockedVMs("vm1")).when(mockedClient).getVirtualMachines(any());
        driver.terminateMachine("vm2");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testAttachMachine() {
        driver.attachMachine("vm1");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testDetachMachine() {
        driver.detachMachine("vm1");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testSetServiceState() {
        driver.setServiceState("vm1", ServiceState.UNHEALTHY);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testSetMembershipStatus() {
        driver.setMembershipStatus("vm1", MembershipStatus.disposable());
    }

    private VirtualMachine getMockedVM(String name) throws RemoteException {
        String region = "region";
        DateTime launchTime = UtcTime.now();
        int numCpu = 2;
        int ram = 1024;
        String machineSize = String.format("cpu-%d-mem-%d", numCpu, ram);
        VirtualMachinePowerState poweredOn = VirtualMachinePowerState.poweredOn;

        return new MockedVm().withName(name).withLaunchTime(launchTime).withPowerState(poweredOn)
                .withResourcePool(region).withMachineSize(machineSize).build();
    }

    private List<VirtualMachine> getMockedVMs(String... names) throws RemoteException {
        return getMockedVMs(Arrays.asList(names));
    }

    private List<VirtualMachine> getMockedVMs(List<String> names) throws RemoteException {
        List<VirtualMachine> vms = Lists.newArrayListWithCapacity(names.size());
        for (String name : names) {
            vms.add(getMockedVM(name));
        }
        return vms;
    }
}
