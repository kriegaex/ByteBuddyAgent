package de.scrum_master.agent.bytebuddy;

import net.bytebuddy.agent.ByteBuddyAgent;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.PrintStream;

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
    ByteBuddyAgent.install();
    JREClassTransformer.perform(ByteBuddyAgent.getInstrumentation());

    // After redefinition the logic of String.matches(String regex) is reversed and
    // timing messages are being printed to the system console
    assertTrue("foo".matches("bar"));
    assertTrue(lastPrintlnMessage.contains("String.matches(java.lang.String) took "));
    assertFalse("foo".matches("foo"));
    assertTrue(lastPrintlnMessage.contains("String.matches(java.lang.String) took "));
  }

}
