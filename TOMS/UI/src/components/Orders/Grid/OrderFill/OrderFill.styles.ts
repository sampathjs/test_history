import { ellipsis, rem } from 'polished';
import styled from 'styled-components/macro';

import { getColor, getFont } from 'styles';
import { OrderSide } from 'types';

export const Wrapper = styled.div`
  display: flex;
  align-items: center;
  max-width: 100%;
  height: 100%;
`;

export const IndicatorWrapper = styled.div`
  border: ${rem(1)} solid ${getColor('white.5')};
  padding: ${rem(1)};
  border-radius: ${rem(2)};
  margin-right: ${rem(6)};
  flex-shrink: 0;
`;

type IndicatorProps = {
  fillAmount: number;
  side: OrderSide;
};

export const Indicator = styled.div<IndicatorProps>`
  width: ${rem(40)};
  height: ${rem(6)};
  background-color: ${getColor('white.5')};
  border-radius: ${rem(1)};
  overflow: hidden;
  position: relative;

  &::after {
    content: '';
    position: absolute;
    height: 100%;
    background-color: ${(props) =>
      props.side === OrderSide.buy
        ? getColor('primary.green100')
        : getColor('primary.red100')};
    width: ${(props) => `${props.fillAmount}%`};
  }
`;

export const Label = styled.div`
  color: ${getColor('white.30')};

  ${ellipsis()}
  ${getFont('button.xSmall')};
`;
