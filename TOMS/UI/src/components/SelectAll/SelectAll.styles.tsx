import { rem } from 'polished';
import styled from 'styled-components/macro';

import { getColor } from 'styles/theme/helpers';

export const SelectAllCheckbox = styled.span`
  font-size: ${rem(14)};
  color: ${getColor('white.100')};
`;

export const Wrapper = styled.span`
  color: ${getColor('white.50')};

  &:hover {
    color: ${getColor('primary.blue100')};

    ${SelectAllCheckbox} {
      color: ${getColor('primary.blue100')};

      #checkbox-check {
        stroke-opacity: 1;
      }

      #checkbox-outline {
        fill-opacity: 1;
      }
    }
  }
`;
