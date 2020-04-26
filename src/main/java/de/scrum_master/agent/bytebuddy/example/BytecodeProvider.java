package de.scrum_master.agent.bytebuddy.example;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;

/**
 * This transformer has the sole purpose of making the bytecode of each processed class
 * available via {@link #getClassFile()} right after the corresponding class was processed.
 * This can be done as follows:
 * <pre>{@code
 * BytecodeProvider bytecodeProvider = new BytecodeProvider();
 * instrumentation.addTransformer(bytecodeProvider, true);
 * try {
 *   instrumentation.retransformClasses(clazz);
 *   return bytecodeProvider.getClassFile();
 * } finally {
 *   instrumentation.removeTransformer(bytecodeProvider);
 * }
 * }</pre>
 */
public class BytecodeProvider implements ClassFileTransformer {
  private byte[] classFile;

  /**
   * This method does not transform anything but always returns the original bytecode.
   * The notable side effect is that it saves the bytecode in a buffer which can be retrieved
   * right after "transformation" via {@link #getClassFile()}.
   * <p></p>
   * <b>Caveat:</b> The buffer is overwritten as soon as the next class has been "transformed".
   * So you want to process and fetch the bytecode class by class or maybe even create a new
   * transformer instance for each class.
   */
  @Override
  public byte[] transform(
    ClassLoader loader,
    String className,
    Class<?> classBeingRedefined,
    ProtectionDomain protectionDomain,
    byte[] classfileBuffer
  ) {
    classFile = classfileBuffer;
    return null;
  }

  /**
   * Retrieve the bytecode of the previous class which has been processed by this transformer.
   */
  public byte[] getClassFile() {
    return classFile;
  }
}
