import { ComponentStory, Meta } from '@storybook/react';

import { LozengeFilter } from './LozengeFilter';

export default {
  title: 'Filter/LozengeFilter',
  component: LozengeFilter,
} as Meta<typeof LozengeFilter>;

const Template: ComponentStory<typeof LozengeFilter> = (args) => (
  <LozengeFilter {...args} />
);

export const Standard = Template.bind({});
Standard.args = {
  allOptionLabel: 'Both',
  onChange: () => {
    return true;
  },
  options: [
    { name: 'Option 1', value: 0 },
    { name: 'Option 2', value: 1 },
    { name: 'Option 3', value: 2 },
    { name: 'Option 4', value: 3 },
    { name: 'Option 5', value: 4 },
  ],
  value: [],
};
