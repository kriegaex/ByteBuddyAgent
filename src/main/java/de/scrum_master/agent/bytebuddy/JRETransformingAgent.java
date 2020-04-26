package de.scrum_master.agent.bytebuddy;

import com.sun.tools.attach.VirtualMachine;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.agent.builder.ResettableClassFileTransformer;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.matcher.ElementMatchers;

import java.lang.instrument.ClassDefinition;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Map;

import static net.bytebuddy.matcher.ElementMatchers.*;

public class JRETransformingAgent {
  /**
   * Dynamically attach this Java agent class to the running local JVM. This method can be used
   * as an alternative to the command line parameter {@code -javaagent:/path/to/thisAgent.jar}.
   * In order to activate the attach API, up to Java 8 {@code tools.jar} from the JDK needs to be
   * on the classpath. For Java 9+ please set property {@code -Djdk.attach.allowAttachSelf}.
   *
   * @param jarFilePath path to the Java agent JAR containing this class
   * @return {@code true} if the agent could be attached successfully; {@code false} otherwise
   */
  public static boolean dynamicallyLoadAgent(String jarFilePath) {
    String nameOfRunningVM = ManagementFactory.getRuntimeMXBean().getName();
    int p = nameOfRunningVM.indexOf('@');
    String pid = nameOfRunningVM.substring(0, p);
    try {
      VirtualMachine vm = VirtualMachine.attach(pid);
      vm.loadAgent(jarFilePath);
      vm.detach();
    } catch (Exception e) {
      System.out.println(e);
      return false;
    }
    return true;
  }

  public static void agentmain(String options, Instrumentation instrumentation) {
    premain(options, instrumentation);
  }

  public static void premain(String options, Instrumentation instrumentation) {
    registerApplicationClassTransformer(instrumentation);
    redefineClasses(instrumentation, String.class);
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

  private static void redefineClasses(Instrumentation instrumentation, Class<?>... classes) {
    Map<Class<?>, byte[]> transformedClasses = transformClasses(instrumentation, classes);
    for (Class<?> classToRedefine : classes) {
      try {
        instrumentation.redefineClasses(new ClassDefinition(classToRedefine, transformedClasses.get(classToRedefine)));
      } catch (Exception e) {
        System.err.println("Cannot redefine " + classToRedefine + " - reason is:");
        e.printStackTrace();
      }
    }
  }

  private static Map<Class<?>, byte[]> transformClasses(Instrumentation instrumentation, Class<?>... classes) {
    BytecodeProvider bytecodeProvider = null;
    ResettableClassFileTransformer jreClassTransformer = null;
    try {
      bytecodeProvider = registerBytecodeProvider(instrumentation);
      jreClassTransformer = registerJREClassTransformer(instrumentation);
      Map<Class<?>, byte[]> transformedClasses = new HashMap<>();
      for (Class<?> classToTransform : classes) {
        try {
          instrumentation.retransformClasses(classToTransform);
          byte[] originalBytes = bytecodeProvider.getClassFile();
          byte[] transformedBytes = jreClassTransformer.transform(
            classToTransform.getClassLoader(),
            classToTransform.getName(),
            classToTransform,
            classToTransform.getProtectionDomain(),
            originalBytes
          );
          transformedClasses.put(classToTransform, transformedBytes);
        } catch (IllegalClassFormatException | UnmodifiableClassException e) {
          System.err.println("Cannot transform " + classToTransform + " - reason is:");
          e.printStackTrace();
        }
      }
      return transformedClasses;
    } finally {
      if (bytecodeProvider != null)
        instrumentation.removeTransformer(bytecodeProvider);
      if (jreClassTransformer != null)
        instrumentation.removeTransformer(jreClassTransformer);
    }
  }

  private static BytecodeProvider registerBytecodeProvider(Instrumentation instrumentation) {
    BytecodeProvider bytecodeProvider = new BytecodeProvider();
    instrumentation.addTransformer(bytecodeProvider, true);
    return bytecodeProvider;
  }

  /**
   * TODO: evolve from hard-coded class/method matching and instrumentation to external parametrisation
   */
  private static ResettableClassFileTransformer registerJREClassTransformer(Instrumentation instrumentation) {
    return new AgentBuilder.Default()
      .with(AgentBuilder.Listener.StreamWriting.toSystemError().withTransformationsOnly())
      .disableClassFormatChanges()
      .ignore(none())
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
