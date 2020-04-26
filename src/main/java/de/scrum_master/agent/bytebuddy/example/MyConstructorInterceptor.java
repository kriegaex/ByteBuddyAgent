package de.scrum_master.agent.bytebuddy.example;

import net.bytebuddy.asm.Advice;

import java.lang.reflect.Constructor;

public class MyConstructorInterceptor {
  @Advice.OnMethodEnter
  public static void enter(@Advice.Origin Constructor<?> method) {
    System.out.println("Intercepted Constr >> " + method);
  }
}
