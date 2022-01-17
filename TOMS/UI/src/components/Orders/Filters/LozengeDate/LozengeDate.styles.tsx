import { rem } from 'polished';
import styled, { css } from 'styled-components/macro';

import { Calendar } from 'components/DatePicker/Calendar/Calendar.styles';
import { zIndex } from 'styles/layout';

export const DatePicker = styled.div<{ $isHidden: boolean }>`
  position: absolute;
  top: calc(100% + ${rem(8)});
  left: 0;
  z-index: ${zIndex.LEVEL_1};

  ${Calendar} {
    width: calc(${rem(236)} - ${rem(24)});
  }

  ${(props) =>
    props.$isHidden &&
    css`
      display: none;
    `}
`;
