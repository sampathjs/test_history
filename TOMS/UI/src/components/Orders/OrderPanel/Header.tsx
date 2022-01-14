import { ReactNode } from 'react';
import { StyledComponentInnerOtherProps } from 'styled-components/macro';

import { XIcon } from 'components/Icon';
import { useOrderPanelContext } from 'contexts';

import * as Styles from './Header.styles';
import { Actions } from './OrderPanel.styles';

type Props = StyledComponentInnerOtherProps<typeof Styles.Header> & {
  children?: ReactNode;
};

export const Header = (props: Props) => {
  const { children, ...headerProps } = props;
  const { clearView } = useOrderPanelContext();

  return (
    <Styles.Header {...headerProps}>
      {children}

      <Actions>
        <Styles.CloseButton onClick={clearView}>
          <XIcon />
        </Styles.CloseButton>
      </Actions>
    </Styles.Header>
  );
};
