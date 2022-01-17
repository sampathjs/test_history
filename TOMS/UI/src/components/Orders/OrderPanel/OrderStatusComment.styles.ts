import { rem } from 'polished';
import styled from 'styled-components/macro';

import { TextArea } from 'components/TextArea';
import { getColor, getFont } from 'styles';

export const Wrapper = styled.div`
  padding: ${rem(20)};
  background-color: ${getColor('orderEntry.selectedLozenge')};
`;

export const Title = styled.h4`
  ${getFont('heading.xSmall')}
`;

export const Comment = styled(TextArea)`
  margin-top: ${rem(8)};
  margin-bottom: ${rem(10)};
  resize: none;
  min-height: ${rem(122)};
  background-color: ${getColor('white.100')};
  border-radius: ${rem(3)};
  color: ${getColor('filter.filterDivider')};

  &::placeholder {
    color: ${getColor('filter.filterDivider')};
  }
`;

export const Actions = styled.div`
  display: flex;
  justify-content: flex-end;
  align-items: center;
`;
