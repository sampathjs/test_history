import { rem } from 'polished';
import styled, { css } from 'styled-components/macro';

import { FILTER_PANEL_WIDTH } from 'styles';

export const Layout = styled.div<{ $isFilterPanelExpanded: boolean }>`
  display: grid;
  grid-template-columns: 2px 1fr;
  grid-template-rows: minmax(0, 1fr);
  grid-template-areas: 'filters .';
  overflow-x: hidden;

  ${(props) =>
    props.$isFilterPanelExpanded &&
    css`
      grid-template-columns: ${rem(FILTER_PANEL_WIDTH)} 1fr;
    `}
`;

export const Main = styled.main`
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
`;

export const Content = styled.div`
  position: relative;
  display: grid;
  grid-template-rows: minmax(0, 1fr);
  margin-top: ${rem(12)};
`;
