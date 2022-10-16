import { useState } from "react";

type PlayerNameProps = {
  setPlayerName: (name: string) => void;
};

const PlayerNamePrompt = ({ setPlayerName }: PlayerNameProps) => {
  const [name, setName] = useState("");
  return (
    <div>
      <h2>Welcome</h2>
      <div>Enter your name to play</div>
      <form
        action=""
        onSubmit={(e) => {
          e.preventDefault();
          setPlayerName(name);
          setName("");
        }}
      >
        <input
          type="text"
          name=""
          id=""
          value={name}
          onChange={(e) => setName(e.target.value)}
          autoFocus={true}
          
        />

        <button type="submit">Enter</button>
      </form>
    </div>
  );
};

export default PlayerNamePrompt
