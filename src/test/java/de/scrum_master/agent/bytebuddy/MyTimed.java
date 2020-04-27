package de.scrum_master.agent.bytebuddy;

public class MyTimed {
  public String doSomething(int i) {
    return ((Integer) i).toString();
  }

  public int doSomethingElse() {
    return 42;
  }
}
