package de.scrum_master.agent.bytebuddy.example;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.matcher.ElementMatchers;

import java.lang.instrument.Instrumentation;

import static net.bytebuddy.matcher.ElementMatchers.*;

/**
 *  Found code for advice-type interceptors by chance inside a ByteBuddy issue:
 *  https://github.com/raphw/byte-buddy/issues/276
 */
public class ExampleAgent {
  public static void premain(String options, Instrumentation instrumentation) {
    new AgentBuilder.Default()
      .ignore(nameStartsWith("net.bytebuddy."))
      .type(ElementMatchers.nameContains("java.net."))
      .transform((builder, td, cl, m) -> builder.visit(Advice.to(MyGeneralInterceptor.class).on(not(isStatic()).and(not(isConstructor())))))
      .transform((builder, td, cl, m) -> builder.visit(Advice.to(MyStaticInterceptor.class).on(isStatic().and(not(isTypeInitializer())).and(not(isConstructor())))))
      .transform((builder, td, cl, m) -> builder.visit(Advice.to(MyInitializerInterceptor.class).on(isTypeInitializer())))
      .transform((builder, td, cl, m) -> builder.visit(Advice.to(MyConstructorInterceptor.class).on(isConstructor())))
      .installOn(instrumentation);
  }
}
