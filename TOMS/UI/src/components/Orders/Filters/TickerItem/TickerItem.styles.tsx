import styled, { css } from 'styled-components/macro';

import { getColor } from 'styles';

export const Ticker = styled.div<{ $hasNuancedStyles: boolean }>`
  display: grid;
  grid-template-columns: auto auto auto 1fr;
  column-gap: 5px;
  width: 100%;
  color: ${getColor('white.60')};

  ${(props) =>
    props.$hasNuancedStyles &&
    css`
      div:first-of-type {
        color: ${getColor('white.40')};
      }
    `}
`;
