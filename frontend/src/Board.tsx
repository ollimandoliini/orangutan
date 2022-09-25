import { useEffect, useRef, useState } from "react";

const CANVAS_WIDTH = 600;
const CANVAS_HEIGHT = 600;

type GameState = Record<string, PlayerPosition>;
type PlayerPosition = {
  x: number;
  y: number;
};

type BoardProps = {
  sendJsonMessage: (payload: { action: string }) => void;
  lastJsonMessage: GameState;
};

const Board = ({ sendJsonMessage, lastJsonMessage }: BoardProps) => {
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


  return <Canvas lastJsonMessage={lastJsonMessage} />;
};

type CanvasProps = {
  // draw?: (ctx: CanvasRenderingContext2D) => void;
  lastJsonMessage: GameState;
};

const Canvas = ({ lastJsonMessage }: CanvasProps) => {
  const ref = useRef<HTMLCanvasElement>(null);
  const [draw, setDraw] = useState<(ctx: CanvasRenderingContext2D) => void>(
    (_) => null
  );

  const mkDraw = (state: GameState) => (ctx: CanvasRenderingContext2D) => {
    ctx.clearRect(0, 0, CANVAS_WIDTH, CANVAS_HEIGHT);
    ctx.font = "48px serif";
    Object.values(state).forEach((position) => {
      ctx.fillText(
        "🦧",
        (CANVAS_WIDTH / 10) * position.x,
        (CANVAS_HEIGHT / 10) * position.y
      );
    });
  };

  useEffect(() => {
    if (lastJsonMessage !== null) {
      setDraw(() => mkDraw(lastJsonMessage));
    }
  }, [lastJsonMessage]);

  useEffect(() => {
    const canvas = ref.current;
    const ctx = canvas?.getContext("2d");
    ctx && draw && draw(ctx);
  }, [draw]);

  return (
    <canvas ref={ref} width={CANVAS_WIDTH} height={CANVAS_HEIGHT}></canvas>
  );
};

export { Board };
export type { GameState };
