import { StyledComponentInnerOtherProps } from 'styled-components/macro';

import * as Styles from './OrderFill.styles';

type Props = StyledComponentInnerOtherProps<typeof Styles.Indicator> & {
  fillAmount: number;
};

export const OrderFill = (props: Props) => {
  const { fillAmount, side } = props;
  const fillAmountPercentage = fillAmount * 100;
  const label = `${parseFloat(fillAmountPercentage.toFixed(0))}%`;

  return (
    <Styles.Wrapper title={label}>
      <Styles.IndicatorWrapper>
        <Styles.Indicator fillAmount={fillAmountPercentage} side={side} />
      </Styles.IndicatorWrapper>
      <Styles.Label>{label}</Styles.Label>
    </Styles.Wrapper>
  );
};
