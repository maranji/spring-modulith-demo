package org.maranji.actionrouter.internal;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import org.maranji.actionrouter.ActionRouter;
import org.maranji.actionrouter.internal.fixture.HandledAction;
import org.maranji.actionrouter.internal.fixture.HandledActionHandler;
import org.maranji.actionrouter.internal.fixture.UnhandledAction;

import static org.assertj.core.api.Assertions.assertThat;

class ActionRouterAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ActionRouterAutoConfiguration.class))
            .withBean(HandledActionHandler.class)
            .withPropertyValues("action-router.base-packages=org.maranji.actionrouter.internal.fixture");

    @Test
    void failsToStartWhenAActionOnTheClasspathHasNoHandler() {
        contextRunner.run(context -> assertThat(context)
                .hasFailed()
                .getFailure()
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(UnhandledAction.class.getName())
                .hasMessageNotContaining(HandledAction.class.getName()));
    }

    @Test
    void startsWhenTheUnhandledActionIsIgnored() {
        contextRunner
                .withPropertyValues("action-router.ignored-actions=" + UnhandledAction.class.getName())
                .run(context -> assertThat(context.getBean(ActionRouter.class).route(new HandledAction()))
                        .isEqualTo("handled"));
    }
}
