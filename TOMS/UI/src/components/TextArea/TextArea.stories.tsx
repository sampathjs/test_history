import { ComponentStory, Meta } from '@storybook/react';

import { TextArea } from './TextArea';

export default {
  title: 'Form/Components/TextArea',
  component: TextArea,
} as Meta<typeof TextArea>;

const Template: ComponentStory<typeof TextArea> = (args) => (
  <TextArea {...args} />
);

export const Default = Template.bind({});
Default.args = {};

export const Controlled = Template.bind({});
Controlled.args = { value: 'foo' };
