import { ellipsis, hideVisually, rem } from 'polished';
import styled, { css } from 'styled-components/macro';

import { getColor } from 'styles/theme/helpers';

import { Ticker } from '../../TickerItem/TickerItem.styles';

export const Label = styled.label<{
  $isActive: boolean;
  $areAllSelected: boolean;
}>`
  position: relative;
  display: flex;
  padding: ${rem(4.5)} ${rem(8)};
  background-color: transparent;
  border-radius: ${rem(3)};
  cursor: pointer;
  width: 100%;

  ${(props) =>
    props.$isActive &&
    css`
      background-color: ${getColor('background.tertiaryNav')};
      background-image: none;

      ${!props.$areAllSelected &&
      css`
        color: ${getColor('white.85')};
      `}
    `};

  ${(props) =>
    props.$areAllSelected &&
    css`
      background-color: transparent;
      background-image: linear-gradient(
        270deg,
        rgba(30, 33, 46, 0) 0%,
        rgba(30, 33, 46, 0.8) 100%
      );
    `}

  &:hover {
    background: ${getColor('filter.hover')};
    background-image: none;
    color: ${getColor('background.tertiaryNav')};

    ${Ticker} {
      color: ${getColor('background.tertiaryNav')};

      div:first-of-type {
        color: ${getColor('background.tertiaryNav')};
      }
    }
  }

  span {
    ${ellipsis()}
  }
`;

export const Input = styled.input`
  ${hideVisually()};
`;
