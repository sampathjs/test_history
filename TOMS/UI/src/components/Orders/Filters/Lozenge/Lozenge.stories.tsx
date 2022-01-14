import { ComponentStory, Meta } from '@storybook/react';

import { Lozenge } from './Lozenge';

export default {
  title: 'Filter/Lozenge',
  component: Lozenge,
} as Meta<typeof Lozenge>;

const Template: ComponentStory<typeof Lozenge> = (args) => (
  <Lozenge {...args}>My label</Lozenge>
);

export const Standard = Template.bind({});
Standard.args = {};

export const Active = Template.bind({});
Active.args = {
  isActive: true,
};
