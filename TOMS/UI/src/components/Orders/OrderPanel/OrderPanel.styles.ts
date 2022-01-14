import { ellipsis, rem } from 'polished';
import styled, { css } from 'styled-components/macro';

import { getColor, getFont, getShadow } from 'styles';
import { OrderSide } from 'types';

export const Wrapper = styled.div<{ $isOpen?: boolean }>`
  display: flex;
  flex-direction: column;
  width: ${rem(470)};
  background-color: ${getColor('orderEntry.bg')};
  position: absolute;
  right: 0;
  height: 100%;
  box-shadow: ${getShadow('orderPanel')};
  transition: transform 0.45s cubic-bezier(0.19, 1, 0.22, 1);
  transform: translateX(470px);

  ${({ $isOpen }) =>
    $isOpen &&
    css`
      transform: translateX(0px);
    `}
`;

export const HeaderTitle = styled.h2`
  margin: 0;
  color: ${getColor('white.100')};
  flex: 1;

  ${getFont('heading.large')}
`;

export const Form = styled.form`
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  flex: 1;
  min-height: 0;
`;

export const Content = styled.div`
  flex: 1;
  overflow-y: auto;
`;

export const Actions = styled.div`
  display: flex;
  align-items: center;
  column-gap: ${rem(8)};

  & + & {
    border-left: 1px solid ${getColor('primary.lilac20')};
    margin-left: ${rem(8)};
    padding-left: ${rem(8)};
  }
`;

export const Footer = styled.footer`
  padding: ${rem(16)} ${rem(20)} ${rem(20)};
  border-top: 1px solid ${getColor('primary.lilac20')};
  display: flex;
  align-items: center;
  justify-content: flex-end;
`;

// Order details
export const Details = styled.div`
  padding: ${rem(4)} ${rem(20)};
`;

const Section = styled.section`
  padding: ${rem(12)} 0;
`;

export const SectionRow = styled.div`
  display: flex;
  column-gap: ${rem(15)};

  & + & {
    margin-top: ${rem(16)};
  }
`;

export const Summary = styled(Section)``;

export const PrimarySection = styled(Section)`
  border-top: 1px solid ${getColor('primary.lilac40')};
`;

export const SecondarySection = styled(Section)`
  border-top: 1px solid ${getColor('primary.lilac20')};
`;

export const Column = styled.div`
  flex: 1;
`;

export const ThirdColumn = styled.div`
  width: calc(100% / 3 - 10px);
  flex-shrink: 0;
`;

export const HalfColumn = styled.div`
  width: calc(100% / 2 - 7px);
  flex-shrink: 0;
`;

export const ToggleWrapper = styled.div`
  display: flex;
  column-gap: ${rem(15)};
  width: 50%;
  justify-content: space-between;

  label {
    flex: 1;
  }

  & ~ label {
    width: 50%;
  }
`;

export const SpotForwardWrapper = styled.div`
  display: flex;
  align-items: center;
  color: ${getColor('primary.lilac20')};
  svg {
    font-size: ${rem(18)};
    margin-left: 14px;
  }
`;

export const SectionTitle = styled.h3`
  margin: 0;
  margin-bottom: ${rem(16)};
  color: ${getColor('white.95')};

  ${getFont('heading.medium')};
`;

export const KeyValue = styled.dl`
  margin: 0;
  min-width: 0;
`;

export const Key = styled.dt`
  padding-bottom: ${rem(5)};
  color: ${getColor('white.70')};
  text-transform: uppercase;

  ${getFont('heading.entryTitle')}
`;

export const SecondaryKey = styled(Key)`
  color: ${getColor('orderEntry.selectedLozenge')};
`;

export const Value = styled.dd`
  margin: 0;
  padding: ${rem(5)} 0;
  color: ${getColor('white.95')};

  ${ellipsis()}
  ${getFont('body.medium')}
`;

export const MultiLineValue = styled.dd`
  margin: 0;
  padding: ${rem(5)} 0;
  color: ${getColor('white.95')};
  white-space: pre-wrap;

  ${getFont('body.medium')}

  line-height: 1.5;
`;

export const ValueTag = styled.span`
  display: inline-block;
  outline: ${rem(4)} solid ${getColor('background.primaryNav')};
  margin: ${rem(4)};
  border-radius: ${rem(4)};
  padding: ${rem(4)} ${rem(5)};
  color: ${getColor('background.primaryNav')};
  text-transform: uppercase;
  background-color: ${getColor('orderEntry.selectedLozenge')};

  ${getFont('button.xSmallBold')}
`;

type TradeSideProps = {
  side: OrderSide;
};

export const TradeSide = styled(ValueTag)<TradeSideProps>`
  ${(props) =>
    props.side === OrderSide.buy &&
    css`
      background-color: ${getColor('primary.green100')};
    `}

  ${(props) =>
    props.side === OrderSide.sell &&
    css`
      background-color: ${getColor('primary.red100')};
    `}
`;

export const SecondarySectionTitle = styled(Key)`
  margin-bottom: ${rem(3)};
`;
