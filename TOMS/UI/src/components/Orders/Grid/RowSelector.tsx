import { ComponentPropsWithoutRef } from 'react';

import { Checkbox } from 'components/Checkbox';

import * as Styles from './RowSelector.styles';

type Props = ComponentPropsWithoutRef<typeof Checkbox>;

export const RowSelector = (props: Props) => {
  return (
    <Styles.Wrapper>
      <Styles.Checkbox
        {...props}
        icon={<Styles.Icon />}
        checkedIcon={<Styles.CheckedIcon />}
      />
    </Styles.Wrapper>
  );
};
