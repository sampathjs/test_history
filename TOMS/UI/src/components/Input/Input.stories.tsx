import { ComponentStory, Meta } from '@storybook/react';

import { Input } from './Input';

export default {
  title: 'Form/Components/Input',
  component: Input,
} as Meta<typeof Input>;

const Template: ComponentStory<typeof Input> = (args) => <Input {...args} />;

export const Default = Template.bind({});
Default.args = {};

export const Controlled = Template.bind({});
Controlled.args = { value: 'foo' };
