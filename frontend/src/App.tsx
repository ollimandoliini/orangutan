import { useState } from "react";
import useWebSocket, { ReadyState } from "react-use-websocket";
import "./App.css";

import { Board, GameState } from "./Board";

const ScoreBoard = (scores: Record<string, number>) => {
  return (
    <>
      <h2>Scores</h2>
      <ul>
        <li></li>
      </ul>
    </>
  );
};

type PlayerNameProps = {
  setPlayerName: (name: string) => void;
};

const PlayerNamePrompt = ({ setPlayerName }: PlayerNameProps) => {
  const [name, setName] = useState("");
  return (
    <form
      action=""
      onSubmit={(e) => {
        e.preventDefault();
        setPlayerName(name)
        setName("");

      }}
    >
      <label htmlFor="">
        Name
        <input
          type="text"
          name=""
          id=""
          value={name}
          onChange={(e) => setName(e.target.value)}
        />
      </label>
      <button type="submit">Enter</button>
    </form>
  );
};

const App = () => {
  const [playerName, setPlayerName] = useState<string | null>(null);
  const { sendJsonMessage, lastJsonMessage } = useWebSocket<GameState>(
    playerName && `ws://localhost:8080/ws/${playerName}`
  );
  return (
    <div className="main">
      <div>
        {playerName ? (
          <Board
            sendJsonMessage={sendJsonMessage}
            lastJsonMessage={lastJsonMessage}
          />
        ) : (
          <PlayerNamePrompt setPlayerName={setPlayerName} />
        )}
      </div>
    </div>
  );
};

export default App;
