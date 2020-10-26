import React from 'react';

// const UserContext = React.createContext({});

export const Login: React.FC = () => {
  const [email, setEmail] = React.useState('');
  const [password, setPassword] = React.useState('');

  const onLoginClick = async () => {
    const response = await fetch('http://localhost:8080/users/login', {
      method: 'POST',
      mode: 'cors',
      cache: 'no-cache',
      credentials: 'include',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        email,
        password,
      }),
    });

    console.log(response);
    console.log(await response.json());
  };

  return (
    <>
      <h1>
        Login
      </h1>
      <form id="rendered-form">
        <div>
          <div>
            <label htmlFor="email">
              email address
              <input
                id="email"
                name="email"
                type="text"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
              />
            </label>
          </div>
          <div>
            <label htmlFor="password">
              password
              <input
                id="password"
                name="password"
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
              />
            </label>
          </div>
          <div>
            <button
              type="button"
              onClick={onLoginClick}
            >
              Login
            </button>
            <button type="button">
              Register
            </button>
          </div>
        </div>
      </form>
    </>
  );
};
export default Login;
