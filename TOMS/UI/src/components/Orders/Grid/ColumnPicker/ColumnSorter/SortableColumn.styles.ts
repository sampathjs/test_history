import { ellipsis, rem } from 'polished';
import styled, { css } from 'styled-components/macro';

import { getColor, getFont } from 'styles';

type ColumnProps = {
  dragging: boolean;
};

export const Column = styled.li<ColumnProps>`
  background-color: ${getColor('orderEntry.selectedLegPanelInput')};
  border-radius: ${rem(3)};
  padding: ${rem(8)};
  color: ${getColor('white.85')};
  flex-shrink: 0;
  cursor: default;

  ${ellipsis()}
  ${getFont('body.small')}

  ${(props) =>
    props.dragging &&
    css`
      z-index: 1;
    `}
`;
