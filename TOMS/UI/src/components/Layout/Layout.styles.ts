import styled from 'styled-components/macro';

export const Layout = styled.div`
  display: grid;
  grid-template-columns: auto 1fr;
  grid-template-rows: auto minmax(0, 1fr);
  grid-template-areas:
    'header header'
    'nav content';
  height: 100vh;
  background-color: ${(props) => props.theme.colors.background.bg};
`;

export const Content = styled.div`
  grid-area: content;
  display: grid;
  grid-template-rows: minmax(0, 1fr);
  overflow: auto;
`;
