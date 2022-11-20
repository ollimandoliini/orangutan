import { useEffect, useState } from "react";
import useWebSocket, { ReadyState } from "react-use-websocket";
import "./App.css";

import { Board, BoardState } from "./Board";

type Scores = Record<string, number>;

const ScoreBoard = ({ scores }: { scores: Scores }) => {
  return (
    <div>
      <h2>Scores</h2>
      <div className="score-wrapper">
        {Object.entries(scores).sort(([aPlayerId, aScore], [bPlayerId, bScore]) => aScore - bScore).map(([playerId, score], index) => (
          <div className="score-line" key={playerId}>
            <div >{index + 1}.</div> <div>{playerId}</div> <div>{score}</div>
          </div>
        ))}
      </div>
    </div>
  );
};



type GameState = {
  board: BoardState;
  scores: Scores;
};

const App = () => {
  const [playerName, setPlayerName] = useState<string | null>(null);

  const [gameState, setGameState] = useState<GameState>({
    board: {},
    scores: {},
  });
  const { sendJsonMessage, lastJsonMessage } = useWebSocket<GameState>(
    playerName && `wss://${window.location.host}/ws/${playerName}`
  );


  useEffect(() => {
    if (lastJsonMessage) {
      setGameState(lastJsonMessage);
    }
  }, [lastJsonMessage]);

  return (
    <div className="main">
      <div className="main-left">
        <h1>Orangutan</h1>
        <Board
          sendJsonMessage={sendJsonMessage}
          boardState={gameState.board}
          playerName={playerName}
          setPlayerName={setPlayerName}
        />
      </div>
      <ScoreBoard scores={gameState.scores} />
    </div>
  );
};

export default App;
