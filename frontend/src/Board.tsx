import { useEffect, useRef, useState } from "react";
import PlayerNamePrompt from "./PlayerNamePrompt";
import "./Board.css";

type BoardState = Record<string, PlayerPosition>;
type PlayerPosition = {
  x: number;
  y: number;
};

type BoardProps = {
  sendJsonMessage: (payload: { action: string }) => void;
  boardState: BoardState;
  playerName: string | null;
  setPlayerName: (playerName: string) => void;
};

const Board = ({
  sendJsonMessage,
  boardState,
  playerName,
  setPlayerName,
}: BoardProps) => {
  const boardRef = useRef<HTMLDivElement>(null);

  const keyHandler = (e: KeyboardEvent) => {
    if (e.repeat) {
      return;
    }
    switch (e.key) {
      case "ArrowRight":
        sendJsonMessage({ action: "right" });
        break;
      case "ArrowDown":
        sendJsonMessage({ action: "down" });
        break;
      case "ArrowLeft":
        sendJsonMessage({ action: "left" });
        break;
      case "ArrowUp":
        sendJsonMessage({ action: "up" });
        break;
      default:
        break;
    }
  };
  useEffect(() => {
    window.addEventListener("keydown", keyHandler);
    return () => {
      window.removeEventListener("keydown", keyHandler);
    };
  });

  return (
    <div className="board" ref={boardRef}>
      {playerName ? (
        <Canvas boardState={boardState} parentRef={boardRef} />
      ) : (
        <PlayerNamePrompt setPlayerName={setPlayerName} />
      )}
    </div>
  );
};

type CanvasProps = {
  boardState: BoardState;
  parentRef: React.RefObject<HTMLDivElement>;
};

const Canvas = ({ boardState, parentRef }: CanvasProps) => {
  const ref = useRef<HTMLCanvasElement>(null);
  const [canvasDims, setCanvasDims] = useState<{
    width?: number;
    height?: number;
  }>({ width: undefined, height: undefined });

  useEffect(() => {
    setCanvasDims({
      width: parentRef.current?.offsetWidth,
      height: parentRef.current?.offsetHeight,
    });
  }, []);

  const [draw, setDraw] = useState<(ctx: CanvasRenderingContext2D) => void>(
    (_) => null
  );

  const mkDraw = (state: BoardState) => (ctx: CanvasRenderingContext2D) => {
    const canvasWidth = parentRef.current?.clientWidth;
    const canvasHeight = parentRef.current?.clientHeight;
    if (canvasWidth && canvasHeight) {
      ctx.clearRect(0, 0, canvasWidth, canvasHeight);
      ctx.font = `60px serif`;
      ctx.textAlign = "left"; 
      ctx.textBaseline = "bottom"; 
      Object.values(state).forEach((position) => {
        ctx.fillText(
          "🦧",
          (canvasWidth / 10) * position.x,
          (canvasHeight - 10) - (canvasHeight / 10) * position.y
        );
      });
    }
  };

  useEffect(() => {
    if (boardState !== null) {
      setDraw(() => mkDraw(boardState));
    }
  }, [boardState]);

  useEffect(() => {
    const canvas = ref.current;
    const ctx = canvas?.getContext("2d");
    ctx && draw && draw(ctx);
  }, [draw]);

  return (
    <canvas
      ref={ref}
      height={canvasDims.height}
      width={canvasDims.width}
    ></canvas>
  );
};

export { Board };
export type { BoardState };
