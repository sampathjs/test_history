import { ComponentStory, Meta } from '@storybook/react';

import { OrderSide } from 'types';

import { Toggle, ToggleItem } from './';

export default {
  title: 'Form/Components/Toggle',
  component: Toggle,
} as Meta<typeof Toggle>;

const Template: ComponentStory<typeof Toggle> = (args) => <Toggle {...args} />;

export const Default = Template.bind({});
Default.args = {
  children: [
    <ToggleItem key="1" value="1">
      Limit
    </ToggleItem>,
    <ToggleItem key="2" value="2">
      Ref
    </ToggleItem>,
  ],
};

export const WithDefaultSelected = Template.bind({});
WithDefaultSelected.args = {
  value: '2',
  children: [
    <ToggleItem key="1" value="1">
      Limit
    </ToggleItem>,
    <ToggleItem key="2" value="2">
      Ref
    </ToggleItem>,
  ],
};

export const OrderSideToggle = Template.bind({});
OrderSideToggle.args = {
  children: [
    <ToggleItem key={OrderSide.buy} side={OrderSide.buy} value={OrderSide.buy}>
      Buy
    </ToggleItem>,
    <ToggleItem
      key={OrderSide.sell}
      side={OrderSide.sell}
      value={OrderSide.sell}
    >
      Sell
    </ToggleItem>,
  ],
};
