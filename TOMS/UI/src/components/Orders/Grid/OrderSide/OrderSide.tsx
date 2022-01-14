import { OrderSide as OrderSideType } from 'types';

import * as Styles from './OrderSide.styles';

type Props = {
  side: OrderSideType;
};

export const OrderSide = (props: Props) => {
  const { side } = props;

  return (
    <Styles.Wrapper title={side}>
      <Styles.Indicator side={side} />
      <Styles.Label>{side}</Styles.Label>
    </Styles.Wrapper>
  );
};
