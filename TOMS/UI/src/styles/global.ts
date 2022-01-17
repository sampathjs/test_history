import { normalize } from 'polished';
import { createGlobalStyle } from 'styled-components/macro';

export const GlobalStyles = createGlobalStyle`
  ${normalize()};

  *,
  *:before,
  *:after {
    box-sizing: border-box;
  }

  body {
    font-size: 16px;
    font-family: 'HK Grotesk';
    overflow-x: auto;
  }

  ul {
    list-style: none;
    margin: 0;
    padding: 0;
  }

  button {
    cursor: pointer;
    appearance: none;
    background-color: transparent;
    border: none;
    outline: none;
    padding: 0;
  }

  h1,
  h2,
  h3,
  h4,
  h5,
  h6 {
    margin: 0;
    padding: 0;
  }
`;
