package org.concurrentspec

import java.util.concurrent.TimeoutException
import org.scalatest.FunSuite
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.Suite
import org.scalatest.BeforeAndAfterEach
import org.scalatest.WordSpec

/** Tests {@link AssertConcurrently}.
  */
@RunWith(classOf[JUnitRunner])
class AssertConcurrentlyTest extends FunSuite with AssertConcurrently {
  /** Should throw an exception.
    */
  test("waitShouldSupportResume") {
    new Thread(new Runnable() {
      def run() {
        resume();
      }
    }).start();
    threadWait();
  }

  /** Should throw an exception.
    */
  test("waitShouldSupportExceptions") {
    intercept[IllegalArgumentException] {
      new Thread(new Runnable() {
        def run() {
          try {
            throw new IllegalArgumentException();
          } catch {
            case e => threadFail(e);
          }
        }
      }).start();
      threadWait();
    }
  }

  /** Should throw an assertion error.
    */
  test("waitShouldSupportAssertionErrors") {
    intercept[AssertionError] {
      new Thread(new Runnable() {
        def run() {
          threadAssertTrue(false);
        }
      }).start();
      threadWait(0);
    }
  }

  /** Should timeout.
    */
  test("waitShouldSupportTimeouts") {
    intercept[TimeoutException] {
      new Thread(new Runnable() {
        def run() {
          threadAssertTrue(true);
        }
      }).start();
      threadWait(500);
    }
  }

  /** Should timeout.
    */
  test("sleepShouldSupportTimeouts") {
    intercept[TimeoutException] {
      new Thread(new Runnable() {
        def run() {
        }
      }).start();
      sleep(500);
    }
  }

  /** Should support wake.
    */
  test("sleepShouldSupportResume") {
    new Thread(new Runnable() {
      def run() {
        resume();
      }
    }).start();
    sleep(500);
  }

  /** Should support assertion errors.
    */
  test("sleepShouldSupportAssertionErrors") {
    intercept[AssertionError] {
      new Thread(new Runnable() {
        def run() {
          threadAssertTrue(false);
        }
      }).start();
      sleep(500);
    }
  }

  /** Ensures that waiting for multiple resumes works as expected.
    */
  test("shouldSupportMultipleResumes") {
    new Thread(new Runnable() {
      def run() {
        for (i <- 0 until 5)
          resume();
      }
    }).start();
    threadWait(500, 5);
  }

  test("shouldSupportThreadWait0WithResumeCount") {
    new Thread(new Runnable() {
      def run() {
        for (i <- 0 until 5)
          resume();
      }
    }).start();
    threadWait(0, 5);
  }
}
