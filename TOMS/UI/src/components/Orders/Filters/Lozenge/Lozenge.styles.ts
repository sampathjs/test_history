import { rem } from 'polished';
import styled from 'styled-components/macro';

import { getColor, getFont } from 'styles/theme/helpers';

export const Lozenge = styled.button<{ $isActive?: boolean }>`
  display: inline-block;
  padding: ${rem(1)} ${rem(8)};
  background: ${(props) =>
    props.$isActive
      ? getColor('background.tertiaryNav')
      : getColor('filter.filterPanelBg')};
  border: none;
  border-radius: ${rem(3)};
  color: ${getColor('white.85')};
  appearance: none;

  &:hover {
    background: ${getColor('filter.hover')};
    color: ${getColor('background.tertiaryNav')};
  }
`;

export const Label = styled.span`
  ${getFont('body.smallBold')}
  display: block;
  margin: ${rem(5)} 0;
  cursor: inherit;
`;
