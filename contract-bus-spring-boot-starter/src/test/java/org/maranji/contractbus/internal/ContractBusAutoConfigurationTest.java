package org.maranji.contractbus.internal;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import org.maranji.contractbus.ContractBus;
import org.maranji.contractbus.internal.fixture.HandledContract;
import org.maranji.contractbus.internal.fixture.HandledContractHandler;
import org.maranji.contractbus.internal.fixture.UnhandledContract;

import static org.assertj.core.api.Assertions.assertThat;

class ContractBusAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ContractBusAutoConfiguration.class))
            .withBean(HandledContractHandler.class)
            .withPropertyValues("contract-bus.base-packages=org.maranji.contractbus.internal.fixture");

    @Test
    void failsToStartWhenAContractOnTheClasspathHasNoHandler() {
        contextRunner.run(context -> assertThat(context)
                .hasFailed()
                .getFailure()
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(UnhandledContract.class.getName())
                .hasMessageNotContaining(HandledContract.class.getName()));
    }

    @Test
    void startsWhenTheUnhandledContractIsIgnored() {
        contextRunner
                .withPropertyValues("contract-bus.ignored-contracts=" + UnhandledContract.class.getName())
                .run(context -> assertThat(context.getBean(ContractBus.class).send(new HandledContract()))
                        .isEqualTo("handled"));
    }
}
