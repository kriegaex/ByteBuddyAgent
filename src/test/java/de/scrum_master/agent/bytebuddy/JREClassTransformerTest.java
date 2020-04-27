package de.scrum_master.agent.bytebuddy;

import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.agent.builder.ResettableClassFileTransformer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.PrintStream;
import java.lang.instrument.Instrumentation;
import java.util.List;

import static org.junit.Assert.*;

/**
 * This is an integration test because it needs an up-to-date agent JAR which is created during the
 * Maven 'package' phase and thus is ready for phase 'integration-test' but not for 'test' yet.
 */
public class JREClassTransformerTest {
  private final static PrintStream systemOutOriginal = System.out;

  private String lastPrintlnMessage;
  private PrintStream systemOutSpy;

  @Before
  public void setUp() {
    lastPrintlnMessage = "";

    systemOutSpy = new PrintStream(systemOutOriginal) {
      @Override
      public void println(String message) {
        super.println(message);
        lastPrintlnMessage = message;
      }

      @Override
      public void close() {
        // Don't close delegate!
      }
    };
    System.setOut(systemOutSpy);
  }

  @After
  public void tearDown() {
    System.setOut(systemOutOriginal);
    systemOutSpy.close();
  }

  @Test
  public void loadedJREClassCanBeRedefined() {
    // Normal behaviour before loading agent
    assertFalse("foo".matches("bar"));
    assertEquals("", lastPrintlnMessage);
    assertTrue("foo".matches("foo"));
    assertEquals("", lastPrintlnMessage);

    // Apply bytecode transformation
    Instrumentation instrumentation = ByteBuddyAgent.install();
    List<ResettableClassFileTransformer> transformers = JREClassTransformer.perform(instrumentation);

    // After redefinition the logic of String.matches(String regex) is reversed and
    // timing messages are being printed to the system console
    assertTrue("foo".matches("bar"));
    assertTrue(lastPrintlnMessage.contains("String.matches(java.lang.String) took "));
    assertFalse("foo".matches("foo"));
    assertTrue(lastPrintlnMessage.contains("String.matches(java.lang.String) took "));

    // Check application class transformer too
    lastPrintlnMessage = "";
    new MyTimed().doSomethingElse();
    assertEquals("", lastPrintlnMessage);
    new MyTimed().doSomething(11);
    assertTrue(lastPrintlnMessage.contains("MyTimed.doSomething(int) took "));

    // Try to reset bytecode transformation, which only works for the JRE transformer which
    // used REDEFINITION strategy and disableClassFormatChanges() before. The application
    // classes were structurally altered during class-loading and cannot be reset to original
    // once they have been loaded.
    for (ResettableClassFileTransformer transformer : transformers)
      transformer.reset(instrumentation, AgentBuilder.RedefinitionStrategy.REDEFINITION);

    // Reset last printed message
    lastPrintlnMessage = "";

    // Normal behaviour for JRE classes after resetting transformers
    assertFalse("foo".matches("bar"));
    assertEquals("", lastPrintlnMessage);
    assertTrue("foo".matches("foo"));
    assertEquals("", lastPrintlnMessage);

    // But still transformation is active for application classes
    new MyTimed().doSomething(11);
    assertTrue(lastPrintlnMessage.contains("MyTimed.doSomething(int) took "));
  }

}
