package de.scrum_master.agent.bytebuddy.example;

import net.bytebuddy.asm.Advice;

import java.lang.reflect.Method;

public class MyStaticInterceptor {
  @Advice.OnMethodEnter
  public static void enter(@Advice.Origin Method method) {
    System.out.println("Intercepted Static >> " + method);
  }
}
