import { ellipsis, rem } from 'polished';
import styled, { css } from 'styled-components/macro';

import { getColor, getFont } from 'styles';
import { OrderSide } from 'types';

export const Wrapper = styled.div`
  ${getFont('button.xSmallBold')}
  background: ${getColor('background.primaryNav')};
  border-radius: ${rem(4)};
  padding: ${rem(4)};
  color: ${getColor('background.dividerLine')};
  display: flex;
  width: 100%;
  justify-content: space-evenly;

  &:hover > div {
    opacity: 0;
  }
`;

type LabelProps = {
  side?: OrderSide;
};

export const Label = styled.label<LabelProps>`
  text-align: center;
  overflow: hidden;
  flex: auto;
  cursor: pointer;

  input[type='radio'] {
    display: none;

    & + div:hover {
      background-color: ${getColor('filter.hover')};
      color: ${getColor('background.ordersGrid')};
    }

    &:checked + div {
      color: ${getColor('background.primaryNav')};
      background-color: ${getColor('orderEntry.selectedLozenge')};

      ${({ side }) =>
        side === OrderSide.buy &&
        css`
          background-color: ${getColor('primary.green100')};
        `}

      ${({ side }) =>
        side === OrderSide.sell &&
        css`
          background-color: ${getColor('primary.red100')};
        `}
    }
  }
`;

interface SeparatorProps {
  isShown: boolean;
}

export const Separator = styled.div<SeparatorProps>`
  width: 1px;
  height: 16px;
  margin-top: 2px;
  margin-left: 1px;
  margin-right: 1px;
  background-color: ${getColor('background.dividerLine')};

  ${({ isShown }) =>
    !isShown &&
    css`
      opacity: 0;
    `}
`;

export const ItemText = styled.div`
  ${ellipsis()}
  text-transform: uppercase;
  display: block;
  border-radius: ${rem(3)};
  padding: ${rem(4)} ${rem(6)};
`;
