import { hideVisually } from 'polished';
import styled from 'styled-components/macro';

export const Wrapper = styled.span<{ $isChecked: boolean }>``;

export const Input = styled.input`
  ${hideVisually()}
`;
