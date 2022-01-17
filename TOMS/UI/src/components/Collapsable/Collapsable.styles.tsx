import styled, { css } from 'styled-components/macro';

export const Wrapper = styled.div``;

export const Button = styled.button`
  width: 100%;
  text-align: left;
  padding: 0;
  margin: 0;
  background-color: transparent;
  border: none;
  color: inherit;
  appearance: none;
`;

export const Content = styled.div<{ $isCollapsed: boolean }>`
  overflow: hidden;
  transition: 0.5s max-height ease-in;
  height: auto;
  max-height: 100vh;

  ${(props) =>
    props.$isCollapsed &&
    css`
      max-height: 0;
      transition: 0.3s max-height ease-out;
    `}
`;
