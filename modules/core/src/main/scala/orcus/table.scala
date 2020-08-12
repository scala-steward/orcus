package orcus

import java.util.concurrent.CompletableFuture

import cats.Applicative
import cats.ApplicativeError
import cats.data.Kleisli
import orcus.async.Par
import orcus.internal.ScalaVersionSpecifics._
import orcus.internal.Utils
import org.apache.hadoop.conf.{Configuration => HConfig}
import org.apache.hadoop.hbase.client.AsyncTable
import org.apache.hadoop.hbase.client.RowMutations
import org.apache.hadoop.hbase.client.ScanResultConsumerBase
import org.apache.hadoop.hbase.client.{Append => HAppend}
import org.apache.hadoop.hbase.client.{Delete => HDelete}
import org.apache.hadoop.hbase.client.{Get => HGet}
import org.apache.hadoop.hbase.client.{Increment => HIncrement}
import org.apache.hadoop.hbase.client.{Put => HPut}
import org.apache.hadoop.hbase.client.{Result => HResult}
import org.apache.hadoop.hbase.client.{ResultScanner => HResultScanner}
import org.apache.hadoop.hbase.client.{Row => HRow}
import org.apache.hadoop.hbase.client.{Scan => HScan}
import org.apache.hadoop.hbase.{TableName => HTableName}

object table {
  type AsyncTableT = AsyncTable[T] forSome { type T <: ScanResultConsumerBase }

  def getName[F[_]](t: AsyncTableT)(implicit
    F: Applicative[F]
  ): F[HTableName] =
    F.pure(t.getName)

  def getConfiguration[F[_]](t: AsyncTableT)(implicit
    F: Applicative[F]
  ): F[HConfig] =
    F.pure(t.getConfiguration)

  def exists[F[_]](t: AsyncTableT, get: HGet)(implicit
    FE: ApplicativeError[F, Throwable],
    F: Par.Aux[CompletableFuture, F]
  ): F[Boolean] =
    FE.map(F.parallel(t.exists(get)))(_.booleanValue())

  def get[F[_]](t: AsyncTableT, a: HGet)(implicit
    F: Par.Aux[CompletableFuture, F]
  ): F[HResult] =
    F.parallel(t.get(a))

  def put[F[_]](t: AsyncTableT, a: HPut)(implicit
    FE: ApplicativeError[F, Throwable],
    F: Par.Aux[CompletableFuture, F]
  ): F[Unit] =
    FE.map(F.parallel(t.put(a)))(_ => ())

  def scanAll[F[_]](t: AsyncTableT, a: HScan)(implicit
    FE: ApplicativeError[F, Throwable],
    F: Par.Aux[CompletableFuture, F]
  ): F[Seq[HResult]] =
    FE.map(F.parallel(t.scanAll(a)))(Utils.toSeq)

  def getScanner[F[_]](t: AsyncTableT, a: HScan)(implicit
    FE: ApplicativeError[F, Throwable]
  ): F[HResultScanner] =
    FE.catchNonFatal(t.getScanner(a))

  def delete[F[_]](t: AsyncTableT, a: HDelete)(implicit
    FE: ApplicativeError[F, Throwable],
    F: Par.Aux[CompletableFuture, F]
  ): F[Unit] =
    FE.map(F.parallel(t.delete(a)))(_ => ())

  def append[F[_]](t: AsyncTableT, a: HAppend)(implicit
    F: Par.Aux[CompletableFuture, F]
  ): F[HResult] =
    F.parallel(t.append(a))

  def increment[F[_]](t: AsyncTableT, a: HIncrement)(implicit
    F: Par.Aux[CompletableFuture, F]
  ): F[HResult] =
    F.parallel(t.increment(a))

  def batch[F[_], C[_]](t: AsyncTableT, as: Seq[_ <: HRow])(implicit
    apErrorF: ApplicativeError[F, Throwable],
    parF: Par.Aux[CompletableFuture, F],
    factoryC: Factory[BatchResult, C[BatchResult]]
  ): F[C[BatchResult]] = {
    val itr   = as.iterator
    val itcfo = Utils.toIterator(t.batch[Object](Utils.toJavaList(as)))
    val itfb = itr
      .zip(itcfo.map(parF.parallel.apply))
      .map {
        case (a, fo) =>
          apErrorF.recoverWith(apErrorF.map[Object, BatchResult](fo) {
            case r: HResult =>
              BatchResult.Mutate(Some(r))
            case null =>
              a match {
                case _: HGet | _: HAppend | _: HIncrement | _: RowMutations =>
                  BatchResult.Mutate(None)
                case _ => // Delete or Put
                  BatchResult.VoidMutate
              }
            case other =>
              BatchResult.Error(new Exception(s"Unexpected class is returned: ${other.getClass.getSimpleName}"), a)
          }) {
            case t: Throwable =>
              apErrorF.pure(BatchResult.Error(t, a))
          }
      }
    val fbb = itfb.foldLeft(apErrorF.pure(factoryC.newBuilder)) {
      case (acc, fb) => apErrorF.map2(fb, acc)((a, b) => b += a)
    }
    apErrorF.map(fbb)(_.result())
  }

  def batchAll[F[_], C[_]](t: AsyncTableT, as: Seq[_ <: HRow])(implicit
    FE: ApplicativeError[F, Throwable],
    F: Par.Aux[CompletableFuture, F],
    factory: Factory[Option[HResult], C[Option[HResult]]]
  ): F[C[Option[HResult]]] =
    FE.map(F.parallel(t.batchAll[Object](Utils.toJavaList(as)))) { xs =>
      val it = xs.iterator
      val c  = factory.newBuilder
      while (it.hasNext) c += (it.next match { case r: HResult => Option(r); case null => None })
      c.result()
    }

  def kleisli[F[_], A](f: AsyncTableT => F[A]): Kleisli[F, AsyncTableT, A] =
    Kleisli[F, AsyncTableT, A](f)
}
