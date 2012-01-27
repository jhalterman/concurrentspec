package org.concurrentspec

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.TimeoutException

import scala.annotation.serializable

import org.junit.Assert.{ assertTrue, assertNull, assertNotNull, assertFalse, assertEquals }
import org.scalatest.Suite
import org.scalatest.BeforeAndAfterEach

/** Concurrent Scaltest spec.
  *
  * <p>
  * Call {@link #sleep(long)}, {@link #sleep(long, int)}, {@link #threadWait(long)} or
  * {@link #threadWait(long, int)} from the main unit test thread to wait for some other thread to
  * perform assertions. These operations will block until {@link #resume()} is called, the operation
  * times out, or a threadAssert call fails.
  *
  * <p>
  * The threadAssert methods can be used from any thread to perform concurrent assertions. Assertion
  * failures will result in the main thread being interrupted and the failure thrown.
  *
  * <p>
  * Usage:
  *
  * <pre>
  * class MyTest extends FunSuite with AssertConcurrently {
  * test("assertAndResume") {
  * new Thread(new Runnable() {
  * public void run() {
  * threadAssertTrue(true);
  * resume();
  * }
  * }).start();
  *
  * sleep(500);
  * }
  * }
  * </pre>
  *
  * @author Jonathan Halterman
  */
trait AssertConcurrently extends BeforeAndAfterEach { this: Suite =>
  private val mainThread = Thread.currentThread()
  private val TIMEOUT_MESSAGE = "Test timed out while waiting for an expected result"
  protected var waitCount: AtomicInteger = _
  protected var failure: Throwable = _

  override protected def beforeEach() {
    waitCount = null
    failure = null
  }

  /** @see org.junit.Assert#assertEquals(Object, Object)
    */
  def threadAssertEquals(x: Any, y: Any) {
    try {
      assertEquals(x, y);
    } catch {
      case e: AssertionError => threadFail(e)
    }
  }

  /** @see org.junit.Assert#assertFalse(boolean)
    */
  def threadAssertFalse(b: Boolean) {
    try {
      assertFalse(b);
    } catch {
      case e: AssertionError => threadFail(e)
    }
  }

  /** @see org.junit.Assert#assertNotNull(Object)
    */
  def threadAssertNotNull(o: Any) {
    try {
      assertNotNull(o);
    } catch {
      case e: AssertionError => threadFail(e)
    }
  }

  /** @see org.junit.Assert#assertNull(Object)
    */
  def threadAssertNull(x: Any) {
    try {
      assertNull(x)
    } catch {
      case e: AssertionError => threadFail(e)
    }
  }

  /** @see org.junit.Assert#assertTrue(boolean)
    */
  def threadAssertTrue(b: Boolean) {
    try {
      assertTrue(b)
    } catch {
      case e: AssertionError => threadFail(e)
    }
  }

  /** Fails the current test for the given reason.
    */
  def threadFail(reason: String) {
    threadFail(new AssertionError(reason))
  }

  /** Fails the current test with the given Throwable.
    */
  def threadFail(e: Throwable) {
    failure = e
    resume(mainThread)
  }

  /** Resumes the main test thread.
    */
  protected def resume() {
    resume(mainThread)
  }

  /** Resumes a waiting test case if {@code thread} is not the mainThread, the waitCount is null or
    * the decremented waitCount is 0.
    *
    * <p>
    * Note: This method is likely not very useful to call directly since a concurrent run of a test
    * case resulting in the need to resume from a separate thread would yield no correlation between
    * the initiating thread and the thread where the resume call takes place.
    *
    * @param thread Thread to resume
    */
  protected def resume(thread: Thread) {
    if (thread != mainThread || waitCount == null || waitCount.decrementAndGet() == 0)
      thread.interrupt()
  }

  /** Sleeps until the {@code sleepDuration} has elapsed, {@link #resume()} is called, or the test is
    * failed.
    *
    * @param sleepDuration
    * @throws TimeoutException if the sleep operation times out while waiting for a result
    * @throws Throwable the last reported test failure
    */
  protected def sleep(sleepDuration: Long) {
    try {
      Thread.sleep(sleepDuration);
      throw new TimeoutException(TIMEOUT_MESSAGE)
    } catch {
      case ignored: InterruptedException =>
    } finally {
      if (failure != null)
        throw failure
    }
  }

  /** Sleeps until the {@code sleepDuration} has elapsed, {@link #resume()} is called
    * {@code resumeThreshold} times, or the test is failed.
    *
    * @param sleepDuration Duration to sleep
    * @param resumeThreshold Number of times resume must be called before sleep is interrupted
    * @throws IllegalStateException if called from outside the main test thread
    * @throws TimeoutException if the sleep operation times out while waiting for a result
    * @throws Throwable the last reported test failure
    */
  protected def sleep(sleepDuration: Long, resumeThreshold: Int) {
    if (Thread.currentThread() != mainThread)
      throw new IllegalStateException("Must be called from within the main test thread")

    waitCount = new AtomicInteger(resumeThreshold)
    sleep(sleepDuration)
    waitCount = null
  }

  /** Waits until {@link #resume()} is called, or the test is failed.
    *
    * @throws IllegalStateException if called from outside the main test thread
    * @throws Throwable the last reported test failure
    */
  protected def threadWait() {
    if (Thread.currentThread() != mainThread)
      throw new IllegalStateException("Must be called from within the main test thread");

    this.synchronized {
      while (true) {
        try {
          wait()
          throw new TimeoutException(TIMEOUT_MESSAGE)
        } catch {
          case e: InterruptedException => {
            if (failure != null)
              throw failure
            return
          }
        }
      }
    }
  }

  /** Waits until the {@code waitDuration} has elapsed, {@link #resume()} is called, or the test is
    * failed. Delegates to {@link #sleep(long)} to avoid spurious wakeups.
    *
    * @see #sleep(long)
    */
  protected def threadWait(waitDuration: Long) {
    if (waitDuration == 0)
      threadWait()
    else
      sleep(waitDuration)
  }

  /** Waits until the {@code waitDuration} has elapsed, {@link #resume()} is called
    * {@code resumeThreshold} times, or the test is failed. Delegates to {@link #sleep(long, int)} to
    * avoid spurious wakeups.
    *
    * @see #sleep(long, int)
    */
  protected def threadWait(waitDuration: Long, resumeThreshold: Int) {
    if (waitDuration == 0) {
      waitCount = new AtomicInteger(resumeThreshold)
      threadWait()
      waitCount = null
    } else
      sleep(waitDuration, resumeThreshold)
  }
}