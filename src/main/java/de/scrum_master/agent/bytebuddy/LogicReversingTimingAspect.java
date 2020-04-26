package de.scrum_master.agent.bytebuddy;

import net.bytebuddy.asm.Advice;

import java.lang.reflect.Method;

public class LogicReversingTimingAspect {
  @Advice.OnMethodEnter
  public static long recordStartTime() {
    return System.nanoTime();
  }

  @Advice.OnMethodExit
  public static void logDurationAndReverseResult(
    @Advice.Origin Method method,
    @Advice.Enter long start,
    @Advice.Return(readOnly = false) boolean returnValue
  ) {
    returnValue = !returnValue;
    System.out.println(method + " took " + (System.nanoTime() - start) / 1.0e6 + " ms");
  }
}
