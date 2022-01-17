import { ReactNode, useState } from 'react';

import * as Styles from './Collapsable.styles';

type Props = {
  label: ReactNode;
  collapsedLabel?: ReactNode;
  collapsedByDefault?: boolean;
  content: ReactNode;
};

export const Collapsable = (props: Props) => {
  const { collapsedByDefault, collapsedLabel, content, label } = props;

  const [isCollapsed, setIsCollapsed] = useState(Boolean(collapsedByDefault));

  const handleClick = () => {
    setIsCollapsed((isCollapsed) => !isCollapsed);
  };

  return (
    <Styles.Wrapper>
      <Styles.Button type="button" onClick={handleClick}>
        {isCollapsed ? (collapsedLabel ? collapsedLabel : label) : label}
      </Styles.Button>
      <Styles.Content $isCollapsed={isCollapsed}>{content}</Styles.Content>
    </Styles.Wrapper>
  );
};
