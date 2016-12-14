package com.elastisys.scale.cloudpool.azure.driver;

import static com.elastisys.scale.cloudpool.azure.driver.AzureTestUtils.invalidConfig;
import static com.elastisys.scale.cloudpool.azure.driver.AzureTestUtils.validConfig;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;

import com.elastisys.scale.cloudpool.api.ApiVersion;
import com.elastisys.scale.cloudpool.api.types.CloudPoolMetadata;
import com.elastisys.scale.cloudpool.api.types.CloudProviders;
import com.elastisys.scale.cloudpool.api.types.MembershipStatus;
import com.elastisys.scale.cloudpool.api.types.ServiceState;
import com.elastisys.scale.cloudpool.azure.driver.client.AzureClient;
import com.elastisys.scale.cloudpool.azure.driver.config.AzurePoolDriverConfig;
import com.elastisys.scale.commons.json.JsonUtils;

/**
 * Exercise {@link AzurePoolDriver}.
 */
public class TestAzurePoolDriver {

    /** Mock azure client being used by driver. */
    private AzureClient clientMock = mock(AzureClient.class);
    /** Object under test. */
    private AzurePoolDriver driver;

    @Before
    public void beforeTestMethod() {
        this.driver = new AzurePoolDriver(this.clientMock);
    }

    /**
     * The pool driver should be able to provide some metadata about itself.
     */
    @Test
    public void metadata() {
        assertThat(this.driver.getMetadata(),
                is(new CloudPoolMetadata(CloudProviders.AZURE, Arrays.asList(ApiVersion.LATEST))));
    }

    /**
     * Ensure that a set configuration propagates to the {@link AzureClient} set
     * for the {@link AzurePoolDriver}.
     */
    @Test
    public void configure() {
        verifyZeroInteractions(this.clientMock);
        assertThat(this.driver.config(), is(nullValue()));

        this.driver.configure(validConfig());

        AzurePoolDriverConfig expectedDriverConfig = JsonUtils.toObject(validConfig().getCloudPool().getDriverConfig(),
                AzurePoolDriverConfig.class);

        // verify that config was set on driver
        assertThat(this.driver.config(), is(expectedDriverConfig));
        // verify that the pool driver config was passed along to the client
        verify(this.clientMock).configure(expectedDriverConfig);
    }

    /**
     * When passed an illegal configuration, no part of the configuration should
     * be applied.
     */
    @Test
    public void onIllegalConfig() {
        try {
            this.driver.configure(invalidConfig());
            fail("expected to fail config validation");
        } catch (IllegalArgumentException e) {
            // expected
        }

        assertThat(this.driver.config(), is(nullValue()));
        // configuration should not have been applied on client
        verifyZeroInteractions(this.clientMock);
    }

    // TODO: AzurePoolDriver tests for listMachines (success and failure)
    // TODO: AzurePoolDriver tests for startMachines (success and failure)
    // TODO: AzurePoolDriver tests for terminateMachine (success and failure)
    // TODO: AzurePoolDriver tests for attachMachine (success and failure)
    // TODO: AzurePoolDriver tests for detachMachine (success and failure)
    // TODO: AzurePoolDriver tests for setServiceState (success and failure)
    // TODO: AzurePoolDriver tests for setMembershipStatus (success and failure)

    @Test(expected = IllegalStateException.class)
    public void listMachinesBeforeConfigured() {
        this.driver.listMachines();
    }

    @Test(expected = IllegalStateException.class)
    public void attachgMachinesBeforeConfigured() {
        this.driver.attachMachine("id");
    }

    @Test(expected = IllegalStateException.class)
    public void detachMachinesBeforeConfigured() {
        this.driver.detachMachine("id");
    }

    @Test(expected = IllegalStateException.class)
    public void getPoolNameBeforeConfigured() {
        this.driver.getPoolName();
    }

    @Test(expected = IllegalStateException.class)
    public void setMembershipStatusBeforeConfigured() {
        this.driver.setMembershipStatus("id", MembershipStatus.blessed());
    }

    @Test(expected = IllegalStateException.class)
    public void setServiceStateBeforeConfigured() {
        this.driver.setServiceState("id", ServiceState.IN_SERVICE);
    }

    @Test(expected = IllegalStateException.class)
    public void startMachinesBeforeConfigured() {
        this.driver.startMachines(1, validConfig().getScaleOutConfig());
    }

    @Test(expected = IllegalStateException.class)
    public void terminateMachineBeforeConfigured() {
        this.driver.terminateMachine("id");
    }

}