package com.elastisys.scale.cloudpool.vsphere;

import com.elastisys.scale.cloudpool.commons.basepool.driver.DriverConfig;
import com.elastisys.scale.cloudpool.vsphere.client.VsphereClient;
import com.elastisys.scale.cloudpool.vsphere.client.impl.StandardVsphereClient;
import com.elastisys.scale.cloudpool.vsphere.driver.config.VsphereApiSettings;
import com.elastisys.scale.cloudpool.vsphere.driver.config.VsphereProvisioningTemplate;
import com.elastisys.scale.cloudpool.vsphere.tag.Tag;
import com.elastisys.scale.cloudpool.vsphere.tag.impl.ScalingTag;
import com.elastisys.scale.cloudpool.vsphere.tag.impl.VsphereTag;
import com.elastisys.scale.commons.json.JsonUtils;
import com.google.common.collect.Lists;
import com.vmware.vim25.mo.Task;
import com.vmware.vim25.mo.VirtualMachine;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.rmi.RemoteException;
import java.util.List;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class IntegrationTestClient {

    private static VsphereApiSettings vsphereApiSettings;
    private static VsphereProvisioningTemplate vsphereProvisioningTemplate;
    private static VsphereClient vsphereClient;

    private static String testTagValue = "VsphereClientIntegrationTest";

    @BeforeClass
    public static void setUpBeforeClass() {
        DriverConfig driverConfig = JsonUtils.toObject(JsonUtils.parseJsonResource("myconfig.json"), DriverConfig.class);
        vsphereApiSettings = driverConfig.parseCloudApiSettings(VsphereApiSettings.class);
        vsphereProvisioningTemplate = driverConfig.parseProvisioningTemplate(VsphereProvisioningTemplate.class);
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        List<Tag> tags = Lists.newArrayList();
        tags.add(new VsphereTag(ScalingTag.CLOUD_POOL, testTagValue));
        List<VirtualMachine> machines = vsphereClient.getVirtualMachines(tags);

        //power off
        List<Task> powerOffTasks = Lists.newArrayList();
        for(VirtualMachine machine : machines) {
            powerOffTasks.add(machine.powerOffVM_Task());
        }
        for(Task task : powerOffTasks) {
            task.waitForTask();
        }

        // destroy
        List<Task> destroyTasks = Lists.newArrayList();
        for(VirtualMachine machine : machines) {
            destroyTasks.add(machine.destroy_Task());
        }
        for(Task task : destroyTasks) {
            task.waitForTask();
        }
    }

    @Before
    public void setUp() throws RemoteException {
        vsphereClient = new StandardVsphereClient();
        vsphereClient.configure(vsphereApiSettings, vsphereProvisioningTemplate);
    }

    @Test
    public void shouldNotGetVirtualMachinesForTagThatDoesNotExist() throws RemoteException {
        List<Tag> tags = Lists.newArrayList();
        tags.add(new VsphereTag(ScalingTag.CLOUD_POOL, "NoSuchPool"));
        List<VirtualMachine> virtualMachines = vsphereClient.getVirtualMachines(tags);
        assertTrue(virtualMachines.isEmpty());
    }

    @Test
    public void shouldGetVirtualMachineWithoutTagRequirements() throws RemoteException {
        // this test assumes that there is at least one virtual machine or template ManagedEntity on the server
        List<VirtualMachine> virtualMachines = vsphereClient.getVirtualMachines(Lists.newArrayList());
        assertFalse(virtualMachines.isEmpty());
    }

    @Test
    public void shouldLaunchNewMachines() throws RemoteException {
        List<Tag> tags = Lists.newArrayList();
        tags.add(new VsphereTag(ScalingTag.CLOUD_POOL, testTagValue));
        List<VirtualMachine> virtualMachines = vsphereClient.launchVirtualMachines(1, tags);
        assertEquals(virtualMachines.size(), 1);
    }

    @Test
    public void shouldDestroyMachines() throws RemoteException {

    }
}
