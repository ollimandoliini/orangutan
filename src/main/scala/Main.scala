import cats.effect.{ExitCode, IO, IOApp, Ref}
import cats.implicits.catsSyntaxEither
import cats.syntax.all.*
import com.comcast.ip4s.*
import fs2.concurrent.{Channel, Topic}
import fs2.{Pipe, Stream}
import io.circe.generic.semiauto.*
import io.circe.parser.*
import io.circe.syntax.*
import io.circe.*
import org.http4s
import org.http4s.dsl.Http4sDsl
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.websocket.WebSocketBuilder2
import org.http4s.websocket.WebSocketFrame
import org.http4s.websocket.WebSocketFrame.{Close, Ping, Text}
import org.http4s.{HttpApp, HttpRoutes}
import org.http4s.server.staticcontent._

import scala.collection.immutable.ArraySeq
import scala.concurrent.duration.DurationInt
import scala.util.Try
import cats.effect.std.Random
import org.http4s.StaticFile
import org.http4s.server.staticcontent.FileService
import org.http4s.server.Router

enum Action:
  case Right
  case Down
  case Left
  case Up

given Decoder[Action] = (c: HCursor) =>
  Decoder.decodeString(c).flatMap { str =>
    Try(Action.valueOf(str.capitalize)).toEither.leftMap(_ =>
      DecodingFailure(
        s"no enum value matched for $str",
        List(CursorOp.Field(str))
      )
    )
  }

case class Payload(
    action: Action
)

given Decoder[Payload] = deriveDecoder

object Server extends IOApp {
  def run(args: List[String]): IO[ExitCode] = {
    for {
      randomGenerator <- Random.scalaUtilRandom[IO]
      board <- Board.create(10, 10, randomGenerator)

      emberServerBuilder <- EmberServerBuilder
        .default[IO]
        .withHost(host"0.0.0.0")
        .withPort(port"8080")
        .withHttpWebSocketApp(service_(board))
        .build
        .use(server =>
          IO.delay(println(s"Server has Started at ${server.address}")) >>
            IO.never.as(ExitCode.Success)
        )

    } yield emberServerBuilder

  }

  def receive(
      board: Board,
      playerId: PlayerId
  )(stream: Stream[IO, WebSocketFrame]): Stream[IO, Unit] =
    stream
      .evalMap({
        case Text(txt, _) =>
          decode[Payload](txt) match {
            case Right(payload) =>
              board.performAction(playerId, payload.action)
            case Left(err) => IO.println(err)
          }
        case _ => IO.unit
      })
      .onFinalize(board.deletePlayer(playerId))

  def webSocketService(
      board: Board
  )(wsb: WebSocketBuilder2[IO]): HttpRoutes[IO] = {
    val dsl = new Http4sDsl[IO] {}
    import dsl.*

    HttpRoutes
      .of[IO] { case GET -> Root / "ws" / username =>
        val playerId = PlayerId(username)
        val keepAlive: Stream[IO, WebSocketFrame.Ping] =
          Stream(Ping()).repeat.metered(10.seconds)

        val send: Stream[IO, WebSocketFrame.Text] = Stream
          .repeatEval(board.get().map(_.fw))
          .metered(10.millis)
          .map(x => Text(x.asJson.toString))

        board.setInitialPosition(playerId)
        >>
        wsb.build(send.merge(keepAlive), receive(board, playerId))
      }
  }

  def service_(board: Board)(wsb: WebSocketBuilder2[IO]) = Router(
    "/ws" -> webSocketService(board)(wsb)
  ).orNotFound

  def service(
      board: Board
  )(wsb: WebSocketBuilder2[IO]): HttpApp[IO] = {
    val dsl = new Http4sDsl[IO] {}
    import dsl.*

    HttpRoutes
      .of[IO] { case GET -> Root / "ws" / username =>
        val playerId = PlayerId(username)
        val keepAlive: Stream[IO, WebSocketFrame.Ping] =
          Stream(Ping()).repeat.metered(10.seconds)

        val send: Stream[IO, WebSocketFrame.Text] = Stream
          .repeatEval(board.get().map(_.fw))
          .metered(10.millis)
          .map(x => Text(x.asJson.toString))

        board.setInitialPosition(playerId)
        >>
        wsb.build(send.merge(keepAlive), receive(board, playerId))
      // case GET -> Root =>
      //   fileService[IO](FileService.Config("./frontend"))
      }
      .orNotFound
  }
}
