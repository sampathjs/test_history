import { ComponentStory, Meta } from '@storybook/react';
import { useForm } from 'react-hook-form';

import { ToggleItem } from 'components/Toggle';
import { OrderSide } from 'types';

import { ToggleInput } from '.';

export default {
  title: 'Form/Inputs/ToggleInput',
  component: ToggleInput,
} as Meta<typeof ToggleInput>;

const Template: ComponentStory<typeof ToggleInput> = (args) => {
  const { control } = useForm();

  return (
    <ToggleInput {...args} name="orderSide" control={control}>
      <ToggleItem side={OrderSide.buy} value={OrderSide.buy} />
      <ToggleItem side={OrderSide.sell} value={OrderSide.sell} />
    </ToggleInput>
  );
};

export const Default = Template.bind({});
Default.args = {};

export const WithDefaultValue = Template.bind({});
WithDefaultValue.args = {
  defaultValue: OrderSide.buy,
};
