# ConcurrentSpec 0.1.0

A simple ScalaTest extension for performing concurrent assertions.

## Update

Bill Venners has [adopted](http://groups.google.com/group/scalatest-users/browse_thread/thread/773c62fd2f860ccb) the ConcurrentSpec notion into ScalaTest 1.8 (release forthcoming). See the [Waiter](http://www.artima.com/docs-scalatest-1.8-1.Feb.2012/#org.scalatest.concurrent.Waiter) class.

## Introduction

ConcurrentSpec allows you to write test cases capable of performing concurrent assertions or waiting for expected operations across multiple threads, with failures being properly reported back to the main test thread.

## Usage

* Extend or mix the `AssertConcurrently` trait into your test class after FunSuite or WordSpec (etc).
* Use `threadWait` or `sleep` calls to block the main test thread while waiting for worker threads to perform assertions. 
* Use `threadAssert` calls from any thread to perform concurrent assertions. Assertion failures will result in the main thread being interrupted and the failure thrown.
* Once expected assertions are completed, use a `resume` call to unblock the main thread.

If a blocking operation times out before all expected `resume` calls occur, the test is failed with a TimeoutException.

## Examples

Block the main thread while waiting for an assertion in a worker thread and resume after completion:

```scala
test("shouldSucceed") {
  new Thread(new Runnable() {
    def run() {
      threadAssertTrue(true)
      resume()
    }
  }).start()
  
  threadWait(100)
}
```

Handle a failed assertion:

```scala
test("shouldFail") {
  intercept[AssertionError] { 
    new Thread(new Runnable() {
      def run() {
        threadAssertTrue(false)
      }
    }).start()
    
    threadWait(0)
  }
}
```

TimeoutException occurs if resume is not called before the wait duration is exceeded:

```scala
test("sleepShouldSupportTimeouts") {
  intercept[TimeoutException] {
    new Thread(new Runnable() {
      def run() { }
    }).start()
    
    sleep(500)
  }
}
```

Block the main thread while waiting for n number of resume calls:

```scala
test("shouldSupportMultipleResumes") {
  new Thread(new Runnable() {
    def run() {
      for (i <- 0 until 5)
        resume()
    }
  }).start()
  
  threadWait(500, 5)
}
```

## References

Thanks to the JSR-166 TCK authors for the initial inspiration.

## License

Copyright 2012 Jonathan Halterman - Released under the [Apache 2.0 license](http://www.apache.org/licenses/LICENSE-2.0.html).
