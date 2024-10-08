package orcus.bigtable

import cats.Monad
import cats.MonadError
import com.google.api.core.ApiFuture
import com.google.api.gax.rpc.ResponseObserver
import com.google.api.gax.rpc.StreamController
import com.google.cloud.bigtable.data.v2.BigtableDataClient
import com.google.cloud.bigtable.data.v2.models.{Row as GRow, *}
import orcus.async.AsyncHandler
import orcus.async.Par
import orcus.bigtable.codec.RowDecoder
import orcus.internal.Utils

import scala.annotation.tailrec
import scala.collection.mutable

trait DataClient[F[_]] {

  def readRowAsync(query: Query): F[Option[Row]]

  def readRowsAsync(query: Query): F[Vector[Row]]

  def sampleRowKeysAsync(tableId: String): F[List[KeyOffset]]

  def mutateRowAsync(mutation: RowMutation): F[Unit]

  def bulkMutateRowsAsync(mutation: BulkMutation): F[Unit]

  def checkAndMutateRowAsync(mutation: ConditionalRowMutation): F[Boolean]

  def readModifyWriteRowAsync(mutation: ReadModifyWriteRow): F[Option[Row]]

  def close(): Unit
}

object DataClient {
  def apply[F[_]](client: BigtableDataClient)(implicit
    F: MonadError[F, Throwable],
    asyncH: AsyncHandler[F],
    parF: Par.Aux[ApiFuture, F]
  ): DataClient[F] = new DefaultDataClient[F](client)
}

final class DefaultDataClient[F[_]](client: BigtableDataClient)(implicit
  F: MonadError[F, Throwable],
  asyncH: AsyncHandler[F],
  parF: Par.Aux[ApiFuture, F]
) extends DataClient[F] {
  private val adapter = DataClientAdapter

  def readRowAsync(query: Query): F[Option[Row]] =
    adapter.readRowAsync(client, query)

  def readRowsAsync(query: Query): F[Vector[Row]] =
    adapter.readRowsAsync(client, query)

  def sampleRowKeysAsync(tableId: String): F[List[KeyOffset]] =
    adapter.sampleRowKeysAsync(client, TableId.of(tableId))

  def sampleRowKeysAsync(targetId: TargetId): F[List[KeyOffset]] =
    adapter.sampleRowKeysAsync(client, targetId)

  def mutateRowAsync(mutation: RowMutation): F[Unit] =
    adapter.mutateRowAsync(client, mutation)

  def bulkMutateRowsAsync(mutation: BulkMutation): F[Unit] =
    adapter.bulkMutateRowsAsync(client, mutation)

  def checkAndMutateRowAsync(mutation: ConditionalRowMutation): F[Boolean] =
    adapter.checkAndMutateRowAsync(client, mutation)

  def readModifyWriteRowAsync(mutation: ReadModifyWriteRow): F[Option[Row]] =
    adapter.readModifyWriteRowAsync(client, mutation)

  def close(): Unit =
    adapter.close(client)
}

object DataClientAdapter {
  import cats.syntax.all._

  def readRowAsync[F[_], A: RowDecoder](client: BigtableDataClient, query: Query)(implicit
    F: MonadError[F, Throwable],
    parF: Par.Aux[ApiFuture, F]
  ): F[Option[A]] =
    F.flatMap(parF.parallel(client.readRowCallable.futureCall(query))) {
      case null => F.pure(none)
      case row  => F.fromEither(RowDecoder[A].apply(decode(row))).map(Option.apply)
    }

  def readRowsAsync[F[_]](client: BigtableDataClient, query: Query)(implicit F: AsyncHandler[F]): F[Vector[Row]] =
    F.handle[Vector[Row]](
      cb =>
        client.readRowsAsync(
          query,
          new ResponseObserver[GRow] {
            private val acc = Vector.newBuilder[Row]

            def onStart(controller: StreamController): Unit = {}
            def onResponse(response: GRow): Unit            = acc += decode(response)
            def onError(e: Throwable): Unit                 = cb(e.asLeft)
            def onComplete(): Unit                          = cb(acc.result().asRight)
          }
        ),
      ()
    )

  @Deprecated
  def sampleRowKeysAsync[F[_]](client: BigtableDataClient, tableId: String)(implicit
    F: Monad[F],
    parF: Par.Aux[ApiFuture, F]
  ): F[List[KeyOffset]] =
    this.sampleRowKeysAsync(client, TableId.of(tableId))

  def sampleRowKeysAsync[F[_]](client: BigtableDataClient, targetId: TargetId)(implicit
    F: Monad[F],
    parF: Par.Aux[ApiFuture, F]
  ): F[List[KeyOffset]] =
    parF.parallel(client.sampleRowKeysAsync(targetId)).map(Utils.toList)

  def mutateRowAsync[F[_]](client: BigtableDataClient, rowMutation: RowMutation)(implicit
    F: Monad[F],
    parF: Par.Aux[ApiFuture, F]
  ): F[Unit] =
    parF.parallel(client.mutateRowAsync(rowMutation)) >> F.unit

  def bulkMutateRowsAsync[F[_]](client: BigtableDataClient, mutation: BulkMutation)(implicit
    F: Monad[F],
    parF: Par.Aux[ApiFuture, F]
  ): F[Unit] =
    parF.parallel(client.bulkMutateRowsAsync(mutation)) >> F.unit

  def checkAndMutateRowAsync[F[_]](client: BigtableDataClient, mutation: ConditionalRowMutation)(implicit
    F: Monad[F],
    parF: Par.Aux[ApiFuture, F]
  ): F[Boolean] =
    parF.parallel(client.checkAndMutateRowAsync(mutation)).map(Boolean.unbox)

  def readModifyWriteRowAsync[F[_]](client: BigtableDataClient, mutation: ReadModifyWriteRow)(implicit
    F: MonadError[F, Throwable],
    parF: Par.Aux[ApiFuture, F]
  ): F[Option[Row]] =
    F.flatMap(parF.parallel(client.readModifyWriteRowAsync(mutation)).map(Option.apply)) {
      case Some(row) => F.pure(Option(decode(row)))
      case _         => F.pure(none)
    }

  def close(client: BigtableDataClient): Unit = client.close()

  private def decode(row: GRow): Row = {
    val acc   = Map.newBuilder[String, List[RowCell]]
    val cells = row.getCells
    val size  = cells.size()

    @tailrec def loop2(
      currentFamily: String,
      i: Int,
      b: mutable.Builder[RowCell, List[RowCell]]
    ): (Int, List[RowCell]) =
      if (i >= size) i -> b.result()
      else {
        val cell = cells.get(i)
        if (currentFamily != cell.getFamily) i -> b.result()
        else loop2(currentFamily, i + 1, b += cell)
      }

    @tailrec def loop(i: Int): Map[String, List[RowCell]] =
      if (i >= size) acc.result()
      else {
        val family   = cells.get(i).getFamily
        val (ii, xs) = loop2(family, i, List.newBuilder)
        acc += family -> xs
        loop(ii)
      }

    Row(row.getKey.toStringUtf8, loop(0))
  }
}
