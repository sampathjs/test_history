import { ComponentStory, Meta } from '@storybook/react';

import { Checkbox } from './Checkbox';

export default {
  title: 'Checkbox',
  component: Checkbox,
} as Meta<typeof Checkbox>;

const Template: ComponentStory<typeof Checkbox> = (args) => (
  <Checkbox {...args} />
);

export const Checked = Template.bind({});
Checked.args = {
  name: 'my-checkbox',
  value: 1,
  checked: true,
};

export const Unchecked = Template.bind({});
Unchecked.args = {
  name: 'my-checkbox',
  value: 1,
  checked: false,
};
