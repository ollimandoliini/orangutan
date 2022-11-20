import cats.effect.ExitCode
import cats.effect.IO
import cats.effect.IOApp
import cats.effect.Ref
import cats.effect.std.Random
import cats.implicits.catsSyntaxEither
import cats.syntax.all.*
import com.comcast.ip4s.*
import fs2.Pipe
import fs2.Stream
import fs2.concurrent.Channel
import fs2.concurrent.Topic
import io.circe.*
import io.circe.generic.semiauto.*
import io.circe.parser.*
import io.circe.syntax.*
import org.http4s.HttpApp
import org.http4s.HttpRoutes
import org.http4s.StaticFile
import org.http4s.*
import org.http4s.dsl.*
import org.http4s.dsl.io.*
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.implicits._
import org.http4s.server.Router
import org.http4s.server.staticcontent._
import org.http4s.server.websocket.WebSocketBuilder2
import org.http4s.websocket.WebSocketFrame
import org.http4s.websocket.WebSocketFrame.{Ping, Text}


import scala.collection.immutable.ArraySeq
import scala.concurrent.duration.DurationInt
import scala.util.Try

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

case class WebSocketOutput(
    board: Map[PlayerId, Point],
    scores: Map[PlayerId, Int]
)
given Encoder[WebSocketOutput] = deriveEncoder

object Server extends IOApp {
  def run(args: List[String]): IO[ExitCode] = {
    for {
      randomGenerator <- Random.scalaUtilRandom[IO]
      ref <- IO.ref(
        GameState(
          scores = Map.empty[PlayerId, Int],
          board = BoardState(
            fw = Map.empty[PlayerId, Point],
            bw = Map.empty[Point, PlayerId]
          )
        )
      )
      game = Game(ref, 10, 10, randomGenerator)

      emberServerBuilder <- EmberServerBuilder
        .default[IO]
        .withHost(host"0.0.0.0")
        .withPort(port"8080")
        .withHttpWebSocketApp(service(game))
        .build
        .use(server =>
          IO.delay(println(s"Server has Started at ${server.address}")) >>
            IO.never.as(ExitCode.Success)
        )

    } yield emberServerBuilder

  }

  def createSendStream(game: Game): Stream[IO, WebSocketFrame] = {
    val keepAlive: Stream[IO, WebSocketFrame.Ping] =
      Stream(Ping()).repeat.metered(10.seconds)

    return Stream
      .repeatEval(
        game
          .get()
          .map(gameState =>
            WebSocketOutput(gameState.board.fw, gameState.scores)
          )
      )
      .metered(50.millis)
      .map(x => Text(x.asJson.toString))
      .merge(keepAlive)
  }

  def receive(
      game: Game,
      playerId: PlayerId
  )(stream: Stream[IO, WebSocketFrame]): Stream[IO, Unit] =
    stream
      .evalMap({
        case Text(txt, _) =>
          decode[Payload](txt) match {
            case Right(payload) =>
              game.performAction(playerId, payload.action)
            case Left(_) => IO.unit
          }
        case _ => IO.unit
      })
      .onFinalize(game.deletePlayer(playerId))

  def webSocketService(
      game: Game
  )(wsb: WebSocketBuilder2[IO]): HttpRoutes[IO] = {
    val dsl = new Http4sDsl[IO] {}
    import dsl.*

    HttpRoutes
      .of[IO] { case GET -> Root / username =>
        val playerId = PlayerId(username)

        game.setInitialPosition(playerId)
        >>
        wsb.build(createSendStream(game), receive(game, playerId))
      }
  }

  def service(game: Game)(wsb: WebSocketBuilder2[IO]) = Router(
    "/" -> fileService[IO](FileService.Config("./frontend/build")),
    "/ws" -> webSocketService(game)(wsb)
  ).orNotFound

}
