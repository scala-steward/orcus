package orcus.async.instances.catsEffect

import java.util.concurrent.CompletableFuture

import cats.effect.{ContextShift, IO, Timer}
import orcus.async._
import orcus.async.implicits._
import concurrent._
import org.scalatest._

import scala.concurrent._

class CatsEffectConcurrentHandlerSpec extends FlatSpec with AsyncSpec {
  import ExecutionContext.Implicits.global

  implicit def contextShift: ContextShift[IO] =
    IO.contextShift(global)

  implicit def timer: Timer[IO] = IO.timer(global)

  it should "convert to a IO" in {
    def run = Par[CompletableFuture, IO].parallel(CompletableFuture.completedFuture(10))
    assert(10 === run.unsafeRunSync())
  }
  it should "convert to a failed IO" in {
    def run = Par[CompletableFuture, IO].parallel(failedFuture[Int](new Exception))
    assertThrows[Exception](run.unsafeRunSync())
  }
  it should "convert to a cancelable IO" in {
    val f = blockedFuture[Int]
    Par[CompletableFuture, IO].parallel(f).start.flatMap(_.cancel).unsafeRunSync()
    assert(f.isCancelled)
  }
}
