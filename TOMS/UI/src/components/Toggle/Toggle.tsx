import {
  ChangeEventHandler,
  cloneElement,
  forwardRef,
  Fragment,
  ReactElement,
} from 'react';

import { ToggleItemProps } from './';
import * as Styles from './Toggle.styles';

export interface ToggleProps {
  children: ReactElement<ToggleItemProps>[];
  name: string;
  value?: string;
  defaultValue?: string;
  onChange?: ChangeEventHandler<HTMLInputElement>;
}

export const Toggle = forwardRef(
  (
    {
      children,
      defaultValue,
      name,
      onChange,
      value,
      ...controlProps
    }: ToggleProps,
    ref
  ) => {
    const isChecked = (itemValue?: string) => {
      if (value) {
        return itemValue === value;
      }
      if (defaultValue) {
        return itemValue === defaultValue;
      }
      return false;
    };

    return (
      <Styles.Wrapper {...controlProps} role="radiogroup">
        {children.map((child, index) => (
          <Fragment key={index}>
            {cloneElement(child, {
              key: index,
              onChange,
              name,
              checked: isChecked(child.props.value),
            })}
            {index !== children.length - 1 && (
              <Styles.Separator isShown={!value} />
            )}
          </Fragment>
        ))}
      </Styles.Wrapper>
    );
  }
);

Toggle.displayName = 'Toggle';
