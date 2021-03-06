package com.elastisys.scale.cloudpool.openstack.functions;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;
import org.openstack4j.model.compute.Server.Status;

import com.elastisys.scale.cloudpool.api.types.MachineState;
import com.elastisys.scale.cloudpool.openstack.functions.StatusToMachineState;

/**
 * Exercises the {@link StatusToMachineState} class.
 */
public class TestStatusToMachineState {

    @Test
    public void testStateConversion() {
        assertThat(convert(Status.ACTIVE), is(MachineState.RUNNING));
        assertThat(convert(Status.ERROR), is(MachineState.RUNNING));
        assertThat(convert(Status.PASSWORD), is(MachineState.RUNNING));

        assertThat(convert(Status.BUILD), is(MachineState.PENDING));
        assertThat(convert(Status.REBUILD), is(MachineState.PENDING));
        assertThat(convert(Status.REBOOT), is(MachineState.PENDING));
        assertThat(convert(Status.HARD_REBOOT), is(MachineState.PENDING));
        assertThat(convert(Status.RESIZE), is(MachineState.PENDING));
        assertThat(convert(Status.REVERT_RESIZE), is(MachineState.PENDING));
        assertThat(convert(Status.VERIFY_RESIZE), is(MachineState.PENDING));

        assertThat(convert(Status.STOPPED), is(MachineState.TERMINATED));
        assertThat(convert(Status.SHUTOFF), is(MachineState.TERMINATED));
        assertThat(convert(Status.PAUSED), is(MachineState.TERMINATED));
        assertThat(convert(Status.SUSPENDED), is(MachineState.TERMINATED));
        assertThat(convert(Status.DELETED), is(MachineState.TERMINATED));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConvertUnrecognizedStatus() {
        convert(Status.UNRECOGNIZED);
    }

    private static MachineState convert(Status state) {
        return new StatusToMachineState().apply(state);
    }
}
