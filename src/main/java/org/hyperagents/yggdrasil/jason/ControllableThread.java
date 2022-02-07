package org.hyperagents.yggdrasil.jason;

public class ControllableThread {

  Thread t;

  public ControllableThread(Thread t, int n){
    this.t = t;
    Thread controlThread = new Thread(new Runnable() {
      @Override
      public void run() {
        while(true){
          if (t.isAlive()){
            long time1 = System.currentTimeMillis();
            long endtime = time1 + n;
            long currentTime = System.currentTimeMillis();
            while (currentTime < endtime){
              currentTime = System.currentTimeMillis();
              System.out.println("current time: "+currentTime);
            }
            System.out.println("time to stop");
            t.interrupt();
            System.out.println("thread interrupted");

          }
          if (t.isInterrupted()){
            System.out.println("the thread is interrupted");
          }
        }
      }
    });
    controlThread.start();
  }

  public void start(){
    t.start();
  }
}
