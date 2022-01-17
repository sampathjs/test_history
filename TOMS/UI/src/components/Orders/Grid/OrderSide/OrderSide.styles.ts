import { ellipsis, rem } from 'polished';
import styled from 'styled-components/macro';

import { getColor } from 'styles';
import { OrderSide } from 'types';

export const Wrapper = styled.div`
  display: inline-flex;
  align-items: center;
  text-transform: uppercase;
  max-width: 100%;
`;

type IndicatorProps = {
  side: OrderSide;
};

export const Indicator = styled.div<IndicatorProps>`
  height: ${rem(9)};
  width: ${rem(9)};
  margin-right: ${rem(6)};
  flex-shrink: 0;
  border-radius: 50%;

  background-color: ${(props) =>
    props.side === OrderSide.buy
      ? getColor('primary.green100')
      : getColor('primary.red100')};
`;

export const Label = styled.div`
  ${ellipsis()}
`;
