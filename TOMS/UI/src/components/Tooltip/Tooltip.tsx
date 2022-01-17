import { ReactNode } from 'react';
import ReactTooltip from 'react-tooltip';

import { InfoIcon } from 'components/Icon';

import * as Styles from './Tooltip.styles';

export interface TooltipProps {
  children: ReactNode;
  text: string;
  hasIcon?: boolean;
}

export const Tooltip = ({ children, hasIcon = true, text }: TooltipProps) => (
  <Styles.Wrapper>
    {children}
    <Styles.Trigger data-tip data-for="tooltip">
      {hasIcon && <InfoIcon />}
      <ReactTooltip
        className="tooltip"
        id="tooltip"
        place="right"
        effect="solid"
      >
        {text}
      </ReactTooltip>
    </Styles.Trigger>
  </Styles.Wrapper>
);
