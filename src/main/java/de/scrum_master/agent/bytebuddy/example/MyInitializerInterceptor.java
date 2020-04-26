package de.scrum_master.agent.bytebuddy.example;

import net.bytebuddy.asm.Advice;

public class MyInitializerInterceptor {
  @Advice.OnMethodEnter
  public static void enter(@Advice.Origin("#t.#m") String method) {
    System.out.println("Intercepted Initia >> " + method);
  }
}
