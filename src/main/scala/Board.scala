import cats.syntax.all.*
import cats.effect.IO
import cats.effect.std.Random
import io.circe.generic.semiauto.*
import io.circe.parser.*
import io.circe.{Encoder, KeyEncoder}
import cats.effect.*

def getNewPosition(point: Point, action: Action): Point =
  point match {
    case Point(x, y) =>
      action match {
        case Action.Left  => Point(x - 1, y)
        case Action.Right => Point(x + 1, y)
        case Action.Up    => Point(x, y - 1)
        case Action.Down  => Point(x, y + 1)
      }
  }

case class PlayerId(value: String)
given Encoder[PlayerId] = deriveEncoder[PlayerId]
given KeyEncoder[PlayerId] = new KeyEncoder {
  def apply(key: PlayerId): String = key.value
}

case class Point(x: Int, y: Int)
given Encoder[Point] = deriveEncoder[Point]

case class BoardState(fw: Map[PlayerId, Point], bw: Map[Point, PlayerId])
case class GameState(board: BoardState, scores: Map[PlayerId, Int])

case class Game(
    ref: Ref[IO, GameState],
    xDim: Int,
    yDim: Int,
    gen: Random[IO]
) {
  def setInitialPosition(id: PlayerId): IO[Unit] = {
    ref.access.flatMap { case (gameState, set) =>
      IO.raiseError(new Exception("Board full"))
        .whenA(gameState.board.fw.size == xDim * yDim) >>
        (gen.nextIntBounded(xDim), gen.nextIntBounded(yDim))
          .mapN(Point.apply)
          .iterateWhile(point => gameState.board.bw.contains(point))
          .flatMap(p => {
            val board = BoardState(
              gameState.board.fw + (id -> p),
              gameState.board.bw + (p -> id)
            )
            val scores = gameState.scores + (id -> 0)
            set(GameState(board, scores))
          })
          .flatMap {
            case true  => IO.unit
            case false => setInitialPosition(id)
          }
    }
  }
  def performAction(playerId: PlayerId, action: Action): IO[Unit] =
    ref.update(gameState =>
      gameState.copy(board = movePlayer(playerId, action, gameState.board))
    )

  def movePlayer(
      playerId: PlayerId,
      action: Action,
      boardState: BoardState
  ): BoardState = {
    boardState.fw.get(playerId) match {
      case Some(position) => {
        val newPosition = getNewPosition(position, action)
        BoardState(
          boardState.fw.updated(playerId, newPosition),
          boardState.bw.updated(newPosition, playerId)
        )

      }
      case None => boardState
    }
  }
  def get() = ref.get

  def deletePlayer(playerId: PlayerId) =
    ref.update(gameState => {
      gameState.board.fw.get(playerId) match {
        case Some(playerPosition) => {
          GameState(
            BoardState(
              gameState.board.fw - playerId,
              gameState.board.bw - playerPosition
            ),
            gameState.scores - playerId
          )
        }
        case None => gameState
      }
    })

  def getNewPosition(point: Point, action: Action): Point =
    point match {
      case Point(x, y) =>
        action match {
          case Action.Left  => if (x > 0) then Point(x - 1, y) else point
          case Action.Down  => if (y > 0) then Point(x, y - 1) else point
          case Action.Right => if (x < xDim - 1 ) then Point(x + 1, y) else point
          case Action.Up    => if (y < yDim - 1) then Point(x, y + 1) else point
        }
    }
}
