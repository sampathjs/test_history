import { rem } from 'polished';
import styled from 'styled-components/macro';

import { commonTextInputStyles } from 'components/Input';

export const Input = styled.textarea`
  ${commonTextInputStyles}
  min-height: 76px;
  padding: ${rem(8)};
  line-height: ${rem(18)};
`;
