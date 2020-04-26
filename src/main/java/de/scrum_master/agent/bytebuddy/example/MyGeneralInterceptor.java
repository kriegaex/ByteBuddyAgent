package de.scrum_master.agent.bytebuddy.example;

import net.bytebuddy.asm.Advice;

import java.lang.reflect.Method;

public class MyGeneralInterceptor {
  @Advice.OnMethodEnter
  public static void enter(@Advice.Origin Method method, @Advice.This Object thiz) {
    System.out.println("Intercepted Normal >> " + method);
  }
}
