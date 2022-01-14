import { rem } from 'polished';
import styled, { css } from 'styled-components/macro';

export const List = styled.ul`
  position: relative;
  display: flex;
  margin: ${rem(9)} 0 ${rem(5)};
  gap: ${rem(5)};
`;

export const DatePicker = styled.div<{ $isHidden: boolean }>`
  position: absolute;
  top: calc(100% + ${rem(8)});
  left: 0;
  z-index: 1;

  ${(props) =>
    props.$isHidden &&
    css`
      display: none;
    `}
`;
