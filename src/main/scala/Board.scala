
import cats.syntax.all.*
import cats.effect.IO
import cats.effect.std.Random
import io.circe.generic.semiauto.*
import io.circe.parser.*
import io.circe.{Encoder, KeyEncoder}


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

case class GameState(
    board: Map[Point, PlayerId]
)

case class BoardState(fw: Map[PlayerId, Point], bw: Map[Point, PlayerId])

trait Board {
  def setInitialPosition(p: PlayerId): IO[Unit]
  def performAction(playerId: PlayerId, action: Action): IO[Unit]
  def get(): IO[BoardState]
  def update(f: BoardState => BoardState): IO[Unit]
  def deletePlayer(playerId: PlayerId): IO[Unit]
}

object model {
  
}

object Board {
  def create(xDim: Int, yDim: Int, gen: Random[IO]): IO[Board] =
    IO.ref(BoardState(Map.empty[PlayerId, Point], Map.empty[Point, PlayerId]))
      .map { board =>
        new Board {
          def setInitialPosition(id: PlayerId): IO[Unit] =
            board.access.flatMap { case (boardState, set) =>
              IO.raiseError(new Exception("Board full"))
                .whenA(boardState.fw.size == xDim * yDim) >>
                (gen.nextIntBounded(xDim), gen.nextIntBounded(yDim))
                  .mapN(Point.apply)
                  .iterateWhile(point => boardState.bw.contains(point))
                  .flatMap(p =>
                    set(
                      BoardState(
                        boardState.fw + (id -> p),
                        boardState.bw + (p -> id)
                      )
                    )
                  )
                  .flatMap {
                    case true  => IO.unit
                    case false => setInitialPosition(id)
                  }
            }
          def performAction(playerId: PlayerId, action: Action): IO[Unit] =
            board.update(boardState => movePlayer(playerId, action, boardState))

          def get(): IO[BoardState] = board.get

          def update(f: BoardState => BoardState) = board.update(f)
          def deletePlayer(playerId: PlayerId) =
            board.update(boardState => {
              boardState.fw.get(playerId) match {
                case Some(playerPosition) => {
                  BoardState(
                    boardState.fw - playerId,
                    boardState.bw - playerPosition
                  )
                }
                case None => boardState
              }
            })
        }
      }

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
}