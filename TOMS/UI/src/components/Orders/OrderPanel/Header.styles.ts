import { rem } from 'polished';
import styled, { css } from 'styled-components/macro';

import { getColor } from 'styles';
import { OrderSide } from 'types';
import { Nullable } from 'types/util';

type HeaderProps = {
  side?: Nullable<OrderSide>;
};

export const Header = styled.header<HeaderProps>`
  padding: ${rem(15)} ${rem(20)} ${rem(12)};
  display: flex;
  align-items: center;
  border-bottom: ${rem(2)} solid ${getColor('primary.lilac100')};

  ${(props) =>
    props.side === OrderSide.buy &&
    css`
      border-bottom-color: ${getColor('primary.green100')};
    `}

  ${(props) =>
    props.side === OrderSide.sell &&
    css`
      border-bottom-color: ${getColor('primary.red100')};
    `}
`;

export const CloseButton = styled.button`
  color: ${getColor('filter.filterDivider')};
  margin-left: auto;
  font-size: ${rem(10)};
  border: 1px solid ${getColor('filter.filterDivider')};
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: ${rem(24)};
  width: ${rem(24)};
  border-radius: 50%;

  &:hover {
    color: ${getColor('white.100')};
    border-color: ${getColor('primary.blue100')};
  }

  &:active {
    color: ${getColor('primary.lilac100')};
    border-color: ${getColor('primary.lilac60')};
  }
`;
