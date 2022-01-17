import { rem } from 'polished';
import styled from 'styled-components/macro';

import { getColor, getFont } from 'styles/theme/helpers';

export const GroupSubheading = styled.h3`
  ${getFont('body.small')}
  color: ${getColor('white.70')};
  padding: ${rem(4.5)} ${rem(8)};
  margin: 0;
`;
