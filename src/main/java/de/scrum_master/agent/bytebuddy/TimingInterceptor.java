package de.scrum_master.agent.bytebuddy;

import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperCall;

import java.lang.reflect.Method;
import java.util.concurrent.Callable;

public class TimingInterceptor {
  @RuntimeType
  public static Object intercept(@Origin Method method, @SuperCall Callable<?> callable) throws Exception {
    long start = System.nanoTime();
    try {
      return callable.call();
    } finally {
      System.out.println(method + " took " + (System.nanoTime() - start) / 1.0e6 + " ms");
    }
  }
}
