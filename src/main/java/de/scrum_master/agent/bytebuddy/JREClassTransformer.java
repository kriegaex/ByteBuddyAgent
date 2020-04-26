package de.scrum_master.agent.bytebuddy;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.agent.builder.ResettableClassFileTransformer;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.matcher.ElementMatchers;

import java.lang.instrument.Instrumentation;
import java.util.Arrays;
import java.util.List;

import static net.bytebuddy.matcher.ElementMatchers.*;

public class JREClassTransformer {
  public static List<ResettableClassFileTransformer> perform(Instrumentation instrumentation) {
    return Arrays.asList(
      registerApplicationClassTransformer(instrumentation),
      registerJREClassTransformer(instrumentation)
    );
  }

  /**
   * TODO: evolve from hard-coded class/method matching and instrumentation to external parametrisation
   */
  private static ResettableClassFileTransformer registerApplicationClassTransformer(Instrumentation instrumentation) {
    return new AgentBuilder.Default()
      .with(AgentBuilder.Listener.StreamWriting.toSystemError().withTransformationsOnly())
      .type(nameEndsWith("Timed"))
      .transform((builder, typeDescription, classLoader, module) -> builder
        .method(ElementMatchers.nameEndsWith("doSomething"))
        .intercept(MethodDelegation.to(TimingInterceptor.class))
      )
      .installOn(instrumentation);
  }

  /**
   * TODO: evolve from hard-coded class/method matching and instrumentation to external parametrisation
   */
  private static ResettableClassFileTransformer registerJREClassTransformer(Instrumentation instrumentation) {
    return new AgentBuilder.Default()
      .with(AgentBuilder.Listener.StreamWriting.toSystemError().withTransformationsOnly())
      .ignore(none())
      .disableClassFormatChanges()
      .with(AgentBuilder.RedefinitionStrategy.REDEFINITION)
      .with(AgentBuilder.TypeStrategy.Default.REDEFINE)
      .type(named("java.lang.String"))
      .transform((builder, typeDescription, classLoader, module) -> builder
        .visit(Advice
          .to(LogicReversingTimingAspect.class)
          .on(nameEndsWith("matches").and(takesArguments(String.class)))
        )
      )
      .installOn(instrumentation);
  }

}
